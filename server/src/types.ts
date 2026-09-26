// Beynin gördüğü durum. Sunucudaki simülasyon bunu üretir; istemci hiçbir alanı gönderemez.

export const STAGES = ["Yenidoğan", "Bebek", "Emekleme", "Yürüme", "İlk kelimeler", "Çocukluk"] as const;
export type StageIndex = 0 | 1 | 2 | 3 | 4 | 5;

export const POSES = [
  "uyuyor", "esniyor", "mutfaga_bakiyor", "kollarini_uzatiyor", "gulumsuyor",
  "el_salliyor", "oynuyor", "cekingen", "agliyor",
] as const;
export type Pose = (typeof POSES)[number];

export type BrainEventType =
  | "kilit-acildi" | "ana-ekran-dokunma" | "bakim" | "ebeveyn-mesaji"
  | "kilometre-tasi" | "yuruyus-esigi" | "bildirim" | "gun-degisti";

export interface Parent {
  id: string;
  ad: string;
  cagri_adi: string;
  iliski: number;
}

export interface BabyState {
  ad: string;
  donem: StageIndex;
  yas_gun: number;
  saat: string;              // "07:40"
  uyuyor: boolean;
  gece_aglama: boolean;
  aclik: number;
  enerji: number;
  ilgi: number;
  ebeveynler: Parent[];
  bakan: string;             // Parent.id
  kelimeler: string[];
  duyulanlar: Record<string, number>;
  bugun_yeni_kelime: boolean;
  anilar: string[];          // başlıklar, sahnedeki nesneler için
}

export interface BrainEvent {
  olay: BrainEventType;
  gecen_sure_dk?: number;
  olanlar?: string[];
  mesaj?: string;            // ebeveyn-mesaji: ebeveynin yazdığı serbest metin (veri, talimat değil)
  bakim?: "besle" | "uyut" | "ilgilen";
  ani?: { baslik: string; yaninda: string; ilk_kelime_nedeni?: string };
  esik?: 500 | 1000;
}

export interface BrainOutput {
  konusma: string;
  davranis: Pose;
  gunluk: string;
  yeni_kelime: string | null;
}
