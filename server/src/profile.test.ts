import { test } from "node:test";
import assert from "node:assert/strict";
import { computeRhythm, type Signal } from "./profile.ts";

const TZ = 180, MIN = 60_000, HOUR = 60 * MIN, DAY = 24 * HOUR;
// Yerel saat (UTC+3) ile gün d, saat h:m → UTC ms
const at = (d: number, h: number, m = 0) => Date.UTC(2026, 9, 1 + d, h, m) - TZ * MIN;
const all = { ekran: true, adim: true, sarj_kulaklik: true };

function week(days: number): Signal[] {
  const s: Signal[] = [];
  for (let d = 0; d < days; d++) {
    const j = (d % 3) * 5;                                   // küçük oynama
    s.push({ t: at(d, 7, 25 + j), type: "screen_on" });
    s.push({ t: at(d, 7, 52 + (d % 2) * 6), type: "headphone_on" }, { t: at(d, 8, 45), type: "headphone_off" });   // 07:52 başlasa da 07 saati sayılmaz
    for (let k = 0; k < 5; k++) s.push({ t: at(d, 10, k * 10), type: "screen_on" });
    s.push({ t: at(d, 18, 10), type: "steps", value: 2400 }, { t: at(d, 12, 0), type: "steps", value: 600 });
    s.push({ t: at(d, 23, 50), type: "charge_on" });
    s.push({ t: at(d + 1, 0, 30 + j), type: "screen_off" });
  }
  return s;
}

test("3 günden az veriyle ritim söylenmez", () => {
  const r = computeRhythm(week(2), at(2, 12), all, TZ)!;
  assert.equal(r.gun_sayisi, 2);
  assert.equal(r.uyanma, undefined);
});

test("uyanma, yatma, yürüyüş, kulaklık ve yoğun saatler çıkarılır", () => {
  const r = computeRhythm(week(5), at(5, 7, 0), all, TZ)!;
  assert.equal(r.gun_sayisi, 5);
  assert.equal(r.uyanma, "07:30");
  assert.equal(r.yatma, "00:35");        // gece yarısından sonra: aynı güne sayılır
  assert.equal(r.gece_sarj, "23:50");
  assert.equal(r.yuruyus_saati, "18:00");
  assert.equal(r.ortalama_adim, 3000);
  assert.deepEqual(r.kulaklik_saatleri, ["08:00-09:00"]);
  assert.deepEqual(r.yogun_saatler, ["10:00-11:00"]);
});

test("bugünün farkı: geç uyanma ve kulaklık", () => {
  const s = [...week(5), { t: at(5, 9, 5), type: "screen_on" as const }, { t: at(5, 9, 10), type: "headphone_on" as const }];
  const r = computeRhythm(s, at(5, 9, 20), all, TZ)!;
  assert.equal(r.bugun!.uyanma_farki_dk, 95);
  assert.equal(r.bugun!.kulaklik_takili, true);
});

test("rıza verilmeyen sinyal kullanılmaz", () => {
  const r = computeRhythm(week(5), at(5, 7), { ekran: true, adim: false, sarj_kulaklik: false }, TZ)!;
  assert.equal(r.uyanma, "07:30");
  assert.equal(r.yuruyus_saati, undefined);
  assert.equal(r.kulaklik_saatleri, undefined);
  assert.equal(computeRhythm(week(5), at(5, 7), { ekran: false, adim: false, sarj_kulaklik: false }, TZ), null);
});
