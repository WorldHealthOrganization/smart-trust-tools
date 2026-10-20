#!/usr/bin/env python3
"""Build the static viewer site (graph explorer, traceability matrix, API,
wire/payload models, requirements, vocabulary) into site/.

Usage:
    python3 tools/build_site.py [--git-ref REF] [--out site]

The TypeScript API reference (site/api/typedoc/) is built separately by
tools/build_site.sh, which needs Node.js. Requires PyYAML, markdown, rdflib,
cbor2, base45.
"""

import argparse
import collections
import html
import json
import pathlib
import re
import shutil
import subprocess
import sys
import urllib.parse
import zlib

import base45
import cbor2
import markdown
import rdflib

TOOLS = pathlib.Path(__file__).resolve().parent
ROOT = TOOLS.parent
sys.path.insert(0, str(TOOLS))
import build_kg  # noqa: E402

REPO = "litlfred/smart-trust-tools"

NAV = [
    ("index.html", "Overview"),
    ("explorer.html", "Graph explorer"),
    ("trace.html", "Traceability"),
    ("requirements.html", "Requirements"),
    ("api.html", "API (WIT)"),
    ("api/typedoc/index.html", "API reference (TS)"),
    ("kotlin.html", "Kotlin API"),
    ("models.html", "Wire & payload models"),
    ("vocabulary.html", "Vocabulary"),
]

FONTS = ("https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700"
         "&family=JetBrains+Mono:wght@400;600&family=Public+Sans:wght@400;550;650;700&display=swap")

DOC_SHORT = {
    "doc-qr": "QR", "doc-hcert": "HCERT", "doc-did-gdhcn": "DID", "doc-endpoints": "Endpoints",
    "doc-changes": "Changes", "doc-trust-domains": "Domains",
}

FAMILY_OF_KIND = {
    "requirement": "requirement",
    "section": "source", "document": "source", "standard": "source", "directive": "source", "upstream": "source",
    "step": "realization", "api": "realization",
    "issue": "issue",
}


def esc(s):
    return html.escape(str(s), quote=True)


def gh(ref, path):
    return "https://github.com/%s/blob/%s/%s" % (REPO, ref, path)


# ---------------------------------------------------------------------------
# Page shell
# ---------------------------------------------------------------------------

def shell(current, title, body, ref, scripts=(), depth=0, fragment=False, head_extra=""):
    up = "../" * depth
    nav = "".join(
        '<a href="%s%s"%s>%s</a>' % (up, href, ' aria-current="page"' if href == current else "", esc(label))
        for href, label in NAV)
    head = (
        '<title>%s</title>\n'
        '<meta name="claude:viewport-fit-sides" content="cover">\n'
        '<link rel="preconnect" href="https://fonts.googleapis.com">\n'
        '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>\n'
        '<link rel="stylesheet" href="%s">\n'
        '<link rel="stylesheet" href="%sassets/site.css">\n%s'
    ) % (esc(title), FONTS, up, head_extra)
    top = (
        '<header class="topbar"><div class="topbar-inner">'
        '<a class="brand" href="%sindex.html"><span class="brand-mark" aria-hidden="true">%s</span>SMART Trust Tools</a>'
        '<nav class="nav" aria-label="Viewers">%s</nav></div></header>\n'
    ) % (up, "<i></i>" * 9, nav)
    tail = "".join('<script src="%s"></script>\n' % s for s in scripts)
    if fragment:
        return head + top + body + tail
    return ('<!doctype html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n'
            '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n'
            + head + '</head>\n<body>\n' + top + body + tail + '</body>\n</html>\n')


# ---------------------------------------------------------------------------
# Graph data
# ---------------------------------------------------------------------------

def section_label(M, sid):
    s = M["sections"][sid]
    short = DOC_SHORT.get(s["doc"], M["docs"][s["doc"]]["title"])
    m = re.match(r"^(\d+(?:\.\d+)*|[A-Z](?:\.\d+)?)\.?\s", s["heading"])
    if m:
        return "%s §%s" % (short, m.group(1))
    if s["heading"].startswith("Appendix A"):
        return "%s App. A" % short
    return "%s: %s" % (short, s["heading"][:28])


def doc_kind(did):
    if did.startswith("dir-"):
        return "directive"
    if did.startswith("std-"):
        return "standard"
    return "document"


