#!/usr/bin/env bun
/**
 * The requirement graph of this instance resolves: no edge points at nothing.
 *
 * cat-harness's own `check:skills` and `check:requirements` walk the
 * cat-harness instance only, so this downstream checks its nodes itself,
 * against the same bootstrap `RequirementSchema`, and checks every edge the
 * viewers draw:
 *
 * - a requirement's id is `req:<file name>`;
 * - each `derivedFrom` names a requirement here (the harness's meaning: the
 *   broader requirement this one specialises);
 * - each `reference` names a node that exists: a library entry or section, an
 *   external schema, a reference, a memory directive, a test vector, or a
 *   smart-trust FHIR Requirements canonical (an instance in the ingested
 *   Requirements.fsh, with its statement key if one is given). The field is
 *   FHIR R5 `Requirements.reference`, the artefacts a requirement was drawn
 *   from;
 * - each success criterion names a requirement;
 * - each skill's `satisfies` names a requirement and one of its statements;
 * - each bean's "Requirements affected" and "Sources" resolve likewise.
 *
 *   bun tools/check-req-nodes/check.ts
 */
import { existsSync, readdirSync, readFileSync } from "node:fs";
import { basename, join } from "node:path";

import { RequirementSchema } from "@litlfred/bootstrap-tools/schemas/requirement.ts";

const ROOT = join(import.meta.dir, "..", "..");
const REQ_DIR = join(ROOT, "skills", "requirements");
const UPSTREAM = "http://smart.who.int/trust/Requirements/";
const problems: string[] = [];

const files = readdirSync(REQ_DIR).filter((n) => n.endsWith(".json")).sort();
const nodes = new Map<string, { file: string; node: Record<string, any> }>();
for (const file of files) {
  const node = JSON.parse(readFileSync(join(REQ_DIR, file), "utf-8"));
  const r = RequirementSchema.safeParse(node);
  if (!r.success) for (const i of r.error.issues) problems.push(`${file}: ${i.path.join(".") || "(root)"}: ${i.message}`);
  if (node.id !== `req:${basename(file, ".json")}`) problems.push(`${file}: id ${node.id} is not req:${basename(file, ".json")}`);
  nodes.set(node.id, { file, node });
}

// The smart-trust FHIR Requirements, read from the ingested Requirements.fsh.
const upstream = new Map<string, Set<string>>();
const fshDir = join(ROOT, "library", "who-smart-trust-requirements-fsh", "sections");
for (const f of existsSync(fshDir) ? readdirSync(fshDir).filter((n) => n.endsWith(".md")) : []) {
  let current: Set<string> | undefined;
  for (const line of readFileSync(join(fshDir, f), "utf-8").split("\n")) {
    const inst = /^Instance:\s+(\S+)/.exec(line);
    if (inst) upstream.set(inst[1]!, (current = new Set()));
    const key = /^\* statement\[\+\]\.key = "([^"]+)"/.exec(line);
    if (key && current) current.add(key[1]!);
  }
}

/** Whether a reference names a node of this graph. */
function resolves(ref: string): boolean {
  if (ref.startsWith("req:")) return nodes.has(ref.split("#")[0]!);
  if (ref.startsWith(UPSTREAM)) {
    const [inst, key] = ref.slice(UPSTREAM.length).split("#");
    const stmts = upstream.get(inst!);
    return stmts !== undefined && (key === undefined || stmts.has(key));
  }
  const m = /^library\/([^/]+)(?:\/sections\/([^/]+))?$/.exec(ref);
  if (m) return existsSync(join(ROOT, "library", m[1]!, m[2] ? `sections/${m[2]}.md` : "manifest.jsonld"));
  for (const [dir, ext] of [["external-schemas", ".json"], ["references", ".json"], ["memory", ".md"]] as const) {
    if (ref.startsWith(`${dir}/`)) return existsSync(join(ROOT, `${ref}${ext}`));
  }
  if (ref.startsWith("vectors/")) return existsSync(join(ROOT, ref));
  return false;
}

for (const { file, node } of nodes.values()) {
  for (const d of node.derivedFrom ?? []) {
    if (!d.startsWith("req:") || !nodes.has(d)) problems.push(`${file}: derivedFrom ${d} names no requirement`);
  }
  for (const d of node.reference ?? []) {
    if (d.startsWith("req:") || !resolves(d)) problems.push(`${file}: reference ${d} names no source node`);
  }
  for (const s of node.statements ?? []) {
    for (const c of s.successCriteria ?? []) {
      if (!nodes.has(`req:${c.key}`)) problems.push(`${file}: success criterion ${c.key} names no requirement`);
    }
  }
}

/** `req:<id>#<statement key>`, checked against the node's own statements. */
function statementResolves(ref: string): boolean {
  const [id, key] = ref.split("#");
  const n = nodes.get(id!);
  return n !== undefined && (key === undefined || (n.node.statements ?? []).some((s: { key: string }) => s.key === key));
}

let skills = 0;
for (const pkg of readdirSync(join(ROOT, "skills"), { withFileTypes: true })) {
  if (!pkg.isDirectory() || pkg.name === "requirements") continue;
  for (const f of readdirSync(join(ROOT, "skills", pkg.name)).filter((n) => n.endsWith(".md"))) {
    skills++;
    const front = /^---\n([\s\S]*?)\n---/.exec(readFileSync(join(ROOT, "skills", pkg.name, f), "utf-8"))?.[1] ?? "";
    for (const m of front.matchAll(/^\s+- "(req:[^"]+)"/gm)) {
      if (!statementResolves(m[1]!)) problems.push(`skills/${pkg.name}/${f}: satisfies ${m[1]} names no requirement statement`);
    }
  }
}

const beanDir = join(ROOT, "beans", "defs");
const beans = existsSync(beanDir) ? readdirSync(beanDir).filter((n) => n.endsWith(".md")) : [];
for (const f of beans) {
  const text = readFileSync(join(beanDir, f), "utf-8");
  for (const section of ["Requirements affected", "Sources"]) {
    const body = new RegExp(`^## ${section}\\n\\n((?:- .*\\n?)+)`, "m").exec(text)?.[1] ?? "";
    for (const m of body.matchAll(/^- (\S+)/gm)) {
      if (!resolves(m[1]!)) problems.push(`beans/defs/${f}: ${section} ${m[1]} names no node`);
    }
  }
}

for (const p of problems) console.error(`  ✗ ${p}`);
console.log(`${files.length} requirement(s), ${skills} skill(s), ${beans.length} bean(s), ${upstream.size} smart-trust Requirements; ${problems.length} problem(s)`);
process.exit(problems.length ? 1 : 0);
