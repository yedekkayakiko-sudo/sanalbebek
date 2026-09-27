import { GESTURES, POSES, TARGETS, type BabyState, type BrainOutput, type Pose } from "./types.ts";
import { isProfane } from "./style.ts";

// Modelin çıktısı ne olursa olsun, bebeğin dönemine aykırı konuşma ve hareket ekrana çıkmaz.
// Skill'ler modeli yönlendirir; bu dosya kuralları deterministik olarak uygular.

const BASE_SYLLABLES = ["ma", "da", "ba", "ta", "na", "dı", "bu", "ga"];
const CHILD_EXTRA = ["daha", "yok", "ver"];
const FALLBACK_SOUND = ["hıı", "agu", "ma-ma", "ta-ta", "", ""];
const VOWELS = "aeıioöuü";

/** "top" → "to", "kedi" → "ke", "araba" → "a". Türkçe için ilk (ünsüz+)ünlü. */
export function firstSyllable(word: string): string | null {
  const w = word.toLocaleLowerCase("tr-TR");
  const m = w.match(new RegExp(`^[^${VOWELS}]{0,2}[${VOWELS}]`, "u"));
  return m && /^[a-zçğıöşü]+$/u.test(m[0]) ? m[0] : null;
}

/** En çok duyulan kelimelerin ilk heceleri (en az 2 kez duyulmuş). */
export function heardSyllables(heard: Record<string, number>, limit = 4): string[] {
  const out: string[] = [];
  for (const [w] of Object.entries(heard).filter(([, n]) => n >= 2).sort((a, b) => b[1] - a[1])) {
    const s = firstSyllable(w);
    if (s && !out.includes(s) && !BASE_SYLLABLES.includes(s)) out.push(s);
    if (out.length >= limit) break;
  }
  return out;
}

function babbleOk(s: string, allowed: string[], maxPerGroup: number): boolean {
  const body = s.replace(/[!?]$/, "");
  const groups = body.split(/[\s-]+/).filter(Boolean);
  if (!groups.length || groups.length > 4) return false;
  return groups.every(g => {
    // Grup, izin verilen hecelerin 1..maxPerGroup kez art arda gelmesidir ("bada" = "ba"+"da").
    const re = new RegExp(`^(?:${allowed.map(a => a.replace(/[-]/g, "")).join("|")}){1,${maxPerGroup}}$`, "u");
    return re.test(g);
  });
}

export function isAllowedSpeech(state: BabyState, speech: string): boolean {
  const s = speech.trim().toLocaleLowerCase("tr-TR");
  if (s === "") return true;
  if (s.length > 20 || state.uyuyor) return false;
  // "bip": küfrü duymuş karakter onu asla açıkça söyleyemez, sadece "bip" çıkar (konusma-uslubu)
  if (/^(bi-)?bip!?$/.test(s)) return state.donem >= 2 && (state.uslup?.bip_sayisi ?? 0) >= 3;
  if (s.split(/[\s!?.,-]+/).some(w => w && isProfane(w))) return false;
  const syll = [...BASE_SYLLABLES, ...state.heceler];
  switch (state.donem) {
    case 0: return /^(\(minik sesler\)|h+ı+|ı+h+|ah+)$/u.test(s);
    case 1: return /^(agu+|ağğ+|a+|ga|(hı)(-hı){0,3}|ı+)[!?]?$/u.test(s);
    case 2: return babbleOk(s, syll, 1);
    case 3: return babbleOk(s, [...syll, "mam", "ah"], 2);
  }
  const vocab = state.kelimeler.map(w => w.toLocaleLowerCase("tr-TR"));
  const words = s.replace(/[!?.,]/g, "").split(/\s+/).filter(Boolean);
  if (state.donem === 4) return words.length === 1 && (vocab.includes(words[0]) || babbleOk(words[0], syll, 2));
  return words.length <= 2 && words.every(w => vocab.includes(w) || CHILD_EXTRA.includes(w));
}

export function isAllowedGesture(state: BabyState, gesture: string): boolean {
  const [name, target] = gesture.split(":");
  const min = GESTURES[name];
  if (min === undefined || state.donem < min) return false;
  if (name === "git" || name === "isaret") return (TARGETS as readonly string[]).includes(target ?? "");
  return target === undefined;
}

export function isAllowedNewWord(state: BabyState, word: string | null): word is string {
  if (!word) return false;
  const w = word.trim().toLocaleLowerCase("tr-TR");
  return state.donem >= 4
    && !state.bugun_yeni_kelime
    && /^[a-zçğıöşü-]{1,6}$/u.test(w)
    && !isProfane(w)
    && (state.duyulanlar[w] ?? 0) >= 3
    && !state.kelimeler.includes(w);
}

// Tanıma defterine asla girmemesi gereken konular (ebeveyni-tanima skill'i).
const NOTE_BLOCK = /sağlık|hasta|uyku ?bozuk|uykusuz|stres|depres|kaygı|ilaç|teşhis|konum|adres|evde|evden|evine|işyer|iş yer|ofis|(^|\s)din(\s|$)|namaz|siyas|seçim|sevgili|boşan|(^|\s)para|maaş|borç|çok geç|az yürü|fazla kullan|bağımlı/iu;