def build_graph(M):
    nodes = collections.OrderedDict()
    edges = []

    def add(nid, kind, label, title, **kw):
        if nid not in nodes:
            nodes[nid] = dict(id=nid, kind=kind, fam=FAMILY_OF_KIND[kind], label=label, title=title, **kw)
        return nodes[nid]

    def edge(s, t, rel):
        edges.append({"s": s, "t": t, "rel": rel})

    def add_source(sid):
        if sid in M["sections"]:
            s = M["sections"][sid]
            add(sid, "section", section_label(M, sid), s["heading"].replace("`", ""),
                doc=s["doc"], urls=s["urls"], status=M["docs"][s["doc"]].get("status"))
            add_source(s["doc"])
            if not any(e["s"] == sid and e["rel"] == "partOf" for e in edges):
                edge(sid, s["doc"], "partOf")
        else:
            d = M["docs"][sid]
            add(sid, doc_kind(sid), DOC_SHORT.get(sid, d["title"][:34]), d["title"],
                urls=d["urls"], status=d.get("status"), note=(d.get("note") or "").strip())

    for r in M["reqs"].values():
        area = r["id"].split("-")[1]
        add(r["id"], "requirement", r["id"], r["title"], area=area,
            statement=" ".join(r["statement"].split()), conformance=r["conformance"],
            category=r["category"], derivation=r["derivation"],
            targets=r.get("targets", ["kotlin", "rust"]))
    for r in M["reqs"].values():
        for d in r["derived_from"]:
            add_source(d["src"])
            e = {"s": r["id"], "t": d["src"], "rel": "derivedFrom"}
            if d.get("quote"):
                e["quote"] = " ".join(d["quote"].split())
            edges.append(e)
        for k in r.get("satisfies", []):
            if k in M["upstmts"]:
                st = M["upstmts"][k]
                add(k, "upstream", k, st["label"], conformance=st["conformance"], parent=st["parent"],
                    urls={"published": "%s/Requirements-%s.html" % (M["src"]["sites"]["published"], st["parent"])})
            else:
                up = M["upreqs"][k]
                add(k, "upstream", k, up["title"],
                    urls={"published": "%s/Requirements-%s.html" % (M["src"]["sites"]["published"], k)})
            edge(r["id"], k, "satisfies")
        for k, rel in (("refines", "refines"), ("verified_by", "verifiedBy")):
            for v in r.get(k, []):
                edge(r["id"], v, rel)
        for v in r.get("steps", []):
            edge(r["id"], v, "implementsStep")
        for v in r.get("api", []):
            edge(r["id"], v, "constrainsApi")
        for v in r.get("issues", []):
            edge(r["id"], v, "affectedBy")
    for s in M["steps"].values():
        add(s["id"], "step", "%d. %s" % (s["order"], s["name"]) if s["order"] <= 9 else s["name"].split(" (")[0],
            s["name"], order=s["order"], output=s["output"])
        add_source(s["src"])
        edge(s["id"], s["src"], "derivedFrom")
    for e in M["elements"].values():
        add(e["id"], "api", e["name"], e["description"], module=M["modules"][e["module"]]["name"],
            wit=e.get("wit"), witFile=e.get("wit_file"), witNote=e.get("wit_note"),
            kotlin=e["kotlin"], rust=e["rust"], apiKind=e["kind"])
    for i in M["issues"].values():
        add(i["id"], "issue", i["id"], i["title"], text=" ".join(i["description"].split()),
            upstream=bool(i.get("upstream")), hint=" ".join((i.get("resolution_hint") or "").split()))
        for s in i.get("sources", []):
            add_source(s)
            edge(i["id"], s, "concerns")
    # Upstream statements belong to their FHIR Requirements resource.
    for n in list(nodes.values()):
        if n["kind"] == "upstream" and n.get("parent"):
            p = M["upreqs"][n["parent"]]
            add(n["parent"], "upstream", n["parent"], p["title"],
                urls={"published": "%s/Requirements-%s.html" % (M["src"]["sites"]["published"], n["parent"])})
            edge(n["id"], n["parent"], "partOf")
    return {"nodes": list(nodes.values()), "edges": edges,
            "areas": [[k, v] for k, v in build_kg.AREA_TITLES.items()]}


# ---------------------------------------------------------------------------
# Highlighting
# ---------------------------------------------------------------------------

STT_TAG = re.compile(r"\[((?:STT-[A-Z0-9]+-\d{3}(?:,\s*)?)+)\]")


def link_tags(text_html):
    def repl(m):
        ids = [x.strip() for x in m.group(1).split(",")]
        return "[" + ", ".join('<a href="explorer.html#%s">%s</a>' % (i, i) for i in ids) + "]"
    return STT_TAG.sub(repl, text_html)


def link_issues(text_html):
    return re.sub(r"\b(OI-\d{2})\b", r'<a href="explorer.html#\1">\1</a>', text_html)


def highlight_wit(src):
    out = []
    for line in src.split("\n"):
        m = re.match(r"^(\s*)(///?.*)$", line)
        if m:
            out.append(m.group(1) + '<span class="tok-comment">%s</span>' % link_issues(link_tags(esc(m.group(2)))))
            continue
        e = esc(line)
        e = re.sub(r"\b(package|interface|world|record|variant|enum|resource|type|use|func|import|export|include|constructor|flags)\b",
                   r'<span class="tok-key">\1</span>', e)
        e = re.sub(r'(<span class="tok-key">(?:interface|world|record|variant|enum|resource|type)</span>\s+)([a-z0-9-]+)',
                   lambda mm: '%s<span class="tok-name" id="wit-%s">%s</span>' % (mm.group(1), mm.group(2), mm.group(2)), e)
        e = re.sub(r"^(\s*)([a-z0-9-]+)(:\s*<span class=\"tok-key\">func</span>)",
                   r'\1<span class="tok-name">\2</span>\3', e)
        out.append(e)
    return "\n".join(out)


def highlight_cddl(src, anchor_prefix):
    out = []
    for line in src.split("\n"):
        code, _, comment = line.partition(";")
        e = esc(code)
        e = re.sub(r"(&quot;(?:[^&]|&(?!quot;))*&quot;)", r'<span class="tok-str">\1</span>', e)
        e = re.sub(r"^([a-z][a-z0-9-]*)(\s*=)",
                   lambda m: '<span class="tok-name" id="%s%s">%s</span>%s' % (anchor_prefix, m.group(1), m.group(1), m.group(2)), e)
        e = re.sub(r"(\.(?:cbor|regexp|size|ne))\b", r'<span class="tok-key">\1</span>', e)
        if comment or line.lstrip().startswith(";"):
            e += '<span class="tok-comment">;%s</span>' % link_issues(link_tags(esc(comment)))
        out.append(e)
    return "\n".join(out)


