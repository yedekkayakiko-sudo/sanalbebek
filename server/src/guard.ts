import { POSES, type BabyState, type BrainOutput, type Pose } from "./types.ts";

// Modelin çıktısı ne olursa olsun, bebeğin dönemine aykırı konuşma ekrana çıkmaz.
// Skill'ler modeli yönlendirir; bu dosya kuralları deterministik olarak uygular.

const BABBLE: RegExp[] = [
  /^(\(minik sesler\)|h+ı+|ı+h+)$/u,                                   // 0 Yenidoğan
  /^(agu+|ağğ+|a+|ı+!?|(ba|ga|a)(-(ba|ga|a)){0,3})!?$/u,               // 1 Bebek
  /^((ma|da|ba|dı|ta|na)(-?(ma|da|ba|dı|ta|na)){0,3})[!?]?$/u,        // 2 Emekleme
  /^((ma|da|ba|dı|ta|na|bu|mam|ah)(-?(ma|da|ba|dı|ta|na)){0,3})[!?]?$/u, // 3 Yürüme
];
const CHILD_EXTRA = ["daha", "yok", "ver"];
const FALLBACK_SOUND = ["hıı", "agu", "ma-ma", "ta-ta", "", ""];

export function isAllowedSpeech(state: BabyState, speech: string): boolean {
  const s = speech.trim().toLocaleLowerCase("tr-TR");
  if (s === "") return true;
  if (s.length > 20) return false;
  if (state.uyuyor) return false;
  const vocab = state.kelimeler.map(w => w.toLocaleLowerCase("tr-TR"));
  if (state.donem <= 3) {
    // Yürüme döneminde bilinen kelime yoksa sadece heceler; hece dönemleri kendi kalıbına bağlı.
    return BABBLE[state.donem].test(s);
  }
  const words = s.replace(/[!?.,]/g, "").split(/\s+/).filter(Boolean);
  if (state.donem === 4) return words.length === 1 && (vocab.includes(words[0]) || BABBLE[3].test(words[0]));
  return words.length <= 2 && words.every(w => vocab.includes(w) || CHILD_EXTRA.includes(w));
}

export function isAllowedNewWord(state: BabyState, word: string | null): word is string {
  if (!word) return false;
  const w = word.trim().toLocaleLowerCase("tr-TR");
  return state.donem >= 4
    && !state.bugun_yeni_kelime
    && /^[a-zçğıöşü-]{1,6}$/u.test(w)
    && (state.duyulanlar[w] ?? 0) >= 3
    && !state.kelimeler.includes(w);
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

export function sanitize(state: BabyState, raw: Partial<BrainOutput> | null): BrainOutput & { duzeltildi: string[] } {
  const fixed: string[] = [];
  const out: BrainOutput = {
    konusma: typeof raw?.konusma === "string" ? raw.konusma.trim() : "",
    davranis: (POSES as readonly string[]).includes(raw?.davranis as string) ? (raw!.davranis as Pose) : rulePose(state),
    gunluk: typeof raw?.gunluk === "string" ? raw.gunluk.trim().slice(0, 280) : "",
    yeni_kelime: raw?.yeni_kelime ?? null,
  };
  if (raw?.davranis !== out.davranis) fixed.push("davranis");
  // Uyku ve gece ağlaması simülasyonun gerçeğidir; model bunları değiştiremez.
  if (state.uyuyor && out.davranis !== "uyuyor") { out.davranis = "uyuyor"; fixed.push("davranis:uyku"); }
  if (state.gece_aglama && out.davranis !== "agliyor") { out.davranis = "agliyor"; fixed.push("davranis:aglama"); }
  if (!isAllowedSpeech(state, out.konusma)) {
    out.konusma = state.uyuyor ? "" : FALLBACK_SOUND[state.donem] ?? "";
    fixed.push("konusma");
  }
  if (out.yeni_kelime !== null && !isAllowedNewWord(state, out.yeni_kelime)) { out.yeni_kelime = null; fixed.push("yeni_kelime"); }
  else if (out.yeni_kelime) out.yeni_kelime = out.yeni_kelime.trim().toLocaleLowerCase("tr-TR");
  return { ...out, duzeltildi: fixed };
}