export function isAllowedNote(state: BabyState, note: string | null): note is string {
  if (!note) return false;
  const n = note.trim();
  return !!state.ritim && state.ritim.gun_sayisi >= 3
    && n.length > 0 && n.length <= 100
    && !NOTE_BLOCK.test(n)
    && !state.defter.some(d => d.toLocaleLowerCase("tr-TR") === n.toLocaleLowerCase("tr-TR"));
}

// Modelin çıktısı hiç gelmezse (ağ yok, ret, zaman aşımı) kullanılan kural tabanlı davranış.
export function rulePose(state: BabyState): Pose {
  const rel = state.ebeveynler.find(p => p.id === state.bakan)?.iliski ?? 50;
  if (state.gece_aglama) return "agliyor";
  if (state.uyuyor) return "uyuyor";
  if (state.aclik >= 70) return "mutfaga_bakiyor";
  if (state.enerji <= 25) return "esniyor";
  if (state.ilgi <= 25) return "kollarini_uzatiyor";
  if (rel < 25) return "cekingen";
  return "oynuyor";
}

/** Ritim ve ihtiyaçlardan kural tabanlı hareket. AI yokken de bebek ebeveynini tanır. */
export function ruleGesture(state: BabyState, event?: string): string {
  const d = state.donem, can = (g: string) => isAllowedGesture(state, g);
  const r = state.ritim?.bugun;
  if (state.uyuyor || state.gece_aglama) return "yok";
  if (event === "kilit-acildi" && r?.uyanma_farki_dk !== undefined && Math.abs(r.uyanma_farki_dk) < 45) return "gerin";
  if (r?.kulaklik_takili && can("dans")) return "dans";
  if (r?.yuruyuse_kalan_dk !== undefined && r.yuruyuse_kalan_dk >= 0 && r.yuruyuse_kalan_dk <= 30) return can("kapida_bekle") ? "kapida_bekle" : can("isaret:kapi") ? "isaret:kapi" : "goz_temasi";
  if (state.aclik >= 70) return can("isaret:kase") ? "isaret:kase" : "uzan";
  if (state.ilgi <= 25) return can("uzan") ? "uzan" : "goz_temasi";
  if (r?.yogun_saat) return "yok";
  const idle = ["goz_temasi", "kahkaha", "el_cirp", "ce_e", "git:donence", "isaret:pencere", "zipla", "saklan"].filter(can)
    .filter(g => !state.son_hareketler.includes(g));
  return idle[(d * 7 + state.son_hareketler.length) % Math.max(1, idle.length)] ?? "goz_temasi";
}

export type Sanitized = BrainOutput & { duzeltildi: string[] };

export function sanitize(state: BabyState, raw: Partial<BrainOutput> | null, maxDiary = 280): Sanitized {
  const fixed: string[] = [];
  const out: BrainOutput = {
    konusma: typeof raw?.konusma === "string" ? raw.konusma.trim() : "",
    davranis: (POSES as readonly string[]).includes(raw?.davranis as string) ? (raw!.davranis as Pose) : rulePose(state),
    hareket: typeof raw?.hareket === "string" ? raw.hareket.trim() : "yok",
    gunluk: typeof raw?.gunluk === "string" ? raw.gunluk.trim().slice(0, maxDiary) : "",
    yeni_kelime: raw?.yeni_kelime ?? null,
    defter_notu: raw?.defter_notu ?? null,
  };
  if (raw?.davranis !== out.davranis) fixed.push("davranis");
  // Uyku ve gece ağlaması simülasyonun gerçeğidir; model bunları değiştiremez.
  if (state.uyuyor && out.davranis !== "uyuyor") { out.davranis = "uyuyor"; fixed.push("davranis:uyku"); }
  if (state.gece_aglama && out.davranis !== "agliyor") { out.davranis = "agliyor"; fixed.push("davranis:aglama"); }
  if ((state.uyuyor || state.gece_aglama) && out.hareket !== "yok") { out.hareket = "yok"; fixed.push("hareket:uyku"); }
  else if (!isAllowedGesture(state, out.hareket)) { out.hareket = state.donem >= 0 ? "goz_temasi" : "yok"; fixed.push("hareket"); }
  if (!isAllowedSpeech(state, out.konusma)) { out.konusma = state.uyuyor ? "" : FALLBACK_SOUND[state.donem] ?? ""; fixed.push("konusma"); }
  if (out.yeni_kelime !== null && !isAllowedNewWord(state, out.yeni_kelime)) { out.yeni_kelime = null; fixed.push("yeni_kelime"); }
  else if (out.yeni_kelime) out.yeni_kelime = out.yeni_kelime.trim().toLocaleLowerCase("tr-TR");
  if (out.defter_notu !== null && !isAllowedNote(state, out.defter_notu)) { out.defter_notu = null; fixed.push("defter_notu"); }
  else if (out.defter_notu) out.defter_notu = out.defter_notu.trim();
  return { ...out, duzeltildi: fixed };
}