# ---------------------------------------------------------------------------
# CBOR diagnostic notation (RFC 8949 section 8)
# ---------------------------------------------------------------------------

def diag(v, indent=0):
    pad = "  " * indent
    if isinstance(v, cbor2.CBORTag):
        return "%d(%s)" % (v.tag, diag(v.value, indent))
    if isinstance(v, bytes):
        return "h'%s'" % v.hex()
    if isinstance(v, str):
        return json.dumps(v, ensure_ascii=False)
    if isinstance(v, bool):
        return "true" if v else "false"
    if v is None:
        return "null"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, list):
        if not v:
            return "[]"
        return "[\n" + ",\n".join(pad + "  " + diag(x, indent + 1) for x in v) + "\n" + pad + "]"
    if isinstance(v, dict):
        if not v:
            return "{}"
        return "{\n" + ",\n".join(pad + "  " + diag(k, indent + 1) + ": " + diag(x, indent + 1)
                                   for k, x in v.items()) + "\n" + pad + "}"
    return repr(v)


# ---------------------------------------------------------------------------
# Pages
# ---------------------------------------------------------------------------

def requirements_page(ref):
    md_text = (ROOT / "requirements" / "generated" / "REQUIREMENTS.md").read_text()
    md_text = re.sub(r"```mermaid\n(.*?)```", lambda m: '<pre class="mermaid">%s</pre>' % esc(m.group(1)),
                     md_text, flags=re.S)
    body = markdown.markdown(md_text, extensions=["tables", "fenced_code", "md_in_html"])

    def fix(m):
        href = m.group(1)
        if href.startswith(("http", "#", "mailto:")):
            return m.group(0)
        path = (pathlib.PurePosixPath("requirements/generated") / href).as_posix()
        parts = []
        for p in path.split("/"):
            if p == "..":
                parts.pop()
            elif p != ".":
                parts.append(p)
        return 'href="%s"' % gh(ref, "/".join(parts))
    body = re.sub(r'href="([^"]+)"', fix, body)
    body = re.sub(r'(<a id="(stt-[a-z0-9-]+)"></a>)',
                  lambda m: m.group(1) + '<span class="small"><a href="explorer.html#%s">open in explorer</a></span>'
                  % m.group(2).upper(), body)
    return ('<main class="page"><article class="prose req-doc">%s</article></main>'
            '<script src="assets/vendor/mermaid.min.js"></script>'
            '<script>if (window.mermaid) { mermaid.initialize({ startOnLoad: true, theme: '
            'matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "neutral" }); }</script>') % body


def api_page(ref, M):
    files = sorted((ROOT / "api" / "wit").glob("*.wit"))
    order = ["types.wit", "codec.wit", "cose.wit", "payload.wit", "qr.wit", "trust.wit", "pipeline.wit", "host.wit", "worlds.wit"]
    files.sort(key=lambda p: order.index(p.name) if p.name in order else 99)
    index, blocks = [], []
    for p in files:
        src = p.read_text()
        names = re.findall(r"^(?:interface|world)\s+([a-z0-9-]+)", src, flags=re.M)
        index.append('<li><a href="#file-%s"><code>%s</code></a> <span class="muted small">%s</span></li>'
                     % (p.stem, p.name, ", ".join(names)))
        blocks.append(
            '<section class="section" id="file-%s"><div class="section-head"><h2><code>%s</code></h2>'
            '<a class="small" href="%s">source</a></div><pre class="wit"><code>%s</code></pre></section>'
            % (p.stem, p.name, gh(ref, "api/wit/" + p.name), highlight_wit(src)))
    worlds = (ROOT / "api" / "wit" / "worlds.wit").read_text()
    rows = []
    for name, body in re.findall(r"world\s+([a-z0-9-]+)\s*\{(.*?)\}", worlds, flags=re.S):
        imps = re.findall(r"import\s+([a-z0-9-]+)", body)
        exps = re.findall(r"export\s+([a-z0-9-]+)", body)
        inc = re.findall(r"include\s+([a-z0-9-]+)", body)
        rows.append("<tr><td><code>%s</code></td><td>%s</td><td>%s</td></tr>" % (
            name, " ".join('<span class="chip">%s</span>' % x for x in imps) or "—",
            " ".join('<span class="chip">%s</span>' % x for x in exps)
            or "includes " + ", ".join("<code>%s</code>" % x for x in inc)))
    body = """
<main class="page">
  <section class="section prose">
    <span class="eyebrow">Library API contract · WIT package who:smart-trust@0.1.0</span>
    <h1>API (WIT)</h1>
    <p>The API is defined first, in WIT, the WebAssembly Component Model interface language. Rust exports it from its
    WebAssembly component; Kotlin follows it through fixed mapping rules. Requirement tags such as
    <code>[STT-COS-002]</code> link into the graph explorer. For the same API as JavaScript callers see it, open the
    <a href="api/typedoc/index.html">TypeScript reference</a>, generated by jco and TypeDoc.</p>
  </section>
  <section class="section">
    <h2>Worlds (deployable bundles)</h2>
    <div class="table-wrap"><table><thead><tr><th>World</th><th>Imports (host provides)</th><th>Exports</th></tr></thead>
    <tbody>%s</tbody></table></div>
  </section>
  <section class="section"><h2>Files</h2><ul class="small">%s</ul></section>
  %s
</main>""" % ("".join(rows), "".join(index), "".join(blocks))
    return body


