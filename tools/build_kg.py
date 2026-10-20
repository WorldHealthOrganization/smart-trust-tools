#!/usr/bin/env python3
"""Build the SMART Trust library requirements knowledge graph.

Reads the YAML sources in requirements/kg/, validates them, and writes:

    requirements/generated/kg.ttl         RDF Turtle
    requirements/generated/kg.jsonld      JSON-LD (same graph)
    requirements/generated/REQUIREMENTS.md  human-readable rendering

Usage:
    python3 tools/build_kg.py               validate and (re)generate
    python3 tools/build_kg.py --check       fail if generated files are stale
    python3 tools/build_kg.py --check-links also probe every URL over HTTP

Requires PyYAML only.
"""

import argparse
import collections
import json
import pathlib
import re
import sys
import urllib.error
import urllib.request

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent
KG_DIR = ROOT / "requirements" / "kg"
OUT_DIR = ROOT / "requirements" / "generated"

BASE = "https://github.com/litlfred/smart-trust-tools/kg/"
VOCAB = BASE + "vocab#"
ID = BASE + "id/"

PREFIXES = {
    "stt": VOCAB,
    "id": ID,
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "dcterms": "http://purl.org/dc/terms/",
    "prov": "http://www.w3.org/ns/prov#",
}

CONFORMANCE = {"SHALL", "SHOULD", "MAY", "SHALL NOT", "SHOULD NOT"}
CATEGORIES = {"scope", "architecture", "functional", "api", "security", "non-functional", "test"}
DERIVATIONS = {"direct", "derived", "directive"}
TARGETS = {"kotlin", "rust"}

AREA_TITLES = collections.OrderedDict([
    ("GEN", "Scope, architecture and language strategy"),
    ("SEL", "Step 1: select payload type"),
    ("PAY", "Step 2: assemble payload"),
    ("CWT", "Step 3: CWT claim set"),
    ("CBR", "Step 4: CBOR"),
    ("COS", "Step 5: COSE_Sign1"),
    ("KID", "Key identifier"),
    ("ZIP", "Step 6: compression"),
    ("B45", "Step 7: Base45"),
    ("HC1", "Step 8: context identifier"),
    ("QR", "Step 9: QR code"),
    ("VER", "Verification"),
    ("TRU", "Trust list"),
    ("ISS", "Issuance"),
    ("SEC", "Security"),
    ("NFR", "Non-functional"),
    ("TST", "Conformance testing"),
])

# Vocabulary: (local name, kind, label, comment)
VOCAB_TERMS = [
    ("Requirement", "class", "Library requirement", "A derived requirement on the Kotlin/Rust HCERT libraries."),
    ("Derivation", "class", "Derivation", "Reified derivation link from a requirement to a source, with optional quote."),
    ("Document", "class", "Source document", "A source page, file, standard or stakeholder directive."),
    ("Section", "class", "Source section", "A headed section of a source document."),
    ("UpstreamRequirement", "class", "Upstream requirement", "A FHIR Requirements resource in the smart-trust IG."),
    ("UpstreamStatement", "class", "Upstream requirement statement", "A statement inside a smart-trust FHIR Requirements resource."),
    ("Actor", "class", "Actor", "A smart-trust ActorDefinition."),
    ("PipelineStep", "class", "Pipeline step", "A step of the HCERT QR pipeline."),
    ("ApiModule", "class", "API module", "A library module."),
    ("ApiElement", "class", "API element", "A type, interface, function or enum of the library API."),
    ("OpenIssue", "class", "Open issue", "A gap, ambiguity or inconsistency found in the sources."),
    ("statement", "datatype", "statement", "Normative requirement text."),
    ("conformance", "datatype", "conformance", "BCP 14 conformance verb."),
    ("category", "datatype", "category", ""),
    ("derivationType", "datatype", "derivation type", "direct, derived or directive."),
    ("target", "datatype", "target language", ""),
    ("rationale", "datatype", "rationale", ""),
    ("quote", "datatype", "quote", "Verbatim text from the source."),
    ("heading", "datatype", "heading", ""),
    ("anchor", "datatype", "anchor", ""),
    ("path", "datatype", "repository path", ""),
    ("status", "datatype", "publication status", "deployed or pending-merge."),
    ("gitCommit", "datatype", "git commit", ""),
    ("pinnedUrl", "object", "pinned URL", "Source at the exact commit drafted against."),
    ("mainUrl", "object", "main-branch source URL", "Source on main (valid for pending-merge content once merged)."),
    ("previewUrl", "object", "PR preview URL", "IG build of the PR branch."),
    ("ciUrl", "object", "CI build URL", "Continuous build of main."),
    ("publishedUrl", "object", "published URL", "Canonical released IG."),
    ("url", "object", "URL", ""),
    ("canonical", "object", "canonical URL", ""),
    ("order", "datatype", "order", ""),
    ("output", "datatype", "output", ""),
    ("kind", "datatype", "kind", ""),
    ("kotlinSignature", "datatype", "Kotlin sketch", ""),
    ("rustSignature", "datatype", "Rust sketch", ""),
    ("kotlinName", "datatype", "Kotlin package", ""),
    ("rustName", "datatype", "Rust crate", ""),
    ("upstreamFix", "datatype", "fix belongs upstream", ""),
    ("resolutionHint", "datatype", "resolution hint", ""),
    ("hasDerivation", "object", "has derivation", ""),
    ("source", "object", "source", ""),
    ("implementsStep", "object", "implements step", ""),
    ("satisfies", "object", "satisfies", "Upstream smart-trust requirement this requirement helps satisfy."),
    ("refines", "object", "refines", ""),
    ("constrainsApi", "object", "constrains API element", ""),
    ("verifiedBy", "object", "verified by", ""),
    ("affectedBy", "object", "affected by issue", ""),
    ("concerns", "object", "concerns", ""),
    ("inModule", "object", "in module", ""),
    ("actor", "object", "actor", ""),
    ("statementOf", "object", "statement of", ""),
    ("definedIn", "object", "defined in", ""),
]


