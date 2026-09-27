// Beynin gördüğü durum. Sunucudaki simülasyon bunu üretir; istemci hiçbir alanı gönderemez.

export const STAGES = ["Yenidoğan", "Bebek", "Emekleme", "Yürüme", "İlk kelimeler", "Çocukluk"] as const;
export type StageIndex = 0 | 1 | 2 | 3 | 4 | 5;
/** Her dönemin başladığı gün (gerçek zaman). İlk kelimeye kadar ~8 hafta. */
export const STAGE_START_DAYS = [0, 7, 21, 35, 56, 90] as const;

export const POSES = [
  "uyuyor", "esniyor", "mutfaga_bakiyor", "kollarini_uzatiyor", "gulumsuyor",
  "el_salliyor", "oynuyor", "cekingen", "agliyor",
] as const;
export type Pose = (typeof POSES)[number];

/** Hareket → yapılabildiği en erken dönem. */
export const GESTURES: Record<string, StageIndex> = {
  yok: 0, gerin: 0, goz_temasi: 0, yuzunu_cevir: 0,
  kahkaha: 1, uzan: 1, el_cirp: 1,
  git: 2, isaret: 2, ce_e: 2, saklan: 2,
  zipla: 3, dans: 3, taklit: 3, kapida_bekle: 3,
};
export const TARGETS = ["kase", "kapi", "pencere", "donence", "sen", "dokunulan_yer"] as const;

export type BrainEventType =
  | "kilit-acildi" | "ana-ekran-dokunma" | "bakim" | "ebeveyn-mesaji"
  | "kilometre-tasi" | "yuruyus-esigi" | "bildirim" | "gun-degisti" | "haftalik-ozet";

export interface Parent {
  id: string;
  ad: string;
  cagri_adi: string;
  iliski: number;
}

/** profile.ts'nin sinyallerden çıkardığı ritim. Ham sinyal beyne gitmez. */
export interface Rhythm {
  gun_sayisi: number;
  uyanma?: string;             // "07:30"
  yatma?: string;              // "00:40"
  yuruyus_saati?: string;      // "18:10"
  ortalama_adim?: number;
  gece_sarj?: string;
  kulaklik_saatleri?: string[];  // ["08:00-09:00"]
  yogun_saatler?: string[];      // ["10:00-11:00"]
  bugun?: { uyanma_farki_dk?: number; kulaklik_takili?: boolean; yuruyuse_kalan_dk?: number; yogun_saat?: boolean };
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
  heceler: string[];         // en çok duyulan kelimelerin ilk heceleri
  bugun_yeni_kelime: boolean;
  anilar: string[];
  son_hareketler: string[];
  ritim: Rhythm | null;      // rıza yoksa null
  defter: string[];          // tanıma defterindeki mevcut notlar
}

export interface BrainEvent {
  olay: BrainEventType;
  gecen_sure_dk?: number;
  olanlar?: string[];
  mesaj?: string;            // ebeveyn-mesaji: ebeveynin yazdığı serbest metin (veri, talimat değil)
  bakim?: "besle" | "uyut" | "ilgilen";
  ani?: { baslik: string; yaninda: string; ilk_kelime_nedeni?: string };
  esik?: 500 | 1000;
  gunluk_satirlari?: string[];
}

export interface BrainOutput {
  konusma: string;
  davranis: Pose;
  hareket: string;
  gunluk: string;
  yeni_kelime: string | null;
  defter_notu: string | null;
}