KT_KEYWORDS = ("package|import|interface|sealed|data|class|object|enum|fun|val|var|typealias|"
               "override|inline|when|is|as|return|null")


def highlight_kotlin(src):
    out = []
    for line in src.split("\n"):
        stripped = line.lstrip()
        if stripped.startswith(("//", "/**", "*", "*/")):
            out.append('<span class="tok-comment">%s</span>' % link_tags(esc(line)))
            continue
        e = esc(line)
        e = re.sub(r"\b(%s)\b" % KT_KEYWORDS, r'<span class="tok-key">\1</span>', e)
        e = re.sub(r'(<span class="tok-key">(?:interface|class|object|typealias)</span>\s+)([A-Z][A-Za-z0-9]*)',
                   lambda m: '%s<span class="tok-name" id="kt-%s">%s</span>' % (m.group(1), m.group(2), m.group(2)), e)
        out.append(e)
    return "\n".join(out)


def kotlin_page(ref):
    base = ROOT / "kotlin" / "hcert-api" / "src" / "commonMain" / "kotlin"
    files = sorted(base.rglob("*.kt"), key=lambda p: (p.parent != base / "who" / "smart" / "trust", p.as_posix()))
    index, blocks = [], []
    for p in files:
        rel = p.relative_to(ROOT).as_posix()
        short = p.relative_to(base / "who" / "smart" / "trust").as_posix()
        anchor = "kt-file-" + short.replace("/", "-").replace(".kt", "")
        src = p.read_text()
        decls = re.findall(r"^(?:sealed |data |enum |fun )?(?:interface|class|object|typealias)\s+([A-Z]\w*)", src, flags=re.M)
        index.append('<li><a href="#%s"><code>%s</code></a> <span class="muted small">%s</span></li>'
                     % (anchor, short, ", ".join(decls[:8]) + (" …" if len(decls) > 8 else "")))
        blocks.append('<section class="section" id="%s"><div class="section-head"><h2><code>%s</code></h2>'
                      '<a class="small" href="%s">source</a></div><pre><code>%s</code></pre></section>'
                      % (anchor, short, gh(ref, rel), highlight_kotlin(src)))
    return """
<main class="page">
  <section class="section prose">
    <span class="eyebrow">Kotlin Multiplatform · generated from the WIT contract</span>
    <h1>Kotlin API skeleton</h1>
    <p>Declarations only, with no behaviour. <code>tools/wit2kotlin.py</code> generates them from <a href="api.html">the WIT contract</a>
    using the mapping rules: records become data classes, variants become sealed interfaces, resources become
    <code>AutoCloseable</code> interfaces, and <code>result&lt;T, E&gt;</code> becomes <code>WitResult&lt;T, E&gt;</code>. Each exported
    WIT interface becomes a Kotlin interface that the library implements, for example <code>Codec</code> or <code>Verifier</code>.
    Imported interfaces (<code>Signer</code>, <code>Terminology</code>) are implemented by the host application. Each WIT world becomes
    a bundle interface, plus a factory that takes the world's imports.</p>
    <p>The module targets the JVM (Java 11+), which Android uses as-is, and Node.js through Kotlin/JS IR with generated TypeScript
    declarations. Regenerate with <code>python3 tools/wit2kotlin.py</code>; CI fails if the skeleton is out of date with the WIT.</p>
  </section>
  <section class="section"><h2>Files</h2><ul class="small">%s</ul></section>
  %s
</main>""" % ("".join(index), "".join(blocks))


