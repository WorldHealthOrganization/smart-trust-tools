---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s007-2-4-decision-summary
section_title: "2.4 Decision summary"
file: "input/pagecontent/qr_code_generation.md"
lines: 108-122
source_sha256: 25cc22304a1d036e
granularity: heading
---
##### 2.4 Decision summary

```
Is the certificate proving an IHR entry requirement (e.g. yellow fever)?
   ├── yes ──▶ ICVP trust domain
   └── no
        │
        Does the content fit in a QR and have to verify with no network?
           ├── yes ──▶ DVC payload, hcert subclaim -6   (DVCMin / DVCMinPreQual)
           └── no  ──▶ VHL URI,     hcert subclaim 5
```

COVID-19 vaccination and test results predate this split and have their own
assigned subclaims (`3` and `4`) under the `DDCC` trust domain.