class BuildError(Exception):
    pass


def slug(heading):
    """GitHub / kramdown-GFM header id."""
    s = heading.strip().lower()
    s = re.sub(r"[^\w\- ]", "", s)
    return s.replace(" ", "-")


def load(name):
    with open(KG_DIR / name, encoding="utf-8") as f:
        return yaml.safe_load(f)


# ---------------------------------------------------------------------------
# Graph
# ---------------------------------------------------------------------------

class Graph:
    def __init__(self):
        self.nodes = collections.OrderedDict()

    def node(self, iri, *types):
        n = self.nodes.setdefault(iri, {"types": [], "props": []})
        for t in types:
            if t not in n["types"]:
                n["types"].append(t)
        return n

    def add(self, iri, pred, obj, iri_obj=False, dtype=None):
        if obj is None or obj == "":
            return
        self.node(iri)["props"].append((pred, obj, iri_obj, dtype))


def expand(curie):
    p, local = curie.split(":", 1)
    return PREFIXES[p] + local


def ttl_term(value):
    if value.startswith("http://") or value.startswith("https://"):
        return "<%s>" % value
    return value


def ttl_literal(value, dtype=None):
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, int):
        return str(value)
    s = str(value)
    if "\n" in s or '"' in s:
        s = s.replace("\\", "\\\\").replace('"""', '\\"\\"\\"')
        lit = '"""%s"""' % s
    else:
        lit = '"%s"' % s.replace("\\", "\\\\")
    return lit + ("^^%s" % dtype if dtype else "")


def to_turtle(g):
    out = ["@prefix %s: <%s> ." % (p, u) for p, u in PREFIXES.items()]
    out.append("")
    for iri, n in g.nodes.items():
        lines = []
        if n["types"]:
            lines.append("a " + ", ".join(n["types"]))
        for pred, obj, iri_obj, dtype in n["props"]:
            o = ttl_term(obj) if iri_obj else ttl_literal(obj, dtype)
            lines.append("%s %s" % (pred, o))
        out.append(ttl_term(iri))
        out.append("    " + " ;\n    ".join(lines) + " .")
        out.append("")
    return "\n".join(out)


def to_jsonld(g):
    graph = []
    for iri, n in g.nodes.items():
        obj = {"@id": iri}
        if n["types"]:
            obj["@type"] = n["types"] if len(n["types"]) > 1 else n["types"][0]
        for pred, o, iri_obj, dtype in n["props"]:
            if iri_obj:
                val = {"@id": o}
            elif dtype:
                val = {"@value": str(o), "@type": dtype}
            else:
                val = o
            obj.setdefault(pred, []).append(val)
        for k, v in list(obj.items()):
            if isinstance(v, list) and len(v) == 1 and k not in ("@type",):
                obj[k] = v[0]
        graph.append(obj)
    return json.dumps({"@context": PREFIXES, "@graph": graph}, indent=2, ensure_ascii=False) + "\n"