def models_page(ref, M):
    env = (ROOT / "api" / "cddl" / "hcert.cddl").read_text()
    gen = (ROOT / "models" / "generated" / "trust-phw.cddl").read_text()
    ctx = json.loads((ROOT / "models" / "generated" / "trust-phw.context.jsonld").read_text())
    vec = json.loads((ROOT / "vectors" / "smart-trust-pr129" / "dvc-qr-example-XXA.json").read_text())
    cert = vec["certificates"]["hepb1"]
    cose = zlib.decompress(base45.b45decode(cert["hc1"][4:]))
    tagged = cbor2.loads(cose)
    prot, unprot, payload_b, sig = tagged.value
    claims = cbor2.loads(payload_b)
    layered = cbor2.CBORTag(18, ["<<%s>>" % diag(cbor2.loads(prot)), unprot, "<<CWT claims, below>>", sig])
    cose_diag = diag(layered).replace('"<<', "<< ").replace('>>"', " >>").replace("\\n", "\n")
    payload = claims[-260][-6]
    doc = dict(payload)
    doc = {"@context": ctx["@context"], "@type": "DVCMin", **doc}
    g = rdflib.Graph().parse(data=json.dumps(doc), format="json-ld")
    nquads = "\n".join(sorted(g.serialize(format="nt").strip().splitlines()))
    playground = "https://json-ld.org/playground/#startTab=tab-nquads&json-ld=" + urllib.parse.quote(json.dumps(doc))
    cbor_me = "https://cbor.me/?bytes=" + cose.hex()

    shapes = rdflib.Graph().parse(ROOT / "models" / "generated" / "trust-phw.shacl.ttl", format="turtle")
    SH = rdflib.Namespace("http://www.w3.org/ns/shacl#")
    LMX = rdflib.Namespace("https://github.com/litlfred/smart-trust-tools/models/lmx#")
    SKOS = rdflib.Namespace("http://www.w3.org/2004/02/skos/core#")
    RDFS = rdflib.RDFS
    shape_html = []
    for shape in sorted(shapes.subjects(rdflib.RDF.type, SH.NodeShape)):
        label = shapes.value(shape, RDFS.label)
        target = shapes.value(shape, SH.targetClass)
        abstract = str(shapes.value(shape, LMX.abstract)) == "true"
        rows = []
        props = list(shapes.objects(shape, SH.property))
        props.sort(key=lambda p: str(shapes.value(p, SH.name)))
        # keep logical-model order where possible
        order = list(payload.keys()) + list(payload["vx"].keys())
        props.sort(key=lambda p: order.index(str(shapes.value(p, SH.name))) if str(shapes.value(p, SH.name)) in order else 99)
        for p in props:
            name = shapes.value(p, SH.name)
            path = shapes.value(p, SH.path)
            mn = shapes.value(p, SH.minCount)
            mx = shapes.value(p, SH.maxCount)
            card = "%s..%s" % (mn or 0, mx or "*")
            node = shapes.value(p, SH.node)
            ftype = shapes.value(p, LMX.fhirType)
            ty = ('<a href="#shape-%s">%s</a>' % (str(node).rsplit("/", 1)[1], str(node).rsplit("/", 1)[1])
                  if node else "<code>%s</code>" % ftype)
            vs = shapes.value(p, LMX.valueSet)
            strength = shapes.value(p, LMX.bindingStrength)
            binding = ('%s <a href="%s">%s</a>' % (esc(strength), esc(vs), esc(str(vs).rsplit("/", 1)[-1]))
                       if vs else "")
            codes = sorted(str(c) for c in shapes.objects(path, SKOS.closeMatch))
            code_html = " ".join('<a class="chip" href="%s">%s</a>' % (esc(c), esc(c.rsplit("/", 1)[-1].split("#")[-1]))
                                 for c in codes if c.startswith("http"))
            rows.append("<tr><td><code>%s</code></td><td class=\"num\">%s</td><td>%s</td><td class=\"small\">%s</td>"
                        "<td class=\"small\">%s</td><td>%s</td></tr>" % (
                            esc(name), card, ty, esc(shapes.value(p, SH.description) or ""), binding, code_html))
        msg = shapes.value(shape, SH.message)
        sname = str(shape).rsplit("/", 1)[1]
        shape_html.append(
            '<div class="section" id="shape-%s"><div class="section-head"><h3>%s</h3>'
            '<span class="small muted">%s · <a href="%s">logical model</a></span></div>'
            '<div class="table-wrap"><table><thead><tr><th>Element</th><th>Card.</th><th>Type</th><th>Meaning</th>'
            '<th>Binding</th><th>Concept codes</th></tr></thead><tbody>%s</tbody></table></div>%s</div>'
            % (sname, esc(label), "abstract" if abstract else "concrete",
               esc(str(target).replace("/StructureDefinition/", "/StructureDefinition-") + ".html"),
               "".join(rows),
               '<p class="small muted">Invariant: %s</p>' % esc(msg) if msg else ""))

    return """
<main class="page">
  <section class="section prose">
    <span class="eyebrow">Wire format and payload models</span>
    <h1>Wire &amp; payload models</h1>
    <p>The CBOR wire format is specified in CDDL (IETF RFC 8610). Payload models are derived mechanically from the
    WHO SMART FHIR logical models into a JSON-LD context, SHACL shapes and CDDL rules. Every example below is the
    Hepatitis B birth-dose certificate from smart-trust PR 129, which CI validates against all of these.</p>
    <div class="chips">
      <a class="chip" href="#cddl">CDDL</a><a class="chip" href="#example">CBOR example</a>
      <a class="chip" href="#jsonld">JSON-LD</a><a class="chip" href="#shacl">SHACL shapes</a>
    </div>
  </section>

  <section class="section" id="cddl">
    <div class="section-head"><h2>CDDL: HCERT envelope</h2><a class="small" href="%(env_src)s">api/cddl/hcert.cddl</a></div>
    <pre><code>%(env)s</code></pre>
    <div class="section-head"><h2>CDDL: payload rules, generated from the logical models</h2><a class="small" href="%(gen_src)s">models/generated/trust-phw.cddl</a></div>
    <pre><code>%(gen)s</code></pre>
  </section>

  <section class="section" id="example">
    <div class="section-head"><h2>CBOR example in diagnostic notation</h2>
      <a class="chip" href="%(cbor_me)s">Open the COSE bytes in cbor.me ↗</a></div>
    <p class="small muted">HC1 string (%(hc1_len)d characters), then the COSE_Sign1 with each embedded layer decoded
      (&lt;&lt; &gt;&gt; marks a byte string holding CBOR).</p>
    <pre><code>%(hc1)s</code></pre>
    <pre><code>%(cose)s</code></pre>
    <pre><code>%(claims)s</code></pre>
  </section>

  <section class="section" id="jsonld">
    <div class="section-head"><h2>JSON-LD: the payload as linked data</h2>
      <a class="chip" href="%(playground)s">Open in JSON-LD Playground ↗</a></div>
    <p class="small muted">Adding the generated context and a type to the payload's JSON view turns it into RDF.
      Each short element name expands to <code>&lt;StructureDefinition URL&gt;#&lt;element&gt;</code>.</p>
    <pre><code>%(doc)s</code></pre>
    <h3>Resulting triples (N-Triples)</h3>
    <pre><code>%(nquads)s</code></pre>
    <details><summary class="small">Generated context (<a href="%(ctx_src)s">trust-phw.context.jsonld</a>)</summary>
      <pre><code>%(ctx)s</code></pre></details>
  </section>

  <section class="section" id="shacl">
    <div class="section-head"><h2>SHACL shapes</h2><a class="small" href="%(shacl_src)s">trust-phw.shacl.ttl</a></div>
    <p class="small muted">Cardinality and FHIR primitive patterns are enforced; bindings are recorded (the PreQual
      value set has no published expansion); concept codes come from each element's <code>ElementDefinition.code</code>.
      To render or edit these shapes in a dedicated tool, load the TTL into
      <a href="https://shacl-play.sparna.fr/play/">SHACL Play!</a>.</p>
    %(shapes)s
  </section>
</main>""" % {
        "env": highlight_cddl(env, "cddl-"), "gen": highlight_cddl(gen, "cddl-"),
        "env_src": gh(ref, "api/cddl/hcert.cddl"), "gen_src": gh(ref, "models/generated/trust-phw.cddl"),
        "ctx_src": gh(ref, "models/generated/trust-phw.context.jsonld"),
        "shacl_src": gh(ref, "models/generated/trust-phw.shacl.ttl"),
        "cbor_me": esc(cbor_me), "playground": esc(playground),
        "hc1": esc(cert["hc1"]), "hc1_len": len(cert["hc1"]),
        "cose": esc(cose_diag), "claims": esc("/ CWT claims /\n" + diag(claims)),
        "doc": esc(json.dumps({"@context": "trust-phw.context.jsonld", "@type": "DVCMin", **payload},
                              indent=2, ensure_ascii=False)),
        "nquads": esc(nquads), "ctx": esc(json.dumps(ctx, indent=2)), "shapes": "".join(shape_html),
    }


