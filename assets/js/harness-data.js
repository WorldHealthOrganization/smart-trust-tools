// The harness data that is the same on every page of a build, written ONCE
// here instead of into each page (bean `gp2f`, page weight). Measured on
// gh-pages 1ad53fa, 2026-10-23: the tiles list was 12.5 KB and the navbar icon
// row 2.5 KB of every Jekyll page, about 15 KB of a 42 KB page.
//
// `head_custom.html` keeps the two places the readers look, empty, and loads
// this as the FIRST deferred script. Deferred scripts run in document order,
// so `navbar-row.js` and `docs-ui.js` find them filled, exactly as when the
// data was inlined. If this file does not load, the islands stay named and
// empty, and the readers report a failed load instead of "declared none".
(function () {
  var row = document.getElementById("fa-navbar-row");
  if (row && row.getAttribute("data-fa-row-src") === "harness-data" && !row.textContent) {
    row.textContent = "{\"icons\":[\"close\",\"qr\",\"launcher\"],\"hrefs\":{},\"notes\":{},\"folders\":[{\"kind\":\"beans\",\"label\":\"Plans and work\",\"note\":\"smart-trust-tools's beans graph is not published on this site\"},{\"kind\":\"code\",\"label\":\"Code\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/code/smart-trust-tools/\"},{\"kind\":\"code-list\",\"label\":\"Code lists\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/code-lists/smart-trust-tools/\"},{\"kind\":\"docs\",\"label\":\"Docs\",\"note\":\"no viewer yet\"},{\"kind\":\"external-schema\",\"label\":\"External schemas\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/external-schemas/instance/smart-trust-tools/\"},{\"kind\":\"fhir-logical-models\",\"label\":\"FHIR logical models\",\"note\":\"no viewer yet\"},{\"kind\":\"glossary\",\"label\":\"Glossary\",\"note\":\"no viewer yet\"},{\"kind\":\"hcert-contract\",\"label\":\"HCERT library contract\",\"note\":\"no viewer yet\"},{\"kind\":\"hcert-generated-contract\",\"label\":\"Generated contract views\",\"note\":\"no viewer yet\"},{\"kind\":\"hcert-test-vectors\",\"label\":\"HCERT test vectors\",\"note\":\"no viewer yet\"},{\"kind\":\"library\",\"label\":\"Library\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/library/smart-trust-tools/\"},{\"kind\":\"memory\",\"label\":\"Memory\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/memory/smart-trust-tools/\"},{\"kind\":\"payload-models\",\"label\":\"Derived payload models\",\"note\":\"no viewer yet\"},{\"kind\":\"processes\",\"label\":\"Processes\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/processes/smart-trust-tools/\"},{\"kind\":\"references\",\"label\":\"References\",\"note\":\"no viewer yet\"},{\"kind\":\"scenarios\",\"label\":\"Scenarios\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/scenarios/smart-trust-tools/\"},{\"kind\":\"schemas\",\"label\":\"Schemas\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/schemas/smart-trust-tools/\"},{\"kind\":\"skills\",\"label\":\"Skills\",\"note\":\"no viewer yet\"},{\"kind\":\"test-plan\",\"label\":\"Test plan\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/test-plans/smart-trust-tools/\"},{\"kind\":\"tools\",\"label\":\"Tools\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/tools/smart-trust-tools/\"},{\"kind\":\"typologies\",\"label\":\"Graph typologies\",\"path\":\"https://litlfred.github.io/folio-assistant/en/cat-harness/graph-typologies/smart-trust-tools/\"},{\"kind\":\"upstream-patches\",\"label\":\"Upstream patches\",\"note\":\"no viewer yet\"}]}";
    row.removeAttribute("data-fa-row-src");
  }
  var tiles = document.querySelector('meta[name="fa-tiles"][data-fa-src="harness-data"]');
  if (tiles && !tiles.getAttribute("content")) {
    tiles.setAttribute("content", "[]");
    tiles.removeAttribute("data-fa-src");
  }
})();
