"""Read this instance's cat-harness nodes into the graph the viewers draw.

Every node and edge here is read from a committed node, never restated:

  requirement   skills/requirements/*.json (bootstrap RequirementSchema)
  section       a library entry's sections (library/<slug>/structure.json)
  document      a library entry, a reference (references/*.json), a test vector
  standard      an external schema (external-schemas/*.json)
  directive     an owner directive (memory/dir-*.md)
  upstream      a smart-trust FHIR Requirements instance or statement, read
                from the ingested Requirements.fsh
  skill         skills/<package>/*.md (front matter `satisfies`)
  tool          tools/<id>/<id>.ts (`satisfies`), read through `bun`
  process       processes/**/*.bpmn (tasks naming skills)
  role          scenarios/roles.json (a role's skills)
  api           an item of the WIT contract, with the [STT-...] tags its doc
                comment carries
  issue         beans/defs/*.md (Requirements affected, Sources)

Edges: derivedFrom (a requirement's `reference`), satisfies (a smart-trust
canonical in `reference`), refines (its `derivedFrom`), verifiedBy,
realizedBy (requirement to skill), constrainsApi, affectedBy, concerns,
partOf, implementedBy (skill to Tool), usedIn (skill to process),
performedBy (skill to role).
"""

import collections
import json
import pathlib
import re
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[2]
UPSTREAM = "http://smart.who.int/trust/Requirements/"
UPSTREAM_SITE = "https://smart.who.int/trust"

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
    ("VHL", "Verifiable Health Link"),
    ("ISS", "Issuance"),
    ("SEC", "Security"),
    ("NFR", "Non-functional"),
    ("TST", "Conformance testing"),
])

ENTRY_SHORT = {
    "who-smart-trust-qr-code-generation": "QR", "who-smart-trust-hcert-spec": "HCERT",
    "who-smart-trust-did-gdhcn": "DID", "who-smart-trust-endpoints": "Endpoints",
    "who-smart-trust-changes": "Changes", "who-smart-trust-trust-domains": "Domains",
    "who-smart-trust-data-exchange": "Data exchange", "who-smart-trust-reference-implementation": "Ref. impl.",
    "ihe-iti-vhl-volume-1": "VHL Vol. 1", "ihe-iti-vhl-iti-yy3": "ITI-YY3", "ihe-iti-vhl-iti-yy4": "ITI-YY4",
    "ihe-iti-vhl-iti-yy5": "ITI-YY5",
}

# The skills of the HCERT pipeline, in the order of QR Code Generation.
PIPELINE = ["hcert-select-payload", "hcert-assemble-payload", "hcert-build-cwt", "hcert-encode-cbor",
            "hcert-sign-cose", "hcert-compress", "hcert-encode-base45", "hcert-prefix-context",
            "hcert-verify", "hcert-trust-list"]

STT_TAG = re.compile(r"STT-[A-Z0-9]+-\d{3}")


def front_matter(text):
    """The YAML front matter of a Markdown node, parsed for the few shapes nodes here use."""
    import yaml
    m = re.match(r"^---\n(.*?)\n---\n?(.*)$", text, flags=re.S)
    return (yaml.safe_load(m.group(1)) or {}, m.group(2)) if m else ({}, text)


def display_id(req_id):
    """req:stt-cos-002 -> STT-COS-002, the id the IG and the WIT comments use."""
    return req_id[len("req:"):].upper()


def load_library():
    entries, sections = {}, {}
    for st in sorted((ROOT / "library").glob("*/structure.json")):
        d = json.loads(st.read_text())
        slug = d["doc_id"]
        up = d["source"].get("upstream", {})
        repo = up.get("repository", "").rstrip("/")
        pinned = "%s/blob/%s/%s" % (repo, up["commit"], up["path"]) if repo and up.get("commit") else None
        eid = "library/" + slug
        entries[eid] = {
            "slug": slug, "title": d["metadata"].get("title") or slug, "short": ENTRY_SHORT.get(slug),
            "urls": {k: v for k, v in (("pinned", pinned), ("published", up.get("published"))) if v},
            "repository": repo, "commit": up.get("commit"), "ref": up.get("ref"),
        }
        for s in d["sections"]:
            urls = {}
            if pinned:
                urls["pinned"] = pinned + ("#L%d-L%d" % (s["line_start"], s["line_end"]) if s.get("line_start") else "")
            if up.get("published"):
                urls["published"] = up["published"]
            sections["%s/sections/%s" % (eid, s["id"])] = {"entry": eid, "heading": s["title"], "urls": urls}
    return entries, sections