def vocabulary_page(ref, M, graph):
    counts = collections.Counter(n["kind"] for n in graph["nodes"])
    cls_rows, prop_rows = [], []
    for local, kind, label, comment in build_kg.VOCAB_TERMS:
        row = "<tr><td><code>stt:%s</code></td><td>%s</td><td class=\"small muted\">%s</td></tr>" % (
            esc(local), esc(label), esc(comment))
        (cls_rows if kind == "class" else prop_rows).append(row)
    count_rows = "".join("<tr><td>%s</td><td class=\"num\">%d</td></tr>" % (esc(k), v)
                         for k, v in sorted(counts.items(), key=lambda kv: -kv[1]))
    return """
<main class="page">
  <section class="section prose">
    <span class="eyebrow">Requirements knowledge graph</span>
    <h1>Vocabulary</h1>
    <p>The graph uses a small vocabulary, <code>stt:</code> = <code>%(vocab)s</code>, alongside Dublin Core
    (<code>dcterms:</code>) and PROV (<code>prov:</code>). Instances live under <code>%(id)s</code>.
    The graph is published as <a href="%(ttl)s">Turtle</a> and <a href="%(jsonld)s">JSON-LD</a>; both contain the same triples.</p>
  </section>
  <section class="section"><h2>Classes</h2>
    <div class="table-wrap"><table><thead><tr><th>Term</th><th>Label</th><th>Meaning</th></tr></thead><tbody>%(cls)s</tbody></table></div></section>
  <section class="section"><h2>Properties</h2>
    <div class="table-wrap"><table><thead><tr><th>Term</th><th>Label</th><th>Meaning</th></tr></thead><tbody>%(props)s</tbody></table></div></section>
  <section class="section"><h2>Nodes in the explorer, by kind</h2>
    <div class="table-wrap" style="max-width:420px"><table><thead><tr><th>Kind</th><th>Count</th></tr></thead><tbody>%(counts)s</tbody></table></div></section>
  <section class="section"><h2>Example query</h2>
    <pre><code>PREFIX stt: &lt;%(vocab)s&gt;
PREFIX dcterms: &lt;http://purl.org/dc/terms/&gt;
# Which library requirements satisfy each smart-trust requirement statement?
SELECT ?upstream ?req WHERE {
  ?r stt:satisfies ?u ; dcterms:identifier ?req .
  ?u dcterms:identifier ?upstream .
} ORDER BY ?upstream ?req</code></pre></section>
</main>""" % {"vocab": build_kg.VOCAB, "id": build_kg.ID, "cls": "".join(cls_rows), "props": "".join(prop_rows),
               "counts": count_rows, "ttl": gh(ref, "requirements/generated/kg.ttl"),
               "jsonld": gh(ref, "requirements/generated/kg.jsonld")}


