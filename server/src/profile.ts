import type { Rhythm } from "./types.ts";

// Ebeveynin ritmini cihaz sinyallerinden çıkarır. Deterministik, AI yok.
// Beyne sadece bu özet gider; ham sinyal (saat saat kilit açma) sunucuda kalır ve 30 gün sonra silinir.

export type SignalType = "screen_on" | "screen_off" | "charge_on" | "charge_off" | "headphone_on" | "headphone_off" | "steps";
export interface Signal { t: number; type: SignalType; value?: number }   // steps: o saatteki adım sayısı
export type Consent = { ekran: boolean; adim: boolean; sarj_kulaklik: boolean };

const MIN = 60_000, DAY_START_MIN = 4 * 60;      // gün 04:00'te başlar: gece yarısından sonra yatmak aynı güne sayılır
const allowed = (s: Signal, c: Consent) =>
  s.type.startsWith("screen") ? c.ekran : s.type === "steps" ? c.adim : c.sarj_kulaklik;

function median(xs: number[]): number | undefined {
  if (!xs.length) return undefined;
  const a = [...xs].sort((x, y) => x - y), m = a.length >> 1;
  return a.length % 2 ? a[m] : (a[m - 1] + a[m]) / 2;
}
/** 04:00'ten itibaren geçen dakika (0..1439). 00:30 → 1230. */
const dayMinute = (localMin: number) => (localMin - DAY_START_MIN + 1440) % 1440;
const fmt = (dm: number) => {
  const m = Math.round(((dm + DAY_START_MIN) % 1440) / 5) * 5 % 1440;
  return `${String(Math.floor(m / 60)).padStart(2, "0")}:${String(m % 60).padStart(2, "0")}`;
};
function ranges(hours: number[]): string[] {
  const hs = [...new Set(hours)].sort((a, b) => a - b), out: string[] = [];
  for (let i = 0; i < hs.length; i++) {
    let j = i; while (j + 1 < hs.length && hs[j + 1] === hs[j] + 1) j++;
    out.push(`${String(hs[i]).padStart(2, "0")}:00-${String((hs[j] + 1) % 24).padStart(2, "0")}:00`); i = j;
  }
  return out;
}

export function computeRhythm(signals: Signal[], now: number, consent: Consent, tzOffsetMin = 180): Rhythm | null {
  if (!consent.ekran && !consent.adim && !consent.sarj_kulaklik) return null;
  const local = (t: number) => t + tzOffsetMin * MIN;
  const dayKey = (t: number) => Math.floor((local(t) - DAY_START_MIN * MIN) / 86_400_000);
  const minOf = (t: number) => Math.floor((local(t) % 86_400_000) / MIN);
  const today = dayKey(now);

  const byDay = new Map<number, Signal[]>();
  for (const s of signals.filter(s => allowed(s, consent) && s.t <= now).sort((a, b) => a.t - b.t)) {
    const k = dayKey(s.t); if (!byDay.has(k)) byDay.set(k, []); byDay.get(k)!.push(s);
  }
  const pastDays = [...byDay.keys()].filter(k => k < today);
  const r: Rhythm = { gun_sayisi: pastDays.length, bugun: {} };
  if (pastDays.length < 3) return r;          // 3 günden az veriyle ritim söylenmez

  const perDay = (fn: (ss: Signal[]) => number | undefined) =>
    pastDays.map(k => fn(byDay.get(k)!)).filter((x): x is number => x !== undefined);

  const wake = median(perDay(ss => { const s = ss.find(s => s.type === "screen_on"); return s && dayMinute(minOf(s.t)); }));
  const sleep = median(perDay(ss => { const s = [...ss].reverse().find(s => s.type === "screen_off"); return s && dayMinute(minOf(s.t)); }));
  const charge = median(perDay(ss => { const s = ss.find(s => s.type === "charge_on" && dayMinute(minOf(s.t)) >= 16 * 60); return s && dayMinute(minOf(s.t)); }));
  if (wake !== undefined) r.uyanma = fmt(wake);
  if (sleep !== undefined) r.yatma = fmt(sleep);
  if (charge !== undefined) r.gece_sarj = fmt(charge);

  // Yürüyüş: günün en çok adım atılan saati (en az 1000 adım).
  const walkHours = perDay(ss => {
    const byHour = new Map<number, number>();
    ss.filter(s => s.type === "steps").forEach(s => { const h = Math.floor(minOf(s.t) / 60); byHour.set(h, (byHour.get(h) ?? 0) + (s.value ?? 0)); });
    const best = [...byHour].sort((a, b) => b[1] - a[1])[0];
    return best && best[1] >= 1000 ? dayMinute(best[0] * 60) : undefined;
  });
  if (walkHours.length >= 3) r.yuruyus_saati = fmt(median(walkHours)!);
  const totals = perDay(ss => { const st = ss.filter(s => s.type === "steps"); return st.length ? st.reduce((a, s) => a + (s.value ?? 0), 0) : undefined; });
  if (totals.length) r.ortalama_adim = Math.round(totals.reduce((a, b) => a + b, 0) / totals.length / 100) * 100;

  // Kulaklık: günlerin en az yarısında takılı olduğu saatler.
  const hpCount = new Map<number, number>();
  for (const k of pastDays) {
    // Bir saat, içinde en az 20 dakika kulaklık takılıysa sayılır.
    const mins = new Map<number, number>(); let on: number | null = null;
    for (const s of byDay.get(k)!) {
      if (s.type === "headphone_on") on = s.t;
      if (s.type === "headphone_off" && on !== null) { for (let t = on; t < s.t; t += 5 * MIN) { const h = Math.floor(minOf(t) / 60); mins.set(h, (mins.get(h) ?? 0) + 5); } on = null; }
    }
    [...mins].filter(([, m]) => m >= 20).forEach(([h]) => hpCount.set(h, (hpCount.get(h) ?? 0) + 1));
  }
  const hp = [...hpCount].filter(([, n]) => n >= Math.max(3, pastDays.length / 2)).map(([h]) => h);
  if (hp.length) r.kulaklik_saatleri = ranges(hp);

  // Yoğun saatler: saatte ortalama 4+ kilit açma, en fazla 3 saat.
  const unlocks = new Map<number, number>();
  pastDays.forEach(k => byDay.get(k)!.filter(s => s.type === "screen_on").forEach(s => { const h = Math.floor(minOf(s.t) / 60); unlocks.set(h, (unlocks.get(h) ?? 0) + 1); }));
  const busy = [...unlocks].filter(([, n]) => n / pastDays.length >= 4).sort((a, b) => b[1] - a[1]).slice(0, 3).map(([h]) => h);
  if (busy.length) r.yogun_saatler = ranges(busy);

  // Bugün: her zamankinden farkı
  const todays = byDay.get(today) ?? [], nowDm = dayMinute(minOf(now)), nowH = Math.floor(minOf(now) / 60);
  const firstOn = todays.find(s => s.type === "screen_on");
  if (firstOn && wake !== undefined) r.bugun!.uyanma_farki_dk = dayMinute(minOf(firstOn.t)) - Math.round(wake);
  const lastHp = [...todays].reverse().find(s => s.type.startsWith("headphone"));
  if (consent.sarj_kulaklik) r.bugun!.kulaklik_takili = lastHp?.type === "headphone_on";
  if (walkHours.length >= 3) r.bugun!.yuruyuse_kalan_dk = Math.round(median(walkHours)!) - nowDm;
  if (busy.length) r.bugun!.yogun_saat = busy.includes(nowH);
  return r;
}
