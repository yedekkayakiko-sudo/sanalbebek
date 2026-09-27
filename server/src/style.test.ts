import { test } from "node:test";
import assert from "node:assert/strict";
import { isProfane, styleProfile, detectTeam, teamAffinity, chooseTeam } from "./style.ts";

test("küfür ve hakaret yakalanır, masum kelimeler yakalanmaz", () => {
  for (const w of ["amk", "siktir", "orospu", "salak", "mal", "Aptal"]) assert.equal(isProfane(w), true, w);
  for (const w of ["sıkıldım", "malzeme", "top", "abi", "gotik", "maltepe"]) assert.equal(isProfane(w), false, w);
});

test("üslup: sık laflar, gülme ve ton", () => {
  const msgs = ["abi naber!", "yaa abi sjsjsj", "hadi abi!", "sjsjsj yaa", "yaa tamam!", "salak şey"];
  const s = styleProfile(msgs);
  assert.deepEqual(s.laflar, ["abi", "yaa"]);
  assert.equal(s.gulme, "sjsj");
  assert.equal(s.ton, "coskulu");
  assert.equal(s.bip_sayisi, 1);
});

test("takım renkle tanınır", () => {
  assert.equal(detectTeam("Bu akşam Fenerbahçe maçı var"), "sari-lacivert");
  assert.equal(detectTeam("cimbom!"), "sari-kirmizi");
  assert.equal(detectTeam("top oynayalım"), null);
});

test("iki ebeveyn farklı takımda: forma hediyesi ve ilişki etkiler, seçim çocuklukta", () => {
  const t = teamAffinity([
    { parentId: "baba", rel: 80, text: "fener" }, { parentId: "baba", rel: 80, text: "kanarya" },
    { parentId: "anne", rel: 40, text: "cimbom" }, { parentId: "anne", rel: 40, gift: "sari-kirmizi" },
  ]);
  assert.deepEqual(t.ebeveyn_takimlari, { baba: "sari-lacivert", anne: "sari-kirmizi" });
  assert.equal(t.egilim["sari-lacivert"], 2.6);
  assert.equal(t.egilim["sari-kirmizi"], 2.7);
  assert.equal(chooseTeam(t, 4), null);        // çocukluktan önce seçmez
  assert.equal(chooseTeam(t, 5), null);        // 3 puanı geçen yok
  const t2 = teamAffinity([{ parentId: "baba", rel: 80, gift: "sari-lacivert" }, { parentId: "baba", rel: 80, text: "fener" }]);
  assert.equal(chooseTeam(t2, 5), "sari-lacivert");
});