def index_page(ref, M, graph):
    reqs = list(M["reqs"].values())
    conf = collections.Counter(r["conformance"] for r in reqs)
    issues = list(M["issues"].values())
    upstream_issues = sum(1 for i in issues if i.get("upstream"))
    steps = [s for s in M["steps"].values() if s["order"] <= 9]
    step_items = "".join(
        '<li><a href="explorer.html#%s"><span class="step-n num">%d</span><span>%s</span></a></li>'
        % (s["id"], s["order"], esc(s["name"])) for s in steps)
    covered = sum(1 for k in M["upstmts"] if any(k in r.get("satisfies", []) for r in reqs))
    pr = M["src"]["refs"]["pr129"]
    issue_rows = "".join(
        '<li><a href="explorer.html#%s"><span class="issue-mark">%s</span></a> %s%s</li>'
        % (i["id"], i["id"], esc(i["title"]), ' <span class="chip">fix upstream</span>' if i.get("upstream") else "")
        for i in issues)
    return """
<main class="page landing">
  <section class="hero">
    <div class="hero-text">
      <span class="eyebrow">WHO SMART Trust · HCERT libraries · Kotlin first, Rust second</span>
      <h1>From the QR Code Generation spec to a library you can build</h1>
      <p class="lede">Requirements, API contract, wire format and payload models for the Kotlin (JVM, Android, Node)
      and Rust (WebAssembly) HCERT libraries. Everything here is derived from, and links back to, the
      <a href="https://github.com/WorldHealthOrganization/smart-trust/pull/129">smart-trust QR Code Generation page (PR 129)</a>
      and the deployed WHO SMART Trust specifications.</p>
    </div>
    <dl class="facts">
      <div><dt>Requirements</dt><dd class="num">%(nreq)d</dd><span class="small muted">%(conf)s</span></div>
      <div><dt>smart-trust statements covered</dt><dd class="num">%(cov)d/%(nup)d</dd><span class="small muted">FHIR Requirements</span></div>
      <div><dt>Open issues</dt><dd class="num">%(niss)d</dd><span class="small muted">%(upiss)d to fix upstream</span></div>
    </dl>
  </section>

  <section class="section">
    <div class="section-head"><h2>The HCERT pipeline</h2><span class="small muted">Each step opens its requirements in the explorer</span></div>
    <ol class="pipeline">%(steps)s</ol>
  </section>

  <section class="section">
    <h2>Viewers</h2>
    <div class="viewer-grid">
      <a class="viewer" href="explorer.html"><span class="eyebrow">Knowledge graph</span><h3>Graph explorer</h3>
        <p>Every requirement with its sources, smart-trust parents, pipeline steps, API elements and open issues.
        Filter, search and focus on a neighbourhood.</p>
        <span class="legend"><i style="background:var(--fam-requirement)"></i>requirement <i style="background:var(--fam-source)"></i>source <i style="background:var(--fam-realization)"></i>implementation <i style="background:var(--status-warning)"></i>issue</span></a>
      <a class="viewer" href="trace.html"><span class="eyebrow">Review</span><h3>Traceability matrix</h3>
        <p>One row per requirement: what it satisfies upstream, which step and WIT item it shapes, how it is tested,
        and what is still open. Filter by area, conformance or gaps.</p></a>
      <a class="viewer" href="requirements.html"><span class="eyebrow">Document</span><h3>Requirements</h3>
        <p>The full requirement set as a document, with verbatim source quotes and links that work before and after
        PR 129 is merged.</p></a>
      <a class="viewer" href="api.html"><span class="eyebrow">Library API · WIT</span><h3>API contract</h3>
        <p>The WIT package that both libraries implement, with requirement tags linked and the deployable worlds
        summarised.</p></a>
      <a class="viewer" href="api/typedoc/index.html"><span class="eyebrow">Library API · TypeScript</span><h3>API reference</h3>
        <p>The same API as Node and browser callers see it, generated by jco and TypeDoc.</p></a>
      <a class="viewer" href="kotlin.html"><span class="eyebrow">Library API · Kotlin</span><h3>Kotlin API skeleton</h3>
        <p>The Kotlin Multiplatform interfaces and types generated from the WIT, ready to implement for JVM, Android and Node.</p></a>
      <a class="viewer" href="models.html"><span class="eyebrow">Wire format · payload models</span><h3>CDDL, JSON-LD, SHACL</h3>
        <p>The CBOR envelope, payload rules derived from the FHIR logical models, and the PR 129 certificate decoded
        layer by layer.</p></a>
      <a class="viewer" href="vocabulary.html"><span class="eyebrow">Knowledge graph</span><h3>Vocabulary</h3>
        <p>Classes and properties of the graph, node counts, and an example SPARQL query.</p></a>
    </div>
  </section>

  <section class="section two-col">
    <div class="section">
      <h2>External tools, prefilled</h2>
      <ul class="tool-list">
        <li><a href="models.html#jsonld">JSON-LD Playground</a> <span class="muted small">the DVC payload expanded to triples</span></li>
        <li><a href="models.html#example">cbor.me</a> <span class="muted small">the COSE_Sign1 bytes of the example certificate</span></li>
        <li><a href="https://shacl-play.sparna.fr/play/">SHACL Play!</a> <span class="muted small">load <a href="%(shacl)s">trust-phw.shacl.ttl</a></span></li>
        <li><a href="https://smart.who.int/trust/">WHO SMART Trust IG</a> <span class="muted small">published specification</span></li>
        <li><a href="https://worldhealthorganization.github.io/smart-trust/branches/%(preview)s/qr_code_generation.html">QR Code Generation (PR preview)</a></li>
      </ul>
      <h2>Provenance</h2>
      <ul class="tool-list small">
        <li>smart-trust main <code>%(main)s</code>, PR 129 head <code>%(prsha)s</code></li>
        <li>smart-trust-phw <code>%(phw)s</code> (DVCMin logical models)</li>
        <li>Built from <a href="https://github.com/%(repo)s/tree/%(ref)s">%(repo)s@%(ref)s</a></li>
      </ul>
    </div>
    <div class="section">
      <h2>Open issues</h2>
      <ul class="issue-list small">%(issues)s</ul>
    </div>
  </section>
</main>""" % {
        "nreq": len(reqs), "conf": ", ".join("%d %s" % (conf[c], c) for c in ("SHALL", "SHOULD", "MAY", "SHALL NOT") if conf[c]),
        "cov": covered, "nup": len(M["upstmts"]), "niss": len(issues), "upiss": upstream_issues,
        "steps": step_items, "issues": issue_rows, "shacl": gh(ref, "models/generated/trust-phw.shacl.ttl"),
        "preview": pr["preview_dir"], "main": M["src"]["refs"]["main"]["sha"][:12], "prsha": pr["sha"][:12],
        "phw": M["src"]["refs"]["phw"]["sha"][:12], "repo": REPO, "ref": esc(ref),
    }


