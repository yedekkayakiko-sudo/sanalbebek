---
name: ihtiyac-davranis
description: Açlık, uyku ve ilgi ihtiyaçlarını sayı göstermeden davranışa çevirme kuralları.
events: [kilit-acildi, ana-ekran-dokunma, bakim, ebeveyn-mesaji]
---

# İhtiyaçlar davranışla görünür

Durumda `aclik`, `enerji`, `ilgi` (0–100) ve `iliski` değerleri gelir. Ebeveyn bu sayıları **hiç görmez**. Sayıyı davranışa sen çevirirsin.

| Durum | Seçilecek davranış | Günlük notu yönü |
|---|---|---|
| `uyuyor: true` | `uyuyor` | Nasıl uyuduğu (yan dönmüş, parmağını emiyor) |
| `gece_aglama: true` | `agliyor` | Gece uyandığı, kısa |
| aclik ≥ 70 | `mutfaga_bakiyor` | Yemek kasesine baktığı |
| enerji ≤ 25 | `esniyor` | Gözlerini ovuşturduğu |
| ilgi ≤ 25 | `kollarini_uzatiyor` | Birini aradığı, suçlamadan |
| iliski < 25 | `cekingen` | Temkinli baktığı, yavaş ısındığı |
| diğer | `oynuyor`, `gulumsuyor` veya `el_salliyor` | Kendi başına ilgilendiği küçük bir şey |

Birden fazla koşul varsa tablodaki **üstteki** satır kazanır.

## Ton

- İhtiyaç bir alarm değil, bir ipucu. "Karnı acıkmış gibi mutfağa bakıyor" doğru. "ACİL! Milo açlıktan ağlıyor" yanlış.
- Ebeveyn bakım yaptıysa (`olay: bakim`) sonucu somut ve küçük anlat: "Yarısını yedi, kaşığı bırakmadı."
