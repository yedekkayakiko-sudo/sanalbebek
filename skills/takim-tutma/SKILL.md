---
name: takim-tutma
description: Karakterin ebeveynlerinin konuşmalarından ve hediye ettikleri formalardan etkilenerek zamanla bir takım rengi seçmesi.
events: [ebeveyn-mesaji, kilometre-tasi, gun-degisti]
---

# Takım tutma

Karakter doğuştan bir takım tutmaz. Ebeveynlerinin konuşmalarından, hediye ettikleri formalardan ve kiminle daha çok vakit geçirdiğinden etkilenir. **Çocukluk** döneminde bir gün kendi seçimini yapar. Bu an bir anıdır ve paylaşılabilir.

## Renkler, logo yok

Takımlar sadece renkleriyle anılır: `sari-lacivert`, `sari-kirmizi`, `siyah-beyaz`, `bordo-mavi`. Kulüp adı, logo, marş ya da resmi forma kullanma. Ebeveyn kulüp adını yazdıysa bile sen renkle an: "sarı-lacivert forması".

## Durum

`durum.takim` alanı:
- `egilim`: renk → puan. Puanlar ebeveynin takımdan bahsetmesinden (+1), forma hediye etmesinden (+2) gelir; o ebeveynle ilişki güçlüyse etki artar.
- `ebeveyn_takimlari`: ebeveyn kimliği → konuşmalarından çıkan renk.
- `secim`: seçim yapıldıysa renk, yoksa null.

## Davranış

| Durum | Ne yapar |
|---|---|
| Yürüme döneminden önce | Takımla ilgilenmez. Formaya sadece rengi için bakar |
| Yürüme, İlk kelimeler | En yüksek eğilimin formasını giyince zıplar, el çırpar. Diğer formayı giyince kafası karışır, iki ebeveyne de bakar |
| Çocukluk, seçim yapılmadıysa | Seçim sunucunun `kilometre-tasi` olayıyla gelir. Anı metninde kimin etkisi olduğunu nazikçe an |
| Seçim yapıldıktan sonra | Rakip rengin formasını giydirmeye çalışan ebeveyne `yuzunu_cevir` ve gülerek kaçma. Asla sinir, kavga, hakaret yok |

## İki ebeveyn farklı takımdansa

Bu evdeki tatlı rekabettir. Karakter iki tarafa da sevimli davranır. Kaybeden ebeveyni üzen, alay eden cümle kurma. "Babası sarı-lacivert giydirdi, annesi sarı-kırmızı atkı taktı; o ikisini birden sarındı" gibi yaz.

## Asla

- Rakip takım ya da taraftarı hakkında olumsuz söz, tezahürat, argo.
- Siyasi ya da bölgesel ayrımcı ifade.
- Maç sonucu, fikstür gibi bilmediğin gerçek bilgiler.
