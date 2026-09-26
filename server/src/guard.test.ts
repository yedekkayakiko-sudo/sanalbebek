import { test } from "node:test";
import assert from "node:assert/strict";
import { isAllowedSpeech, isAllowedNewWord, sanitize } from "./guard.ts";
import type { BabyState } from "./types.ts";

const base: BabyState = {
  ad: "Milo", donem: 0, yas_gun: 1, saat: "10:00", uyuyor: false, gece_aglama: false,
  aclik: 30, enerji: 70, ilgi: 60, ebeveynler: [{ id: "p1", ad: "Sen", cagri_adi: "anne", iliski: 50 }],
  bakan: "p1", kelimeler: [], duyulanlar: {}, bugun_yeni_kelime: false, anilar: [],
};

test("yenidoğan kelime söyleyemez", () => {
  assert.equal(isAllowedSpeech(base, "hıı"), true);
  assert.equal(isAllowedSpeech(base, "mama"), false);
  assert.equal(isAllowedSpeech(base, "Merhaba anne, nasılsın?"), false);
});

test("emekleme hece zinciri söyler, kelime söyleyemez", () => {
  const s = { ...base, donem: 2 as const };
  assert.equal(isAllowedSpeech(s, "ma-ma-ma"), true);
  assert.equal(isAllowedSpeech(s, "top"), false);
});

test("ilk kelimeler döneminde sadece bilinen tek kelime", () => {
  const s = { ...base, donem: 4 as const, kelimeler: ["mama", "top"] };
  assert.equal(isAllowedSpeech(s, "mama!"), true);
  assert.equal(isAllowedSpeech(s, "kedi"), false);
  assert.equal(isAllowedSpeech(s, "mama top"), false);
});

test("çocukluk en fazla iki kelime", () => {
  const s = { ...base, donem: 5 as const, kelimeler: ["mama", "top"] };
  assert.equal(isAllowedSpeech(s, "mama ver"), true);
  assert.equal(isAllowedSpeech(s, "mama ver top"), false);
});

test("uyurken konuşma yok, davranış uyku olur", () => {
  const s = { ...base, uyuyor: true };
  const out = sanitize(s, { konusma: "agu", davranis: "el_salliyor", gunluk: "x", yeni_kelime: null });
  assert.equal(out.konusma, "");
  assert.equal(out.davranis, "uyuyor");
});

test("yeni kelime 3 kez duyulmadan öğrenilmez", () => {
  const s = { ...base, donem: 4 as const, duyulanlar: { top: 2, kedi: 3 } };
  assert.equal(isAllowedNewWord(s, "top"), false);
  assert.equal(isAllowedNewWord(s, "kedi"), true);
  assert.equal(isAllowedNewWord({ ...s, bugun_yeni_kelime: true }, "kedi"), false);
});

test("bilinmeyen poz kural tabanlı poza düşer", () => {
  const s = { ...base, aclik: 90 };
  const out = sanitize(s, { konusma: "", davranis: "dans_ediyor" as never, gunluk: "", yeni_kelime: null });
  assert.equal(out.davranis, "mutfaga_bakiyor");
});