LANDING_CSS = """
<style>
.landing .hero { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr); gap: 28px; align-items: end; padding-block: 8px 4px; }
.landing .hero-text { display: grid; gap: 12px; }
.landing .lede { font-size: var(--step-1); color: var(--fg-2); max-width: 62ch; margin: 0; }
.facts { display: grid; gap: 10px; margin: 0; }
.facts div { display: grid; grid-template-columns: 1fr auto; align-items: baseline; gap: 0 12px; border-top: 1px solid var(--line); padding-top: 8px; }
.facts dt { color: var(--fg-2); font-size: var(--step--1); }
.facts dd { margin: 0; font-family: var(--font-display); font-size: var(--step-2); font-weight: 700; grid-row: span 2; }
.pipeline { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 112px), 1fr)); gap: 6px; }
.pipeline a { display: grid; grid-template-columns: auto 1fr; gap: 8px; align-items: center; text-decoration: none; color: var(--fg); background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); padding: 8px 10px; font-size: var(--step--1); height: 100%; }
.pipeline a:hover { border-color: var(--fam-realization); }
.step-n { font-family: var(--font-mono); font-weight: 600; color: var(--accent); }
.viewer-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr)); gap: 12px; }
.viewer { display: grid; gap: 6px; align-content: start; text-decoration: none; color: var(--fg); background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); padding: 14px 16px; }
.viewer:hover { border-color: var(--accent); }
.viewer p { margin: 0; color: var(--fg-2); font-size: var(--step--1); }
.legend { display: flex; flex-wrap: wrap; gap: 4px 10px; align-items: center; font-size: 0.75rem; color: var(--fg-2); }
.legend i { width: 9px; height: 9px; border-radius: 2px; display: inline-block; margin-right: -6px; }
.two-col { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 28px; }
.tool-list, .issue-list { margin: 0; padding-left: 1.1em; display: grid; gap: 5px; }
.issue-list { list-style: none; padding-left: 0; }
@media (max-width: 760px) { .landing .hero, .two-col { grid-template-columns: 1fr; } }
</style>
"""


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--git-ref", default=None)
    ap.add_argument("--out", default=str(ROOT / "site"))
    args = ap.parse_args()
    ref = args.git_ref or subprocess.run(["git", "-C", str(ROOT), "rev-parse", "--abbrev-ref", "HEAD"],
                                         capture_output=True, text=True).stdout.strip() or "main"
    out = pathlib.Path(args.out)

    M = build_kg.build_model()
    graph = build_graph(M)

    if out.exists():
        for child in out.iterdir():
            if child.name != "api":
                shutil.rmtree(child) if child.is_dir() else child.unlink()
    (out / "data").mkdir(parents=True, exist_ok=True)
    shutil.copytree(TOOLS / "site" / "assets", out / "assets", dirs_exist_ok=True)
    (out / ".nojekyll").write_text("")  # serve TypeDoc's files as-is
    (out / "data" / "graph.json").write_text(json.dumps(graph, ensure_ascii=False, separators=(",", ":")))

    pages = {
        "explorer.html": ("Graph Explorer", (TOOLS / "site" / "explorer.body.html").read_text(),
                          ["assets/vendor/cytoscape.min.js", "assets/common.js", "assets/explorer.js"]),
        "trace.html": ("Traceability Matrix", (TOOLS / "site" / "trace.body.html").read_text(),
                       ["assets/common.js", "assets/trace.js"]),
        "requirements.html": ("HCERT Library Requirements", requirements_page(ref), []),
        "api.html": ("HCERT API Contract", api_page(ref, M), []),
        "models.html": ("HCERT Wire and Payload Models", models_page(ref, M), []),
        "vocabulary.html": ("Requirements Graph Vocabulary", vocabulary_page(ref, M, graph), []),
        "kotlin.html": ("Kotlin API Skeleton", kotlin_page(ref), []),
    }
    for name, (title, body, scripts) in pages.items():
        (out / name).write_text(shell(name, title, body, ref, scripts))
    landing = index_page(ref, M, graph)
    (out / "index.html").write_text(shell("index.html", "SMART Trust Tools", landing, ref, head_extra=LANDING_CSS))
    print("wrote site to %s (%d graph nodes, %d edges)" % (out, len(graph["nodes"]), len(graph["edges"])))
    return 0


if __name__ == "__main__":
    sys.exit(main())