# ---------------------------------------------------------------------------
# Load, resolve URLs, validate
# ---------------------------------------------------------------------------

def build_model():
    src = load("sources.yaml")
    up = load("upstream.yaml")
    steps = load("steps.yaml")["steps"]
    api = load("api.yaml")
    issues = load("issues.yaml")["issues"]
    reqs = load("requirements.yaml")["requirements"]

    errors, warnings = [], []
    ids = {}

    def claim(i, kind):
        if i in ids:
            errors.append("duplicate id %s (%s and %s)" % (i, ids[i], kind))
        ids[i] = kind

    repo = src["repo"]
    refs = src["refs"]
    sites = src["sites"]

    docs, sections = collections.OrderedDict(), collections.OrderedDict()
    for d in src["documents"]:
        claim(d["id"], "document")
        d = dict(d)
        urls = {}
        if "path" in d:
            ref = refs[d["ref"]]
            d["sha"] = ref["sha"]
            urls["pinned"] = "https://github.com/%s/blob/%s/%s" % (repo, ref["sha"], d["path"])
            urls["main"] = "https://github.com/%s/blob/main/%s" % (repo, d["path"])
        if "page" in d:
            ref = refs[d["ref"]]
            if d.get("status") == "pending-merge" and "preview_dir" in ref:
                urls["preview"] = "%s/%s/%s" % (sites["preview"], ref["preview_dir"], d["page"])
            urls["ci"] = "%s/%s" % (sites["ci"], d["page"])
            urls["published"] = "%s/%s" % (sites["published"], d["page"])
        if "url" in d:
            urls["url"] = d["url"]
        d["urls"] = urls
        docs[d["id"]] = d
        for s in d.get("sections", []):
            claim(s["id"], "section")
            s = dict(s)
            s["doc"] = d["id"]
            s["anchor"] = slug(s["heading"])
            s["urls"] = {k: "%s#%s" % (u, s["anchor"]) for k, u in urls.items() if k != "url"}
            sections[s["id"]] = s

    actors = {a["id"]: a for a in up["actors"]}
    for a in actors:
        claim(a, "actor")
    upreqs, upstmts = collections.OrderedDict(), collections.OrderedDict()
    for r in up["requirements"]:
        claim(r["id"], "upstream requirement")
        upreqs[r["id"]] = r
        if r["actor"] not in actors:
            errors.append("upstream %s: unknown actor %s" % (r["id"], r["actor"]))
        for s in r["statements"]:
            claim(s["key"], "upstream statement")
            upstmts[s["key"]] = dict(s, parent=r["id"])

    stepmap = collections.OrderedDict()
    for s in steps:
        claim(s["id"], "step")
        stepmap[s["id"]] = s
        if s["src"] not in sections and s["src"] not in docs:
            errors.append("step %s: unknown src %s" % (s["id"], s["src"]))

    modules = collections.OrderedDict((m["id"], m) for m in api["modules"])
    for m in modules:
        claim(m, "module")
    elements = collections.OrderedDict()
    for e in api["elements"]:
        claim(e["id"], "api element")
        elements[e["id"]] = e
        if e["module"] not in modules:
            errors.append("api %s: unknown module %s" % (e["id"], e["module"]))

    issuemap = collections.OrderedDict()
    for i in issues:
        claim(i["id"], "issue")
        issuemap[i["id"]] = i
        for s in i.get("sources", []):
            if s not in sections and s not in docs:
                errors.append("issue %s: unknown source %s" % (i["id"], s))

    reqmap = collections.OrderedDict()
    for r in reqs:
        claim(r["id"], "requirement")
        reqmap[r["id"]] = r

    for r in reqs:
        rid = r["id"]
        m = re.match(r"STT-([A-Z0-9]+)-\d{3}$", rid)
        if not m or m.group(1) not in AREA_TITLES:
            errors.append("%s: id must be STT-<AREA>-nnn with a known area" % rid)
        if r.get("conformance") not in CONFORMANCE:
            errors.append("%s: bad conformance %r" % (rid, r.get("conformance")))
        elif r["conformance"] not in r["statement"]:
            warnings.append("%s: statement does not contain its conformance verb %s" % (rid, r["conformance"]))
        if r.get("category") not in CATEGORIES:
            errors.append("%s: bad category %r" % (rid, r.get("category")))
        if r.get("derivation") not in DERIVATIONS:
            errors.append("%s: bad derivation %r" % (rid, r.get("derivation")))
        for t in r.get("targets", []):
            if t not in TARGETS:
                errors.append("%s: bad target %s" % (rid, t))
        if not r.get("derived_from"):
            errors.append("%s: no derived_from" % rid)
        for d in r.get("derived_from", []):
            if d["src"] not in sections and d["src"] not in docs:
                errors.append("%s: unknown source %s" % (rid, d["src"]))
        if r.get("derivation") == "directive" and not any(d["src"].startswith("dir-") for d in r.get("derived_from", [])):
            errors.append("%s: directive derivation must cite a dir-* source" % rid)
        if r.get("derivation") == "direct" and not any(d.get("quote") for d in r.get("derived_from", [])):
            warnings.append("%s: direct derivation without a quote" % rid)
        for k, table in (("steps", stepmap), ("satisfies", None), ("refines", reqmap),
                         ("verified_by", reqmap), ("api", elements), ("issues", issuemap)):
            for v in r.get(k, []):
                if k == "satisfies":
                    if v not in upstmts and v not in upreqs:
                        errors.append("%s: satisfies unknown upstream %s" % (rid, v))
                elif v not in table:
                    errors.append("%s: %s references unknown %s" % (rid, k, v))
        for v in r.get("verified_by", []):
            if v in reqmap and reqmap[v]["category"] != "test":
                errors.append("%s: verified_by %s is not a test requirement" % (rid, v))

    for s in stepmap:
        if not any(s in r.get("steps", []) for r in reqs):
            errors.append("step %s has no requirement" % s)
    for k in upstmts:
        if not any(k in r.get("satisfies", []) for r in reqs):
            warnings.append("upstream statement %s is not satisfied by any library requirement" % k)
    for i in issuemap:
        if issuemap[i].get("about") == "kg":
            continue  # concerns the graph itself, not a requirement
        if not any(i in r.get("issues", []) for r in reqs):
            warnings.append("issue %s is not linked from any requirement" % i)

    if errors:
        raise BuildError("\n".join(errors))

    return {
        "src": src, "docs": docs, "sections": sections, "actors": actors,
        "upreqs": upreqs, "upstmts": upstmts, "steps": stepmap,
        "modules": modules, "elements": elements, "issues": issuemap,
        "reqs": reqmap, "warnings": warnings,
    }


