import { test } from "node:test";
import assert from "node:assert/strict";
import { isAllowedSpeech, isAllowedNewWord, isAllowedGesture, isAllowedNote, sanitize, firstSyllable, heardSyllables, ruleGesture } from "./guard.ts";
import type { BabyState } from "./types.ts";

const base: BabyState = {
  ad: "Milo", donem: 0, yas_gun: 1, saat: "10:00", uyuyor: false, gece_aglama: false,
  aclik: 30, enerji: 70, ilgi: 60, ebeveynler: [{ id: "p1", ad: "Sen", cagri_adi: "anne", iliski: 50 }],
  bakan: "p1", kelimeler: [], duyulanlar: {}, heceler: [], bugun_yeni_kelime: false, anilar: [],
  son_hareketler: [], ritim: null, defter: [],
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
  assert.equal(isAllowedSpeech(s, "to-to!"), false);
});

test("heceler ebeveynin kelimelerinden gelir", () => {
  assert.equal(firstSyllable("top"), "to");
  assert.equal(firstSyllable("kedi"), "ke");
  assert.deepEqual(heardSyllables({ top: 4, kedi: 2, su: 1 }), ["to", "ke"]);
  const s = { ...base, donem: 2 as const, heceler: ["to"] };
  assert.equal(isAllowedSpeech(s, "to-to!"), true);
  const y = { ...base, donem: 3 as const, heceler: ["to"] };
  assert.equal(isAllowedSpeech(y, "bada-to?"), true);
  assert.equal(isAllowedSpeech(y, "top oynayalım"), false);
});

test("hareketler döneme bağlı", () => {
  assert.equal(isAllowedGesture(base, "gerin"), true);
  assert.equal(isAllowedGesture(base, "zipla"), false);
  assert.equal(isAllowedGesture({ ...base, donem: 2 }, "isaret:kase"), true);
  assert.equal(isAllowedGesture({ ...base, donem: 2 }, "isaret:mutfak"), false);
  const out = sanitize(base, { konusma: "", davranis: "gulumsuyor", hareket: "dans", gunluk: "", yeni_kelime: null, defter_notu: null });
  assert.equal(out.hareket, "goz_temasi");
});

test("tanıma defteri: 3 gün veri şart, hassas konu yasak", () => {
  const ritim = { gun_sayisi: 5 };
  assert.equal(isAllowedNote({ ...base, ritim: { gun_sayisi: 2 } }, "Sabahları 07:30 civarı uyanıyorsun."), false);
  assert.equal(isAllowedNote({ ...base, ritim }, "Sabahları 07:30 civarı uyanıyorsun."), true);
  assert.equal(isAllowedNote({ ...base, ritim }, "Son günlerde stresli görünüyorsun."), false);
  assert.equal(isAllowedNote({ ...base, ritim }, "Çok geç yatıyorsun."), false);
  assert.equal(isAllowedNote({ ...base, ritim, defter: ["Sabahları 07:30 civarı uyanıyorsun."] }, "sabahları 07:30 civarı uyanıyorsun."), false);
});

test("ritim kural katmanında da kullanılır", () => {
  const s = { ...base, donem: 3 as const, ritim: { gun_sayisi: 5, bugun: { kulaklik_takili: true } } };
  assert.equal(ruleGesture(s), "dans");
  const w = { ...base, donem: 3 as const, ritim: { gun_sayisi: 5, bugun: { yuruyuse_kalan_dk: 20 } } };
  assert.equal(ruleGesture(w), "kapida_bekle");
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
  const out = sanitize(s, { konusma: "agu", davranis: "el_salliyor", hareket: "gerin", gunluk: "x", yeni_kelime: null, defter_notu: null });
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
  const out = sanitize(s, { konusma: "", davranis: "dans_ediyor" as never, hareket: "yok", gunluk: "", yeni_kelime: null, defter_notu: null });
  assert.equal(out.davranis, "mutfaga_bakiyor");
});
