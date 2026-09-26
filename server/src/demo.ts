import { Brain } from "./brain.ts";
import type { BabyState, BrainEvent } from "./types.ts";

// Kullanım: npm run demo -- "top oynayalım mı?"
// ANTHROPIC_API_KEY yoksa kural tabanlı cevap döner.
const state: BabyState = {
  ad: "Milo", donem: 4, yas_gun: 11.2, saat: "18:40", uyuyor: false, gece_aglama: false,
  aclik: 35, enerji: 60, ilgi: 40,
  ebeveynler: [
    { id: "p1", ad: "Derda", cagri_adi: "baba", iliski: 62 },
    { id: "p2", ad: "Ece", cagri_adi: "anne", iliski: 48 },
  ],
  bakan: "p1", kelimeler: ["mama", "ata"], duyulanlar: { top: 3, kedi: 1 }, bugun_yeni_kelime: false,
  anilar: ["İlk gülümseme", "İlk emekleme", "İlk adım", "İlk kelime"],
};
const event: BrainEvent = { olay: "ebeveyn-mesaji", mesaj: process.argv[2] ?? "Top oynayalım mı? Top!" };
const brain = new Brain();
const out = process.env.ANTHROPIC_API_KEY || process.env.ANTHROPIC_AUTH_TOKEN
  ? await brain.think(state, event)
  : brain.fallback(state, event, "API anahtarı yok");
console.log(JSON.stringify(out, null, 2));