# ---------------------------------------------------------------------------
# RDF
# ---------------------------------------------------------------------------

def build_graph(M):
    g = Graph()
    I = lambda x: ID + x  # noqa: E731

    for local, kind, label, comment in VOCAB_TERMS:
        iri = VOCAB + local
        t = {"class": "rdfs:Class", "datatype": "rdf:Property", "object": "rdf:Property"}[kind]
        g.node(iri, t)
        g.add(iri, "rdfs:label", label)
        g.add(iri, "rdfs:comment", comment)

    url_pred = {"pinned": "stt:pinnedUrl", "main": "stt:mainUrl", "preview": "stt:previewUrl",
                "ci": "stt:ciUrl", "published": "stt:publishedUrl", "url": "stt:url"}

    for d in M["docs"].values():
        iri = I(d["id"])
        g.node(iri, "stt:Document")
        g.add(iri, "dcterms:identifier", d["id"])
        g.add(iri, "dcterms:title", d["title"])
        g.add(iri, "stt:path", d.get("path"))
        g.add(iri, "stt:status", d.get("status"))
        g.add(iri, "stt:gitCommit", d.get("sha"))
        g.add(iri, "rdfs:comment", d.get("note"))
        for k, u in d["urls"].items():
            g.add(iri, url_pred[k], u, iri_obj=True)

    for s in M["sections"].values():
        iri = I(s["id"])
        g.node(iri, "stt:Section")
        g.add(iri, "dcterms:identifier", s["id"])
        g.add(iri, "stt:heading", s["heading"])
        g.add(iri, "stt:anchor", s["anchor"])
        g.add(iri, "dcterms:isPartOf", I(s["doc"]), iri_obj=True)
        for k, u in s["urls"].items():
            g.add(iri, url_pred[k], u, iri_obj=True)

    sites = M["src"]["sites"]
    for a in M["actors"].values():
        iri = I(a["id"])
        g.node(iri, "stt:Actor")
        g.add(iri, "dcterms:title", a["name"])
        g.add(iri, "stt:canonical", a["canonical"], iri_obj=True)
        g.add(iri, "stt:publishedUrl", "%s/%s" % (sites["published"], a["page"]), iri_obj=True)

    for r in M["upreqs"].values():
        iri = I(r["id"])
        g.node(iri, "stt:UpstreamRequirement")
        g.add(iri, "dcterms:identifier", r["id"])
        g.add(iri, "dcterms:title", r["title"])
        g.add(iri, "stt:actor", I(r["actor"]), iri_obj=True)
        g.add(iri, "stt:canonical", "http://smart.who.int/trust/Requirements/" + r["id"], iri_obj=True)
        g.add(iri, "stt:ciUrl", "%s/Requirements-%s.html" % (sites["ci"], r["id"]), iri_obj=True)
        g.add(iri, "stt:publishedUrl", "%s/Requirements-%s.html" % (sites["published"], r["id"]), iri_obj=True)
        g.add(iri, "stt:definedIn", I("doc-fhir-requirements"), iri_obj=True)
    for s in M["upstmts"].values():
        iri = I(s["key"])
        g.node(iri, "stt:UpstreamStatement")
        g.add(iri, "dcterms:identifier", s["key"])
        g.add(iri, "dcterms:title", s["label"])
        g.add(iri, "stt:conformance", s["conformance"])
        g.add(iri, "stt:statementOf", I(s["parent"]), iri_obj=True)

    for s in M["steps"].values():
        iri = I(s["id"])
        g.node(iri, "stt:PipelineStep")
        g.add(iri, "dcterms:title", s["name"])
        g.add(iri, "stt:order", s["order"])
        g.add(iri, "stt:output", s["output"])
        g.add(iri, "prov:wasDerivedFrom", I(s["src"]), iri_obj=True)

    for m in M["modules"].values():
        iri = I(m["id"])
        g.node(iri, "stt:ApiModule")
        g.add(iri, "dcterms:title", m["name"])
        g.add(iri, "dcterms:description", m["description"])
        g.add(iri, "stt:kotlinName", m["kotlin"])
        g.add(iri, "stt:rustName", m["rust"])
    for e in M["elements"].values():
        iri = I(e["id"])
        g.node(iri, "stt:ApiElement")
        g.add(iri, "dcterms:title", e["name"])
        g.add(iri, "stt:kind", e["kind"])
        g.add(iri, "dcterms:description", e["description"])
        g.add(iri, "stt:inModule", I(e["module"]), iri_obj=True)
        g.add(iri, "stt:kotlinSignature", e["kotlin"])
        g.add(iri, "stt:rustSignature", e["rust"])

    for i in M["issues"].values():
        iri = I(i["id"])
        g.node(iri, "stt:OpenIssue")
        g.add(iri, "dcterms:identifier", i["id"])
        g.add(iri, "dcterms:title", i["title"])
        g.add(iri, "dcterms:description", i["description"].strip())
        g.add(iri, "stt:upstreamFix", bool(i.get("upstream")))
        g.add(iri, "stt:resolutionHint", (i.get("resolution_hint") or "").strip())
        for s in i.get("sources", []):
            g.add(iri, "stt:concerns", I(s), iri_obj=True)

    for r in M["reqs"].values():
        iri = I(r["id"])
        g.node(iri, "stt:Requirement")
        g.add(iri, "dcterms:identifier", r["id"])
        g.add(iri, "dcterms:title", r["title"])
        g.add(iri, "stt:statement", r["statement"].strip())
        g.add(iri, "stt:conformance", r["conformance"])
        g.add(iri, "stt:category", r["category"])
        g.add(iri, "stt:derivationType", r["derivation"])
        g.add(iri, "stt:rationale", r.get("rationale"))
        for t in r.get("targets", sorted(TARGETS)):
            g.add(iri, "stt:target", t)
        for n, d in enumerate(r["derived_from"], 1):
            div = "%s/derivation/%d" % (iri, n)
            g.add(iri, "prov:wasDerivedFrom", I(d["src"]), iri_obj=True)
            g.add(iri, "stt:hasDerivation", div, iri_obj=True)
            g.node(div, "stt:Derivation")
            g.add(div, "stt:source", I(d["src"]), iri_obj=True)
            g.add(div, "stt:quote", d.get("quote"))
        for k, pred in (("steps", "stt:implementsStep"), ("satisfies", "stt:satisfies"),
                        ("refines", "stt:refines"), ("api", "stt:constrainsApi"),
                        ("verified_by", "stt:verifiedBy"), ("issues", "stt:affectedBy")):
            for v in r.get(k, []):
                g.add(iri, pred, I(v), iri_obj=True)
    return g


