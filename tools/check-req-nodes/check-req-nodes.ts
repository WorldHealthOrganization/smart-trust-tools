import { toolFactory } from "@litlfred/cat-harness/schemas/tool-folders.js";
import { defineTool } from "@litlfred/cat-harness/schemas/tool.js";

export default toolFactory((t) =>
  defineTool({
    id: "check-req-nodes",
    title: "Check the requirement graph",
    description:
      "Check every node of this instance's requirement graph: each skills/requirements/*.json parses as a bootstrap requirement with id req:<file>; every derivedFrom names a node that exists (a requirement, a library section or entry, an external schema, a reference, a directive, a test vector, or a smart-trust FHIR Requirements canonical); every success criterion names a test requirement; every skill's satisfies and every bean's requirements resolve.",
    install: { none: true },
    invoke: { shell: "bun tools/check-req-nodes/check.ts" },
    io: {
      inputs: [

      ],
      outputs: [
        { name: "report", schema: t("Text"), description: "One line per problem; exit 1 on any." },
      ],
    },
    satisfies: ["hcert-kg-publication"],
    selection: {
      when: "Before the site is built (it is the recipe's pre step), and whenever a requirement, skill or bean is edited.",
      limits: "Checks references resolve, not that a quote matches its source.",
      cost: "Under a second.",
    },
    requires: { runtime: ["bun"], network: false },
  }),
);