def section_label(entries, sec):
    e = entries[sec["entry"]]
    short = e["short"] or e["title"][:24]
    m = re.match(r"^(\d+(?:\.\d+)*|[A-Z](?:\.\d+)?)\.?\s", sec["heading"])
    if m:
        return "%s §%s" % (short, m.group(1))
    m = re.match(r"^(\d+:\d+(?:\.[A-Z0-9]+)+)\s", sec["heading"])
    if m:
        return "%s %s" % (short, m.group(1))
    return "%s: %s" % (short, sec["heading"][:28])


def load_upstream():
    """smart-trust FHIR Requirements: {instance: {title, statements: {key: label}}}."""
    out = collections.OrderedDict()
    for f in sorted((ROOT / "library" / "who-smart-trust-requirements-fsh" / "sections").glob("*.md")):
        cur, key = None, None
        for line in f.read_text().split("\n"):
            m = re.match(r"^Instance:\s+(\S+)", line)
            if m:
                cur = out.setdefault(m.group(1), {"title": m.group(1), "statements": collections.OrderedDict()})
                continue
            m = re.match(r'^\* title = "([^"]+)"', line)
            if m and cur:
                cur["title"] = m.group(1)
            m = re.match(r'^\* statement\[\+\]\.key = "([^"]+)"', line)
            if m and cur:
                key = m.group(1)
                cur["statements"][key] = key
            m = re.match(r'^\* statement\[=\]\.label = "([^"]+)"', line)
            if m and cur and key:
                cur["statements"][key] = m.group(1)
    return out


def load_requirements():
    reqs = collections.OrderedDict()
    for p in sorted((ROOT / "skills" / "requirements").glob("*.json")):
        n = json.loads(p.read_text())
        st = n["statements"][0]
        tags = n.get("tags", [])
        reqs[n["id"]] = {
            "id": display_id(n["id"]), "iri": n["id"], "title": n["title"], "description": n.get("description", ""),
            "statement": " ".join(st["requirement"].split()), "conformance": st["conformance"],
            "category": st.get("category", ""), "actors": n.get("actors", []),
            "derivation": next((t for t in tags if t in ("direct", "derived", "directive")), ""),
            "targets": [t.split(":", 1)[1] for t in tags if t.startswith("target:")] or ["kotlin", "rust"],
            "derivedFrom": n.get("derivedFrom", []), "reference": n.get("reference", []),
            "verifiedBy": [c["key"] for s in n["statements"] for c in s.get("successCriteria", [])],
            "area": display_id(n["id"]).split("-")[1], "file": p.relative_to(ROOT).as_posix(),
        }
    return reqs


def load_skills():
    skills = collections.OrderedDict()
    for pkg in sorted(p for p in (ROOT / "skills").iterdir() if p.is_dir() and p.name != "requirements"):
        for f in sorted(pkg.glob("*.md")):
            fm, _ = front_matter(f.read_text())
            skills[fm["name"]] = {
                "name": fm["name"], "package": pkg.name, "description": " ".join(str(fm.get("description", "")).split()),
                "satisfies": [s.split("#")[0] for s in fm.get("satisfies", [])], "file": f.relative_to(ROOT).as_posix(),
            }
    return skills


def load_tools():
    script = ('import {tools} from "./tools/index.ts"; console.log(JSON.stringify(tools().map(t => '
              '({id: t.id, title: t.title, description: t.description, satisfies: t.satisfies}))))')
    out = subprocess.run(["bun", "-e", script], cwd=ROOT, capture_output=True, text=True, check=True).stdout
    return collections.OrderedDict((t["id"], t) for t in json.loads(out))


def load_processes():
    procs = collections.OrderedDict()
    for f in sorted((ROOT / "processes").glob("*/*.bpmn")):
        x = f.read_text()
        m = re.search(r'<bpmn:process id="([^"]+)" name="([^"]+)"', x)
        doc = re.search(r"<bpmn:documentation>(.*?)</bpmn:documentation>", x, flags=re.S)
        procs[f.stem] = {
            "id": f.stem, "name": m.group(2) if m else f.stem, "file": f.relative_to(ROOT).as_posix(),
            "doc": " ".join(doc.group(1).split()) if doc else "",
            "skills": sorted(set(re.findall(r'<bootstrap\.processes:skill ref="([^"]+)"', x))),
        }
    return procs


def load_roles():
    return json.loads((ROOT / "scenarios" / "roles.json").read_text())["roles"]


def load_beans():
    beans = collections.OrderedDict()
    for f in sorted((ROOT / "beans" / "defs").glob("*.md")):
        fm, body = front_matter(f.read_text())
        parts = re.split(r"^## ", body, flags=re.M)
        text = parts[0].strip()
        sec = {p.split("\n", 1)[0].strip(): p.split("\n", 1)[1] if "\n" in p else "" for p in parts[1:]}
        refs = lambda k: re.findall(r"^- (\S+)", sec.get(k, ""), flags=re.M)
        m = re.match(r"^(OI-\d+):\s*(.*)$", fm.get("title", ""))
        beans[f.name] = {
            "id": m.group(1) if m else f.stem, "title": m.group(2) if m else fm.get("title", f.stem),
            "text": " ".join(text.split()), "hint": " ".join(sec.get("Resolution", "").split()),
            "upstream": "upstream" in (fm.get("tags") or []), "status": fm.get("status"),
            "requirements": refs("Requirements affected"), "sources": refs("Sources"),
            "file": f.relative_to(ROOT).as_posix(),
        }
    return beans