# ---------------------------------------------------------------------------
# Markdown
# ---------------------------------------------------------------------------

def md_escape(s):
    return str(s).replace("|", "\\|").replace("\n", " ")


def src_links(M, sid):
    if sid in M["sections"]:
        s = M["sections"][sid]
        d = M["docs"][s["doc"]]
        label = "%s § %s" % (d["title"], s["heading"].replace("`", ""))
        urls = s["urls"]
    else:
        d = M["docs"][sid]
        label = d["title"]
        urls = d["urls"]
    main = urls.get("pinned") or urls.get("url") or urls.get("published")
    out = "[%s](%s)" % (label, main) if main else label
    extra = []
    if d.get("status") == "pending-merge":
        if "preview" in urls:
            extra.append("[PR preview](%s)" % urls["preview"])
        if "published" in urls:
            extra.append("[after deploy](%s)" % urls["published"])
        elif "main" in urls:
            extra.append("[after merge](%s)" % urls["main"])
    elif "published" in urls:
        extra.append("[published](%s)" % urls["published"])
    if extra:
        out += " (" + " · ".join(extra) + ")"
    return out


def upstream_link(M, key):
    sites = M["src"]["sites"]
    parent = M["upstmts"][key]["parent"] if key in M["upstmts"] else key
    return "[%s](%s/Requirements-%s.html)" % (key, sites["published"], parent)


