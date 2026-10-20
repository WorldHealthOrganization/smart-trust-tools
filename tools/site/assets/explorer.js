/* Graph explorer: Cytoscape view of the requirements knowledge graph. */
(function () {
  "use strict";
  var S = window.STT, esc = S.esc;

  var FAMILIES = [
    { id: "requirement", label: "Requirements", color: "--fam-requirement" },
    { id: "source", label: "Sources & smart-trust", color: "--fam-source" },
    { id: "realization", label: "Steps & API", color: "--fam-realization" },
    { id: "issue", label: "Open issues", color: "--status-warning" }
  ];
  var SHAPE = {
    requirement: "round-rectangle", section: "rectangle", document: "cut-rectangle", standard: "cut-rectangle",
    directive: "star", upstream: "diamond", step: "hexagon", api: "ellipse", issue: "triangle"
  };
  var SHAPE_NOTE = [
    ["round-rectangle", "requirement"], ["rectangle", "source section"], ["cut-rectangle", "document or standard"],
    ["star", "stakeholder directive"], ["diamond", "smart-trust FHIR requirement"], ["hexagon", "pipeline step"],
    ["ellipse", "API element"], ["triangle", "open issue"]
  ];
  var COLUMN = { directive: 0, standard: 0, document: 0, section: 1, upstream: 1, requirement: 2, step: 3, api: 3, issue: 4 };
  var REL_LABEL = {
    derivedFrom: ["Derived from", "Source for"], satisfies: ["Satisfies (smart-trust)", "Satisfied by"],
    refines: ["Refines", "Refined by"], verifiedBy: ["Verified by", "Verifies"],
    implementsStep: ["Pipeline steps", "Requirements"], constrainsApi: ["API elements", "Constrained by"],
    affectedBy: ["Open issues", "Affects"], concerns: ["Concerns", "Raised by"], partOf: ["Part of", "Contains"]
  };

  var G, cy, layoutMode = "layered", hidden = {}, areaFilter = "";

  function shapeSvg(shape, color) {
    var p = {
      "round-rectangle": '<rect x="1" y="3" width="12" height="8" rx="2.5"/>',
      "rectangle": '<rect x="1" y="3" width="12" height="8"/>',
      "cut-rectangle": '<path d="M3 3h8l2 2v4l-2 2H3l-2-2V5z"/>',
      "star": '<path d="M7 1l1.8 3.9 4.2.4-3.2 2.8 1 4.1L7 10l-3.8 2.2 1-4.1L1 5.3l4.2-.4z"/>',
      "diamond": '<path d="M7 1l6 6-6 6-6-6z"/>',
      "hexagon": '<path d="M4 2h6l3 5-3 5H4L1 7z"/>',
      "ellipse": '<ellipse cx="7" cy="7" rx="6" ry="4.5"/>',
      "triangle": '<path d="M7 1.5l6 11H1z"/>'
    }[shape];
    return '<svg class="glyph" viewBox="0 0 14 14" aria-hidden="true" fill="' + color + '">' + p + "</svg>";
  }

  function colors() {
    return {
      requirement: S.cssVar("--fam-requirement"), source: S.cssVar("--fam-source"),
      realization: S.cssVar("--fam-realization"), issue: S.cssVar("--status-warning"),
      fg: S.cssVar("--fg"), fg2: S.cssVar("--fg-2"), line: S.cssVar("--line"), accent: S.cssVar("--accent"),
      surface: S.cssVar("--surface"), bg: S.cssVar("--bg")
    };
  }

  function style() {
    var c = colors();
    return [
      { selector: "node", style: {
        "background-color": function (n) { return c[n.data("fam")]; },
        "shape": function (n) { return SHAPE[n.data("kind")]; },
        "width": function (n) { return n.data("kind") === "requirement" ? 26 : 18; },
        "height": function (n) { return n.data("kind") === "requirement" ? 16 : 18; },
        "border-width": 1.5, "border-color": c.bg,
        "label": "data(label)", "font-family": "Public Sans, system-ui, sans-serif", "font-size": 9,
        "color": c.fg, "text-valign": "bottom", "text-margin-y": 3, "min-zoomed-font-size": 7,
        "text-background-color": c.bg, "text-background-opacity": 0.75, "text-background-padding": 1
      } },
      { selector: "edge", style: {
        "width": 1, "line-color": c.line, "curve-style": "haystack", "haystack-radius": 0, "opacity": 0.9
      } },
      { selector: ".faded", style: { "opacity": 0.12, "text-opacity": 0 } },
      { selector: "edge.hl", style: { "line-color": c.accent, "width": 2, "opacity": 1, "z-index": 9 } },
      { selector: "node.hl", style: { "border-color": c.fg, "border-width": 2, "z-index": 10, "font-weight": 600 } },
      { selector: "node:selected", style: { "border-color": c.accent, "border-width": 3.5 } }
    ];
  }

  function elements() {
    var els = [];
    G.nodes.forEach(function (n) {
      els.push({ group: "nodes", data: { id: n.id, label: n.label, fam: n.fam, kind: n.kind, area: n.area || "" } });
    });
    G.edges.forEach(function (e, i) {
      if (G.byId[e.s] && G.byId[e.t]) els.push({ group: "edges", data: { id: "e" + i, source: e.s, target: e.t, rel: e.rel } });
    });
    return els;
  }

  function areaRank(a) {
    for (var i = 0; i < G.areas.length; i++) if (G.areas[i][0] === a) return i;
    return 99;
  }

  function layeredPositions(nodes) {
    var cols = [[], [], [], [], []];
    nodes.forEach(function (n) { cols[COLUMN[n.data("kind")]].push(n); });
    var key = function (n) {
      var d = G.byId[n.id()];
      if (d.kind === "requirement") return [areaRank(d.area), d.id];
      if (d.kind === "section") return [d.doc, d.label];
      if (d.kind === "step") return [0, ("0" + d.order).slice(-2)];
      if (d.kind === "api") return [1, d.module + d.label];
      return [d.kind, d.label];
    };
    var tallest = Math.max.apply(null, cols.map(function (c) { return c.length; }));
    var height = Math.max(400, tallest * 26);
    var pos = {};
    cols.forEach(function (col, ci) {
      col.sort(function (a, b) {
        var ka = key(a), kb = key(b);
        return ka[0] < kb[0] ? -1 : ka[0] > kb[0] ? 1 : ka[1] < kb[1] ? -1 : ka[1] > kb[1] ? 1 : 0;
      });
      var gap = col.length > 1 ? height / (col.length - 1) : 0;
      col.forEach(function (n, i) { pos[n.id()] = { x: ci * 360, y: col.length > 1 ? i * gap : height / 2 }; });
    });
    return pos;
  }

  function runLayout(eles, fit) {
    var visible = eles.filter(":visible");
    if (layoutMode === "layered") {
      var pos = layeredPositions(visible.nodes());
      visible.nodes().layout({ name: "preset", positions: function (n) { return pos[n.id()]; }, fit: fit, padding: 40, animate: false }).run();
    } else {
      visible.layout({ name: "cose", animate: false, fit: fit, padding: 40, nodeRepulsion: 9000, idealEdgeLength: 60,
        randomize: true, componentSpacing: 60, nodeDimensionsIncludeLabels: false }).run();
    }
  }

  function applyFilters() {
    cy.batch(function () {
      cy.nodes().forEach(function (n) {
        var d = n.data();
        var show = !hidden[d.fam] && (!areaFilter || d.kind !== "requirement" || d.area === areaFilter);
        n.style("display", show ? "element" : "none");
      });
      if (areaFilter) {
        // With an area chosen, hide nodes that no visible requirement touches.
        var reqs = cy.nodes('[kind = "requirement"]').filter(":visible");
        var keep = reqs.union(reqs.neighborhood().nodes());
        keep = keep.union(keep.filter('[kind = "section"], [kind = "upstream"]').outgoers('edge[rel = "partOf"]').targets());
        cy.nodes().not(keep).style("display", "none");
      }
    });
    updateCount();
    runLayout(cy.elements(), true);
  }

  function updateCount() {
    var n = cy.nodes(":visible").length, e = cy.edges(":visible").length;
    document.getElementById("ex-count").textContent = n + " nodes · " + e + " links shown";
  }

  function clearHighlight() {
    cy.elements().removeClass("faded hl");
  }

  function highlight(node) {
    var hood = node.closedNeighborhood();
    cy.batch(function () {
      cy.elements().addClass("faded").removeClass("hl");
      hood.removeClass("faded").addClass("hl");
    });
  }

  function isolate(node, depth) {
    var set = node.closedNeighborhood();
    for (var i = 1; i < depth; i++) set = set.closedNeighborhood();
    cy.batch(function () {
      cy.elements().style("display", "none");
      set.style("display", "element");
    });
    clearHighlight();
    var dist = {};
    var bfs = set.bfs({ roots: node, directed: false, visit: function (v, e, u, i, d) { dist[v.id()] = d; } });
    set.layout({ name: "concentric", concentric: function (n) { return 10 - (dist[n.id()] || 0); },
      levelWidth: function () { return 1; }, minNodeSpacing: 50, animate: false, fit: true, padding: 60 }).run();
    if (cy.zoom() > 1.3) { cy.zoom(1.3); cy.center(set); }
    node.select();
    updateCount();
    setIsolated(true);
  }

  var isolated = false;
  function setIsolated(on) {
    isolated = on;
    var b = document.getElementById("btn-reset");
    if (b) b.hidden = !on;
  }

  function resetView() {
    setIsolated(false);
    applyFilters();
  }

  /* ---- Details panel ---- */

  function nodeLink(n) {
    return '<button type="button" class="d-link" data-goto="' + esc(n.id) + '">' + esc(n.label) + "</button>";
  }

  function relSection(id, rel, dir) {
    var items = S.related(G, id, rel, dir);
    if (!items.length) return "";
    var title = REL_LABEL[rel][dir === "in" ? 1 : 0];
    var lis = items.map(function (x) {
      var n = x.node, extra = "";
      if (n.kind === "requirement") extra = ' <span class="muted">' + esc(n.title) + "</span>";
      else if (n.kind === "section" || n.kind === "upstream" || n.kind === "issue" || n.kind === "step")
        extra = ' <span class="muted">' + esc(n.title) + "</span>";
      else if (n.kind === "api" && n.wit) extra = ' <span class="muted mono">' + esc(n.wit) + "</span>";
      var q = x.edge.quote ? '<span class="d-quote">“' + esc(x.edge.quote) + "”</span>" : "";
      var urls = (rel === "derivedFrom" || rel === "concerns") && dir !== "in" ? '<span class="d-urls">' + S.urlList(n.urls) + "</span>" : "";
      return "<li><span>" + nodeLink(n) + extra + "</span>" + q + urls + "</li>";
    }).join("");
    return '<div class="d-sec"><h4>' + esc(title) + " (" + items.length + ')</h4><ul class="d-list">' + lis + "</ul></div>";
  }

  function badge(text, cls) { return '<span class="badge ' + (cls || "") + '">' + esc(text) + "</span>"; }

  function renderDetail(id) {
    var n = G.byId[id], h = [];
    var c = colors();
    h.push('<div class="d-head"><span class="d-kind eyebrow">' + shapeSvg(SHAPE[n.kind], c[n.fam]) + esc(S.KIND_LABEL[n.kind]) + "</span>");
    h.push('<span class="mono small">' + esc(n.id) + '</span><span class="d-title">' + esc(n.title) + "</span></div>");
    if (n.kind === "requirement") {
      h.push('<div class="chips">' + badge(n.conformance, n.conformance.split(" ")[0].toLowerCase()) +
        '<span class="chip">' + esc(n.category) + '</span><span class="chip">' + esc(n.derivation) + "</span>" +
        n.targets.map(function (t) { return '<span class="chip">' + esc(t) + "</span>"; }).join("") + "</div>");
      h.push('<p class="d-statement">' + esc(n.statement) + "</p>");
      h.push('<div class="d-actions small"><a href="requirements.html#' + esc(n.id.toLowerCase()) + '">In the requirements document</a> · <a href="trace.html#' + esc(n.id) + '">In the matrix</a></div>');
      ["derivedFrom", "satisfies", "implementsStep", "constrainsApi", "refines", "verifiedBy", "affectedBy"].forEach(function (r) { h.push(relSection(id, r, "out")); });
      h.push(relSection(id, "refines", "in"));
      h.push(relSection(id, "verifiedBy", "in"));
    } else if (n.kind === "issue") {
      h.push('<div class="chips"><span class="issue-mark">open</span>' + (n.upstream ? '<span class="chip">fix upstream</span>' : '<span class="chip">this project</span>') + "</div>");
      h.push('<p class="small">' + esc(n.text) + "</p>");
      if (n.hint) h.push('<p class="small"><strong>Suggested resolution:</strong> ' + esc(n.hint) + "</p>");
      h.push(relSection(id, "affectedBy", "in"));
      h.push(relSection(id, "concerns", "out"));
    } else if (n.kind === "api") {
      h.push('<div class="chips"><span class="chip">' + esc(n.apiKind) + '</span><span class="chip">module ' + esc(n.module) + "</span></div>");
      if (n.wit) h.push('<p class="small">WIT: <a href="api.html#wit-' + esc(n.wit.split(".")[1]) + '"><code>' + esc(n.wit) + "</code></a></p>");
      if (n.witNote) h.push('<p class="small muted">Not in WIT: ' + esc(n.witNote) + "</p>");
      h.push('<div class="d-sec"><h4>Kotlin sketch</h4><pre class="d-code"><code>' + esc(n.kotlin) + "</code></pre></div>");
      h.push('<div class="d-sec"><h4>Rust sketch</h4><pre class="d-code"><code>' + esc(n.rust) + "</code></pre></div>");
      h.push(relSection(id, "constrainsApi", "in"));
    } else if (n.kind === "step") {
      h.push('<p class="small">Output: <code>' + esc(n.output) + "</code></p>");
      h.push(relSection(id, "derivedFrom", "out"));
      h.push(relSection(id, "implementsStep", "in"));
    } else {
      if (n.status === "pending-merge") h.push('<div class="chips"><span class="chip">in smart-trust PR 129, not yet merged</span></div>');
      if (n.note) h.push('<p class="small muted">' + esc(n.note) + "</p>");
      if (n.urls) h.push('<p class="d-urls">' + S.urlList(n.urls) + "</p>");
      h.push(relSection(id, "partOf", "out"));
      h.push(relSection(id, "partOf", "in"));
      h.push(relSection(id, "derivedFrom", "in"));
      h.push(relSection(id, "satisfies", "in"));
      h.push(relSection(id, "concerns", "in"));
    }
    h.push('<div class="d-actions"><button type="button" id="btn-iso1">Isolate neighbours</button><button type="button" id="btn-iso2">Two hops</button><button type="button" id="btn-reset"' + (isolated ? "" : " hidden") + ">Show whole graph</button></div>");
    var el = document.getElementById("detail");
    el.innerHTML = h.join("");
    el.scrollTop = 0;
    document.getElementById("btn-iso1").onclick = function () { isolate(cy.getElementById(id), 1); };
    document.getElementById("btn-iso2").onclick = function () { isolate(cy.getElementById(id), 2); };
    document.getElementById("btn-reset").onclick = resetView;
  }

  function select(id, opts) {
    var node = cy.getElementById(id);
    if (!node.length) return;
    if (node.style("display") === "none") {
      hidden[node.data("fam")] = false;
      syncFamilyButtons();
      areaFilter = ""; document.getElementById("ex-area").value = "";
      applyFilters();
    }
    cy.$(":selected").unselect();
    node.select();
    if (!isolated) highlight(node);
    renderDetail(id);
    if (opts && opts.center) cy.animate({ center: { eles: node }, zoom: Math.max(cy.zoom(), 1.1) }, { duration: 250 });
    try { history.replaceState(null, "", "#" + id); } catch (e) { /* sandboxed */ }
  }

  function syncFamilyButtons() {
    FAMILIES.forEach(function (f) {
      var b = document.getElementById("fam-" + f.id);
      if (b) b.setAttribute("aria-pressed", hidden[f.id] ? "false" : "true");
    });
  }

  function buildControls() {
    var counts = {};
    G.nodes.forEach(function (n) { counts[n.fam] = (counts[n.fam] || 0) + 1; });
    var famBox = document.getElementById("ex-families");
    var c = colors();
    FAMILIES.forEach(function (f) {
      var b = document.createElement("button");
      b.type = "button"; b.className = "fam-btn"; b.id = "fam-" + f.id; b.setAttribute("aria-pressed", "true");
      b.innerHTML = '<span class="swatch" style="background:' + c[f.id] + '"></span><span>' + esc(f.label) + '</span><span class="num muted">' + (counts[f.id] || 0) + "</span>";
      b.onclick = function () { hidden[f.id] = !hidden[f.id]; syncFamilyButtons(); setIsolated(false); applyFilters(); };
      famBox.appendChild(b);
    });
    var area = document.getElementById("ex-area");
    G.areas.forEach(function (a) {
      var o = document.createElement("option"); o.value = a[0]; o.textContent = a[0] + " — " + a[1]; area.appendChild(o);
    });
    area.onchange = function () { areaFilter = area.value; setIsolated(false); applyFilters(); };
    var dl = document.getElementById("ex-ids");
    G.nodes.forEach(function (n) {
      var o = document.createElement("option"); o.value = n.id; o.label = n.title; dl.appendChild(o);
    });
    var search = document.getElementById("ex-search");
    search.addEventListener("change", function () { find(search.value); });
    search.addEventListener("keydown", function (e) { if (e.key === "Enter") find(search.value); });
    var shapes = document.getElementById("ex-shapes");
    shapes.innerHTML = SHAPE_NOTE.map(function (s) {
      return '<li style="grid-template-columns:16px 1fr;display:grid;gap:6px;align-items:center">' + shapeSvg(s[0], c.fg2) + "<span>" + esc(s[1]) + "</span></li>";
    }).join("");
    document.getElementById("lay-layered").onclick = function () { setLayout("layered"); };
    document.getElementById("lay-force").onclick = function () { setLayout("force"); };
    document.getElementById("z-in").onclick = function () { cy.zoom({ level: cy.zoom() * 1.3, renderedPosition: { x: cy.width() / 2, y: cy.height() / 2 } }); };
    document.getElementById("z-out").onclick = function () { cy.zoom({ level: cy.zoom() / 1.3, renderedPosition: { x: cy.width() / 2, y: cy.height() / 2 } }); };
    document.getElementById("z-fit").onclick = function () { cy.fit(cy.elements(":visible"), 40); };
  }

  function setLayout(mode) {
    layoutMode = mode;
    document.getElementById("lay-layered").setAttribute("aria-pressed", mode === "layered");
    document.getElementById("lay-force").setAttribute("aria-pressed", mode === "force");
    setIsolated(false);
    applyFilters();
  }

  function find(q) {
    q = (q || "").trim();
    if (!q) return;
    var exact = G.byId[q] || G.byId[q.toUpperCase()];
    if (exact) return select(exact.id, { center: true });
    var lq = q.toLowerCase();
    var hit = G.nodes.find(function (n) {
      return n.label.toLowerCase().indexOf(lq) >= 0 || (n.title || "").toLowerCase().indexOf(lq) >= 0 ||
        (n.statement || "").toLowerCase().indexOf(lq) >= 0;
    });
    if (hit) select(hit.id, { center: true });
    else document.getElementById("ex-count").textContent = "No node matches “" + q + "”";
  }

  function init(g) {
    G = g;
    cy = cytoscape({
      container: document.getElementById("cy"), elements: elements(), style: style(),
      wheelSensitivity: 0.25, minZoom: 0.05, maxZoom: 4, boxSelectionEnabled: false
    });
    buildControls();
    applyFilters();
    cy.on("tap", "node", function (e) { select(e.target.id()); });
    cy.on("dbltap", "node", function (e) { isolate(e.target, 1); renderDetail(e.target.id()); });
    cy.on("tap", function (e) { if (e.target === cy) { cy.$(":selected").unselect(); if (!isolated) clearHighlight(); } });
    var mq = window.matchMedia("(prefers-color-scheme: dark)");
    var restyle = function () { cy.style(style()); };
    if (mq.addEventListener) mq.addEventListener("change", restyle);
    new MutationObserver(restyle).observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
    var h = decodeURIComponent((location.hash || "").slice(1));
    if (h && G.byId[h]) { select(h); isolate(cy.getElementById(h), 1); renderDetail(h); }
  }

  S.loadGraph().then(init).catch(function (err) {
    document.getElementById("ex-count").textContent = "Could not load the graph data (" + err.message + ").";
  });
})();
