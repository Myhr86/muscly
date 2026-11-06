import { test } from "node:test";
import assert from "node:assert";
import { outputOk, YOUTH_SYSTEM_PROMPT } from "./safety.js";

test("blokkerer farlige råd", () => {
  assert.equal(outputOk("Ta kreatin hver dag"), false);
  assert.equal(outputOk("Gå i kaloriunderskudd"), false);
  assert.equal(outputOk("Knebøy og planke er supre øvelser!"), true);
});
test("systemprompt inneholder ungdomsregler", () => {
  for (const w of ["kalorirestriksjon", "kosttilskudd", "smerte", "helsesykepleier"]) assert.ok(YOUTH_SYSTEM_PROMPT.includes(w));
});