def req_anchor(rid):
    return rid.lower()


def to_markdown(M):
    src = M["src"]
    reqs = list(M["reqs"].values())
    L = []
    w = L.append
    w("<!-- GENERATED by tools/build_kg.py from requirements/kg/*.yaml. Do not edit. -->")
    w("")
    w("# SMART Trust HCERT API library requirements")
    w("")
    w("Derived requirements for the Kotlin (phase 1) and Rust (phase 2) libraries that "
      "implement the HCERT QR pipeline described in the WHO SMART Trust IG, in particular "
      "the *QR Code Generation* page added by "
      "[WorldHealthOrganization/smart-trust#129](https://github.com/WorldHealthOrganization/smart-trust/pull/129).")
    w("")
    w("The source of truth is the YAML under [`requirements/kg/`](../kg/). This page and the "
      "RDF graph ([`kg.ttl`](kg.ttl), [`kg.jsonld`](kg.jsonld)) are generated from it.")
    w("")
    w("## How sources are referenced")
    w("")
    w("Every smart-trust reference carries up to four URLs, so links work today and after PR 129 is merged and deployed:")
    w("")
    w("| Tier | Meaning | Base |")
    w("|---|---|---|")
    for k, v in src["refs"].items():
        w("| pinned (`%s`) | %s | `%s` |" % (k, md_escape(v["label"]), v["sha"][:12]))
    w("| PR preview | IG build of the PR branch | %s/%s/ |" % (src["sites"]["preview"], src["refs"]["pr129"]["preview_dir"]))
    w("| CI build | continuous build of main | %s/ |" % src["sites"]["ci"])
    w("| published | canonical released IG | %s/ |" % src["sites"]["published"])
    w("")
    w("Links below go to the pinned source. Content from PR 129 also links to its PR preview and to "
      "where it will be published. Section anchors are computed from headings (see OI-10).")
    w("")

    counts = collections.Counter(r["conformance"] for r in reqs)
    cats = collections.Counter(r["category"] for r in reqs)
    der = collections.Counter(r["derivation"] for r in reqs)
    w("## Summary")
    w("")
    w("- **%d requirements**: %s" % (len(reqs), ", ".join("%d %s" % (counts[c], c) for c in sorted(counts))))
    w("- by category: %s" % ", ".join("%s %d" % (c, n) for c, n in sorted(cats.items())))
    w("- by derivation: %s" % ", ".join("%s %d" % (c, n) for c, n in sorted(der.items())))
    w("- **%d open issues** (%d to fix upstream in smart-trust)" % (
        len(M["issues"]), sum(1 for i in M["issues"].values() if i.get("upstream"))))
    w("")

    w("## Pipeline")
    w("")
    w("```mermaid")
    w("flowchart LR")
    order = [s for s in M["steps"].values() if s["id"].startswith("step-") and s["id"][5:].isdigit()]
    for s in order:
        w('  %s["%d. %s"]' % (s["id"].replace("-", ""), s["order"], s["name"].replace('"', "'")))
    for a, b in zip(order, order[1:]):
        w("  %s --> %s" % (a["id"].replace("-", ""), b["id"].replace("-", "")))
    w('  stepverify["Verify (reverse)"]')
    w('  steptrust["Trust list"]')
    w("  step9 -. scan .-> stepverify")
    w("  steptrust --> stepverify")
    w("```")
    w("")
    w("| Step | Source | Requirements |")
    w("|---|---|---|")
    for s in M["steps"].values():
        rs = [r["id"] for r in reqs if s["id"] in r.get("steps", [])]
        w("| %s. %s | %s | %s |" % (s["order"], md_escape(s["name"]), src_links(M, s["src"]),
                                    ", ".join("[%s](#%s)" % (x, req_anchor(x)) for x in rs)))
    w("")

    w("## Requirements")
    w("")
    by_area = collections.OrderedDict((a, []) for a in AREA_TITLES)
    for r in reqs:
        by_area[r["id"].split("-")[1]].append(r)
    for area, rs in by_area.items():
        if not rs:
            continue
        w("### %s — %s" % (area, AREA_TITLES[area]))
        w("")
        for r in rs:
            w('<a id="%s"></a>' % req_anchor(r["id"]))
            w("")
            w("#### %s %s" % (r["id"], r["title"]))
            w("")
            w("> %s" % " ".join(r["statement"].split()))
            w("")
            meta = ["**%s**" % r["conformance"], r["category"], r["derivation"],
                    "targets: " + ", ".join(r.get("targets", ["kotlin", "rust"]))]
            w(" · ".join(meta))
            w("")
            w("- Derived from:")
            for d in r["derived_from"]:
                line = "  - " + src_links(M, d["src"])
                if d.get("quote"):
                    line += " — “%s”" % " ".join(d["quote"].split())
                w(line)
            if r.get("steps"):
                w("- Steps: " + ", ".join("%s" % M["steps"][s]["name"] for s in r["steps"]))
            if r.get("satisfies"):
                w("- Satisfies (smart-trust): " + ", ".join(upstream_link(M, k) for k in r["satisfies"]))
            if r.get("refines"):
                w("- Refines: " + ", ".join("[%s](#%s)" % (x, req_anchor(x)) for x in r["refines"]))
            if r.get("api"):
                w("- API: " + ", ".join("[`%s`](#%s)" % (M["elements"][x]["name"], x) for x in r["api"]))
            if r.get("verified_by"):
                w("- Verified by: " + ", ".join("[%s](#%s)" % (x, req_anchor(x)) for x in r["verified_by"]))
            if r.get("issues"):
                w("- Open issues: " + ", ".join("[%s](#%s)" % (x, x.lower()) for x in r["issues"]))
            w("")

    w("## Traceability to smart-trust requirements")
    w("")
    w("| smart-trust statement | Library requirements |")
    w("|---|---|")
    keys = list(M["upstmts"]) + [k for k in M["upreqs"] if not M["upreqs"][k]["statements"]]
    for k in keys:
        rs = [r["id"] for r in reqs if k in r.get("satisfies", [])]
        w("| %s | %s |" % (upstream_link(M, k), ", ".join("[%s](#%s)" % (x, req_anchor(x)) for x in rs) or "—"))
    w("")

    w("## Source coverage")
    w("")
    w("| Source | Requirements |")
    w("|---|---|")
    for sid in list(M["sections"]) + list(M["docs"]):
        rs = [r["id"] for r in reqs if any(d["src"] == sid for d in r["derived_from"])]
        if rs:
            w("| %s | %s |" % (src_links(M, sid), ", ".join("[%s](#%s)" % (x, req_anchor(x)) for x in rs)))
    w("")

    w("## Open issues")
    w("")
    for i in M["issues"].values():
        w('<a id="%s"></a>' % i["id"].lower())
        w("")
        w("### %s %s%s" % (i["id"], i["title"], " *(fix upstream)*" if i.get("upstream") else ""))
        w("")
        w(" ".join(i["description"].split()))
        w("")
        if i.get("resolution_hint"):
            w("*Suggested resolution:* " + " ".join(i["resolution_hint"].split()))
            w("")
        if i.get("sources"):
            w("Sources: " + "; ".join(src_links(M, s) for s in i["sources"]))
            w("")
        rs = [r["id"] for r in reqs if i["id"] in r.get("issues", [])]
        if rs:
            w("Affects: " + ", ".join("[%s](#%s)" % (x, req_anchor(x)) for x in rs))
            w("")

    w("## API sketch")
    w("")
    w("Language-neutral elements with intended Kotlin and Rust shapes. Names are placeholders (OI-14).")
    w("")
    for m in M["modules"].values():
        els = [e for e in M["elements"].values() if e["module"] == m["id"]]
        w("### `%s` — %s" % (m["name"], m["description"]))
        w("")
        w("Kotlin `%s` · Rust `%s`" % (m["kotlin"], m["rust"]))
        w("")
        for e in els:
            w('<a id="%s"></a>' % e["id"])
            w("")
            w("**`%s`** (%s): %s" % (e["name"], e["kind"], e["description"]))
            rs = [r["id"] for r in reqs if e["id"] in r.get("api", [])]
            if rs:
                w("Constrained by " + ", ".join("[%s](#%s)" % (x, req_anchor(x)) for x in rs) + ".")
            w("")
            w("```kotlin")
            w(e["kotlin"])
            w("```")
            w("```rust")
            w(e["rust"])
            w("```")
            w("")
    return "\n".join(L).rstrip() + "\n"