def load_wit():
    """Items of the WIT contract: {wit:iface.item: {...}}, with the [STT-...] tags of each doc comment."""
    items = collections.OrderedDict()
    for f in sorted((ROOT / "api" / "wit").glob("*.wit")):
        iface, doc = None, []
        for line in f.read_text().split("\n"):
            s = line.strip()
            if s.startswith("//"):
                doc.append(s)
                continue
            m = re.match(r"^(interface|world)\s+([a-z0-9-]+)", s)
            if m:
                iface = m.group(2)
                key = "wit:" + iface
                items[key] = {"module": iface, "name": iface, "kind": m.group(1), "file": f.name,
                              "tags": sorted(set(STT_TAG.findall(" ".join(doc)))), "doc": doc}
                doc = []
                continue
            m = (re.match(r"^(record|variant|enum|flags|resource|type)\s+([a-z0-9-]+)", s)
                 or re.match(r"^()([a-z0-9-]+):\s*(?:static\s+)?func\b", s))
            if m and iface:
                key = "wit:%s.%s" % (iface, m.group(2))
                if key not in items:
                    items[key] = {"module": iface, "name": m.group(2), "kind": m.group(1) or "func", "file": f.name,
                                  "tags": sorted(set(STT_TAG.findall(" ".join(doc)))), "doc": doc}
            if s and not s.startswith("//"):
                doc = []
    for v in items.values():
        v["wit"] = "%s.%s" % (v["module"], v["name"]) if v["name"] != v["module"] else v["module"]
        v["text"] = " ".join(re.sub(r"^///?\s?", "", d) for d in v.pop("doc"))
    return items


def provenance(entries):
    """The upstream commits the library pins, one line per repository and ref."""
    seen = collections.OrderedDict()
    for e in entries.values():
        if e["repository"] and e["commit"]:
            seen.setdefault((e["repository"], e["commit"]), e["ref"])
    return [{"repository": r, "commit": c, "ref": ref} for (r, c), ref in seen.items()]


def load(with_tools=True):
    """The nodes. Without Tools when the harness is not mounted (CI): tools/index.ts imports it."""
    entries, sections = load_library()
    return {
        "entries": entries, "sections": sections, "upstream": load_upstream(), "reqs": load_requirements(),
        "skills": load_skills(), "tools": load_tools() if with_tools else {}, "processes": load_processes(),
        "roles": load_roles(), "beans": load_beans(), "wit": load_wit(),
    }


FAMILY = {
    "requirement": "requirement",
    "section": "source", "document": "source", "standard": "source", "directive": "source", "upstream": "source",
    "skill": "realization", "api": "realization", "tool": "realization", "process": "realization", "role": "realization",
    "issue": "issue",
}


