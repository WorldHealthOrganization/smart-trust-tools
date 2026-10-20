#!/usr/bin/env bun
/**
 * Every file in skills/requirements/ is a valid cat-harness requirement node.
 *
 * cat-harness's own `check:skills` and `check:requirements` walk the
 * cat-harness instance only, so this downstream checks its nodes itself,
 * against the same bootstrap `RequirementSchema`, plus what build_kg.py relies
 * on: the id is `req:<file name>`, every `req:` in `derivedFrom` and every
 * success criterion key names a node here.
 *
 *   bun tools/check_req_nodes.ts
 */
import { readdirSync, readFileSync } from "node:fs";
import { basename, join } from "node:path";

import { RequirementSchema } from "@litlfred/bootstrap-tools/schemas/requirement.ts";

const DIR = join(import.meta.dir, "..", "skills", "requirements");
const files = readdirSync(DIR).filter((n) => n.endsWith(".json")).sort();
const nodes = new Map<string, { file: string; node: Record<string, any> }>();
const problems: string[] = [];

for (const file of files) {
  const node = JSON.parse(readFileSync(join(DIR, file), "utf-8"));
  const r = RequirementSchema.safeParse(node);
  if (!r.success) for (const i of r.error.issues) problems.push(`${file}: ${i.path.join(".") || "(root)"}: ${i.message}`);
  if (node.id !== `req:${basename(file, ".json")}`) problems.push(`${file}: id ${node.id} is not req:${basename(file, ".json")}`);
  nodes.set(node.id, { file, node });
}
for (const { file, node } of nodes.values()) {
  for (const d of node.derivedFrom ?? []) {
    if (d.startsWith("req:") && !nodes.has(d)) problems.push(`${file}: derivedFrom ${d} names no node`);
  }
  for (const s of node.statements ?? []) {
    for (const c of s.successCriteria ?? []) {
      if (!nodes.has(`req:${c.key}`)) problems.push(`${file}: success criterion ${c.key} names no test requirement`);
    }
  }
}

for (const p of problems) console.error(`  ✗ ${p}`);
console.log(`${files.length} requirement node(s) in skills/requirements/; ${problems.length} problem(s)`);
process.exit(problems.length ? 1 : 0);