# ---------------------------------------------------------------------------

def all_urls(M):
    urls = set()
    for d in M["docs"].values():
        urls.update(d["urls"].values())
    for s in M["sections"].values():
        urls.update(u.split("#")[0] for u in s["urls"].values())
    return sorted(urls)


def check_links(M):
    bad = []
    for u in all_urls(M):
        req = urllib.request.Request(u, method="HEAD", headers={"User-Agent": "smart-trust-tools/build_kg"})
        try:
            with urllib.request.urlopen(req, timeout=20) as resp:
                code = resp.status
        except urllib.error.HTTPError as e:
            code = e.code
        except Exception as e:  # network errors are reported, not fatal
            code = type(e).__name__
        if code != 200:
            bad.append((code, u))
        print("%s %s" % (code, u))
    return bad


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--check", action="store_true", help="fail if generated files are out of date")
    ap.add_argument("--check-links", action="store_true", help="probe every URL over HTTP")
    args = ap.parse_args()

    try:
        M = build_model()
    except BuildError as e:
        print("KG validation failed:\n" + str(e), file=sys.stderr)
        return 1
    for wmsg in M["warnings"]:
        print("warning: " + wmsg, file=sys.stderr)

    g = build_graph(M)
    outputs = {
        "kg.ttl": to_turtle(g),
        "kg.jsonld": to_jsonld(g),
        "REQUIREMENTS.md": to_markdown(M),
    }

    if args.check:
        stale = [n for n, c in outputs.items()
                 if not (OUT_DIR / n).exists() or (OUT_DIR / n).read_text(encoding="utf-8") != c]
        if stale:
            print("stale generated files: %s (run tools/build_kg.py)" % ", ".join(stale), file=sys.stderr)
            return 1
        print("generated files are up to date")
    else:
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        for n, c in outputs.items():
            (OUT_DIR / n).write_text(c, encoding="utf-8")
        print("wrote %s: %d requirements, %d nodes" % (
            ", ".join(outputs), len(M["reqs"]), len(g.nodes)))

    if args.check_links:
        bad = check_links(M)
        if bad:
            print("%d URLs did not return 200 (expected for pending-merge content)" % len(bad), file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