def build_graph(M):
    nodes = collections.OrderedDict()
    edges = []
    reqs_by_display = {r["id"]: r for r in M["reqs"].values()}

    def add(nid, kind, label, title, **kw):
        if nid not in nodes:
            nodes[nid] = dict(id=nid, kind=kind, fam=FAMILY[kind], label=label, title=title, **kw)
        return nodes[nid]

    def edge(s, t, rel, **kw):
        edges.append(dict(s=s, t=t, rel=rel, **kw))

    def add_upstream(inst, key=None):
        u = M["upstream"][inst]
        url = {"published": "%s/Requirements-%s.html" % (UPSTREAM_SITE, inst)}
        add(UPSTREAM + inst, "upstream", inst, u["title"], urls=url)
        if key is None:
            return UPSTREAM + inst
        nid = "%s%s#%s" % (UPSTREAM, inst, key)
        if nid not in nodes:
            add(nid, "upstream", key, u["statements"].get(key, key), parent=inst, urls=url)
            edge(nid, UPSTREAM + inst, "partOf")
        return nid

    def add_source(ref):
        """The node a derivedFrom or Sources reference names; returns its id."""
        if ref.startswith(UPSTREAM):
            inst, _, key = ref[len(UPSTREAM):].partition("#")
            return add_upstream(inst, key or None)
        if ref in M["sections"]:
            s = M["sections"][ref]
            add(ref, "section", section_label(M["entries"], s), s["heading"].replace("`", ""), doc=s["entry"], urls=s["urls"])
            add_source(s["entry"])
            if not any(e["s"] == ref and e["rel"] == "partOf" for e in edges):
                edge(ref, s["entry"], "partOf")
            return ref
        if ref in M["entries"]:
            e = M["entries"][ref]
            add(ref, "document", e["short"] or e["title"][:34], e["title"], urls=e["urls"], origin="library")
            return ref
        path = ROOT / ref
        if ref.startswith("external-schemas/"):
            d = json.loads(path.with_suffix(".json").read_text())
            add(ref, "standard", d.get("version") or d["id"], d["title"], authority=d.get("authority"),
                urls={"url": d.get("specUrl")}, note=d.get("note", ""))
        elif ref.startswith("references/"):
            d = json.loads(path.with_suffix(".json").read_text())
            add(ref, "document", d["title"][:34], d["title"], urls={"url": d.get("URL")}, origin="reference")
        elif ref.startswith("memory/"):
            fm, body = front_matter(path.with_suffix(".md").read_text())
            add(ref, "directive", fm.get("id", ref), fm.get("summary", ""), note=" ".join(body.split()))
        elif ref.startswith("vectors/"):
            add(ref, "document", path.name, "Test vector " + ref, origin="vector",
                urls={"main": "https://github.com/litlfred/smart-trust-tools/blob/main/" + ref})
        else:
            raise ValueError("unresolved reference " + ref)
        return ref

    for r in M["reqs"].values():
        add(r["id"], "requirement", r["id"], r["title"], area=r["area"], statement=r["statement"],
            conformance=r["conformance"], category=r["category"], derivation=r["derivation"], targets=r["targets"],
            iri=r["iri"], file=r["file"])
    for r in M["reqs"].values():
        for d in r["derivedFrom"]:
            edge(r["id"], display_id(d), "refines")
        for d in r["reference"]:
            edge(r["id"], add_source(d), "satisfies" if d.startswith(UPSTREAM) else "derivedFrom")
        for v in r["verifiedBy"]:
            edge(r["id"], display_id("req:" + v), "verifiedBy")

    order = {s: i + 1 for i, s in enumerate(PIPELINE)}
    for s in M["skills"].values():
        add("skill:" + s["name"], "skill", s["name"], s["description"], package=s["package"],
            order=order.get(s["name"], 100), file=s["file"])
        for rid in s["satisfies"]:
            edge(display_id(rid), "skill:" + s["name"], "realizedBy")
    for t in M["tools"].values():
        add("tool:" + t["id"], "tool", t["id"], t["title"], text=t["description"], file="tools/%s/%s.ts" % (t["id"], t["id"]))
        for s in t["satisfies"]:
            edge("skill:" + s, "tool:" + t["id"], "implementedBy")
    for p in M["processes"].values():
        add("process:" + p["id"], "process", p["name"], p["name"], text=p["doc"], file=p["file"])
        for s in p["skills"]:
            edge("skill:" + s, "process:" + p["id"], "usedIn")
    for ro in M["roles"]:
        add("role:" + ro["id"], "role", ro["title"], ro["description"], actorKinds=ro.get("actorKinds", []))
        for s in ro.get("skills", []):
            edge("skill:" + s, "role:" + ro["id"], "performedBy")
    for key, w in M["wit"].items():
        add(key, "api", w["name"], w["text"] or w["wit"], module=w["module"], wit=w["wit"], apiKind=w["kind"],
            witFile=w["file"])
        for tag in w["tags"]:
            if tag in reqs_by_display:
                edge(tag, key, "constrainsApi")
        if w["name"] != w["module"]:
            edge(key, "wit:" + w["module"], "partOf")
    for b in M["beans"].values():
        add(b["id"], "issue", b["id"], b["title"], text=b["text"], hint=b["hint"], upstream=b["upstream"],
            status=b["status"], file=b["file"])
        for r in b["requirements"]:
            edge(display_id(r), b["id"], "affectedBy")
        for s in b["sources"]:
            edge(b["id"], add_source(s), "concerns")

    known = set(nodes)
    dangling = [e for e in edges if e["s"] not in known or e["t"] not in known]
    if dangling:
        raise ValueError("edges to no node: %s" % dangling[:5])
    return {"nodes": list(nodes.values()), "edges": edges, "areas": [[k, v] for k, v in AREA_TITLES.items()]}


if __name__ == "__main__":
    import sys
    # CI: every edge resolves. Without --with-tools the Tool edges are left to
    # the site build's pre step, which runs with the harness mounted.
    g = build_graph(load(with_tools="--with-tools" in sys.argv))
    skills = {n["id"] for n in g["nodes"] if n["kind"] == "skill"}
    lost = sorted(skills - {e["s"] for e in g["edges"] if e["rel"] in ("usedIn", "performedBy")})
    if lost:
        sys.exit("skills in no process or role: %s" % lost)
    print("%d nodes, %d edges, all resolve" % (len(g["nodes"]), len(g["edges"])))
