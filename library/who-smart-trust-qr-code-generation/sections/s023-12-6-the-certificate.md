---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s023-12-6-the-certificate
section_title: "12.6 The certificate"
file: "input/pagecontent/qr_code_generation.md"
lines: 397-426
source_sha256: 25cc22304a1d036e
granularity: heading
---
##### 12.6 The certificate

Hepatitis B birth dose:

<div style="display:block">
  <img src="dvc-qr-example-hepb1-XXA.png" alt="Sample DVC QR code, Hepatitis B birth dose, test participant XXA" width="300" />
</div>

```
HC1:NCFOXN%TSMAHN-HWM4/GR3.M: AQSSGAH9M91RIHF4NZKC7TFSTYUIBRR34Q-MPD ND%ERJC/HJI+8H-BI+8F.9HJ8 4GFDJ38KLUKAQC419%EEGJB/.D4-9MJCVPLW1KD0K9JAF/BT3EQ*9%NH7$K MI..3EEWVPMD+K$F91EF+:L*.9G%8M+8CNNE1J $47LPNV4YW8490-LN258VUUZX2LV34G8-:V4SI5SPDDK2XEPS5OH6CN5MZUCRQPK50F2TK1AVU%J6WLITK22VE TUVI13M9LREHOPJPQJVFKWOBY5PRALTIHR6L47CX53OVK682Y7IKUQK3.$AP.CE.CG%DAWCD8SSCH3ME6OHG5MNRH.8LTOT06Q/.F4KL4-GSP66G1%WR/+BG2M6ARB84I*2-J5-7J$*0XFSLLD
```

BCG, the second certificate from the same visit:

<div style="display:block">
  <img src="dvc-qr-example-bcg-XXA.png" alt="Sample DVC QR code, BCG birth dose, test participant XXA" width="300" />
</div>

```
HC1:NCFOXN%TSMAHN-HWM4/GR3.M: AQSSZBH9M91RIHF4NZKC7TFSTYUIBRR34Q-MPD ND%ERJC/HJI+8H-BI+8F.9HJ8 4GFDJ38KLUKAQC419%EEGJB/.D4-9MJCVPLW1KD0K9JAF/BT3EQ*9%NH7$K MI..3EEWVPMD+K$F98JN97QVONVZ0I:C:Y0P8TYE98EFA%5-B1NJJ+JP:N3ZSMDOOSM8-T3PR3:KAFNM2LHX%EPS5OH6CN5MZUCRQPK50F2TK1AVU%J6WLITK22VE TUVI13M9LREHOPJPQJVF:O1VYHPRALTIHR6B67CVAYSUNOA*IF.W6R B1Z39$7HT08TD/ ERK1M8SWHPS HDYD%7IB70AYQ6-SSPNIPFEF19*NCR6/6EU0GB$RMWVO*AISB8TL7ZVV50$:D4KD
```

The `kid` for both is `WHM+BYqqZiE=` (hex `58733e058aaa6621`).

The test DSC and the test SCA that signed it are published alongside the images
as `dvc-qr-example-XXA-dsc.pem`, so that a verifier implementation can be
exercised against this example end to end. A machine-readable summary of both
certificates, including the payloads and the full `HC1:` strings, is in
`dvc-qr-example-XXA.json`.
