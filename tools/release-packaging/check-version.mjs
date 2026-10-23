// One version for every artefact (STT-NFR-001): VERSION at the repository
// root is the source. This fails if it is not a semantic version, if
// tools/hcert-rust/Cargo.toml says otherwise, if the Gradle build stops
// reading it (or a module sets its own version), or if CHANGELOG.md has no
// entry for it. Each directory given is searched for built artefacts too:
// every package.json must carry the version, no dependencies and no scripts,
// and every .pom must carry the version.
//
//   node tools/release-packaging/check-version.mjs [<dist-or-repo-dir> ...]
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const repo = fileURLToPath(new URL("../../", import.meta.url));
const read = (p) => readFileSync(join(repo, p), "utf8");
const problems = [];

const version = read("VERSION").trim();
// semver.org 2.0.0, without build metadata (registries ignore it).
if (!/^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$/.test(version)) {
  problems.push(`VERSION: "${version}" is not a semantic version`);
}

const cargo = read("tools/hcert-rust/Cargo.toml");
const pkg = cargo.split(/^\[/m).find((s) => s.startsWith("package]")) ?? "";
const cargoVersion = /^version\s*=\s*"([^"]*)"/m.exec(pkg)?.[1];
if (cargoVersion !== version) problems.push(`tools/hcert-rust/Cargo.toml: version ${cargoVersion} != VERSION ${version}`);
const lock = read("tools/hcert-rust/Cargo.lock");
const lockVersion = /\[\[package\]\]\nname = "hcert"\nversion = "([^"]*)"/.exec(lock)?.[1];
if (lockVersion !== version) problems.push(`tools/hcert-rust/Cargo.lock: hcert ${lockVersion} != VERSION ${version}`);

const gradle = read("tools/hcert-kotlin/build.gradle.kts");
if (!gradle.includes('rootProject.file("../../VERSION").readText().trim()')) {
  problems.push("tools/hcert-kotlin/build.gradle.kts: no longer reads ../../VERSION");
}
for (const m of ["hcert-api", "hcert-core"]) {
  if (/^\s*version\s*=/m.test(read(`tools/hcert-kotlin/${m}/build.gradle.kts`))) {
    problems.push(`tools/hcert-kotlin/${m}/build.gradle.kts: sets its own version; VERSION is the only source`);
  }
}

const changelog = read("CHANGELOG.md");
if (!new RegExp(`^## \\[${version.replace(/\./g, "\\.")}\\]`, "m").test(changelog)) {
  problems.push(`CHANGELOG.md: no "## [${version}]" entry`);
}

function* files(dir, depth) {
  for (const n of readdirSync(dir).sort()) {
    const p = join(dir, n);
    if (statSync(p).isDirectory()) {
      if (depth > 0 && n !== "node_modules") yield* files(p, depth - 1);
    } else yield p;
  }
}
let artefacts = 0;
for (const dir of process.argv.slice(2)) {
  if (!existsSync(dir)) { problems.push(`${dir}: no such directory`); continue; }
  for (const f of files(dir, 8)) {
    if (f.endsWith("/package.json")) {
      artefacts++;
      const j = JSON.parse(readFileSync(f, "utf8"));
      if (j.version !== version) problems.push(`${f}: version ${j.version} != VERSION ${version}`);
      for (const k of ["dependencies", "peerDependencies", "optionalDependencies", "scripts"]) {
        if (j[k] && Object.keys(j[k]).length) problems.push(`${f}: has ${k}`);
      }
    } else if (f.endsWith(".pom")) {
      artefacts++;
      const v = /<artifactId>[^<]*<\/artifactId>\s*<version>([^<]*)<\/version>/.exec(readFileSync(f, "utf8"))?.[1];
      if (v !== version) problems.push(`${f}: version ${v} != VERSION ${version}`);
    }
  }
}

if (problems.length) {
  for (const p of problems) console.error(`FAIL ${p}`);
  process.exit(1);
}
console.log(`ok   version ${version}: VERSION, Cargo.toml, Cargo.lock, Gradle, CHANGELOG.md${artefacts ? `, ${artefacts} built artefact(s)` : ""}`);
