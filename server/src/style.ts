// Ebeveynin mesajlarından üslup ve takım eğilimi çıkarır. Deterministik, AI yok.
// Küfürler saklanmaz: sadece sayılır. Beyne ne kelimenin kendisi ne de mesajın tamamı gider.

const lower = (s: string) => s.toLocaleLowerCase("tr-TR");
const words = (s: string) => lower(s).match(/[a-zçğıöşü]+/gu) ?? [];

// Kök eşleşmesi: "siktir", "sikeyim" gibi çekimleri de yakalar. Hakaretler de "bip" sayılır.
const PROFANE_ROOTS = ["amk", "aq", "amına", "amina", "sik", "orospu", "piç", "pic", "yarrak", "yarak", "göt", "got", "kahpe", "pezevenk", "ibne", "gerizekal", "salak", "aptal", "şerefsiz", "serefsiz"];
const PROFANE_EXACT = new Set(["mal", "oç", "oc", "bok"]);
const SAFE_PREFIX = ["sıkı", "sikke", "gotik", "malzeme", "malı", "mala"];   // "sik" kökünün yanlış yakalayacağı masum kelimeler
export function isProfane(word: string): boolean {
  const w = lower(word);
  if (SAFE_PREFIX.some(p => w.startsWith(p))) return false;
  return PROFANE_EXACT.has(w) || PROFANE_ROOTS.some(r => w.startsWith(r));
}

const FILLERS = ["abi", "abla", "yaa", "ya", "valla", "vallahi", "hadi", "aynen", "tamam", "oha", "lan", "kanka", "canım", "tatlım", "aşkım", "bebeğim", "haydi", "hayda", "eyvah", "oley", "yuppi", "yok", "evet", "hayır"];

export interface Style { laflar: string[]; gulme: string | null; ton: "coskulu" | "sakin" | "sorucu"; bip_sayisi: number }

export function styleProfile(messages: string[]): Style {
  const counts = new Map<string, number>();
  let bip = 0, excl = 0, quest = 0, laughs: Record<string, number> = { haha: 0, sjsj: 0, ahah: 0, hehe: 0 };
  for (const m of messages) {
    if (m.includes("!")) excl++;
    if (m.includes("?")) quest++;
    for (const w of words(m)) {
      if (isProfane(w)) { bip++; continue; }
      if (/^(ha){2,}h?$/.test(w)) laughs.haha++;
      else if (/^(sj){2,}/.test(w) || /^[sjdk]{4,}$/.test(w)) laughs.sjsj++;
      else if (/^a(ha){2,}/.test(w)) laughs.ahah++;
      else if (/^(he){2,}/.test(w)) laughs.hehe++;
      else if (FILLERS.includes(w)) counts.set(w, (counts.get(w) ?? 0) + 1);
    }
  }
  const laflar = [...counts].filter(([, n]) => n >= 3).sort((a, b) => b[1] - a[1]).slice(0, 5).map(([w]) => w);
  const topLaugh = Object.entries(laughs).sort((a, b) => b[1] - a[1])[0];
  const n = Math.max(1, messages.length);
  const ton = excl / n >= 0.4 ? "coskulu" : quest / n >= 0.4 ? "sorucu" : "sakin";
  return { laflar, gulme: topLaugh[1] >= 2 ? topLaugh[0] : null, ton, bip_sayisi: bip };
}

// Takımlar sadece renkleriyle: kulüp adı, logo ve resmi forma lisans ister.
export const TEAMS = {
  "sari-lacivert": ["fener", "fenerbahçe", "fenerbahce", "fb", "kanarya", "kadıköy"],
  "sari-kirmizi": ["galatasaray", "cimbom", "gs", "aslan", "cimbombom"],
  "siyah-beyaz": ["beşiktaş", "besiktas", "bjk", "kartal", "karakartal"],
  "bordo-mavi": ["trabzon", "trabzonspor", "ts", "fırtına", "firtina"],
} as const;
export type TeamColor = keyof typeof TEAMS;

export function detectTeam(text: string): TeamColor | null {
  const ws = words(text);
  for (const [color, keys] of Object.entries(TEAMS) as [TeamColor, readonly string[]][]) {
    if (ws.some(w => keys.some(k => w === k || (k.length >= 5 && w.startsWith(k))))) return color;
  }
  return null;
}

export interface TeamInput { parentId: string; rel: number; text?: string; gift?: TeamColor }
export interface TeamState { egilim: Partial<Record<TeamColor, number>>; ebeveyn_takimlari: Record<string, TeamColor>; secim: TeamColor | null }

/** Bahsetme +1, forma hediyesi +2; ilişkisi güçlü ebeveynin etkisi en fazla 1,5 kat. */
export function teamAffinity(inputs: TeamInput[], secim: TeamColor | null = null): TeamState {
  const egilim: Partial<Record<TeamColor, number>> = {};
  const perParent: Record<string, Partial<Record<TeamColor, number>>> = {};
  for (const i of inputs) {
    const color = i.gift ?? (i.text ? detectTeam(i.text) : null);
    if (!color) continue;
    const w = (i.gift ? 2 : 1) * (0.5 + Math.min(100, Math.max(0, i.rel)) / 100);
    egilim[color] = +(((egilim[color] ?? 0) + w).toFixed(2));
    (perParent[i.parentId] ??= {})[color] = (perParent[i.parentId][color] ?? 0) + 1;
  }
  const ebeveyn_takimlari: Record<string, TeamColor> = {};
  for (const [p, m] of Object.entries(perParent)) ebeveyn_takimlari[p] = (Object.entries(m) as [TeamColor, number][]).sort((a, b) => b[1] - a[1])[0][0];
  return { egilim, ebeveyn_takimlari, secim };
}

/** Çocukluk döneminde, en az 3 puanlık bir eğilim varsa seçim yapılır. */
export function chooseTeam(t: TeamState, donem: number): TeamColor | null {
  if (t.secim || donem < 5) return t.secim;
  const best = (Object.entries(t.egilim) as [TeamColor, number][]).sort((a, b) => b[1] - a[1])[0];
  return best && best[1] >= 3 ? best[0] : null;
}
