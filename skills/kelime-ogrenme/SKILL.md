---
name: kelime-ogrenme
description: Ebeveynin bebeğe söylediği kelimelerden bebeğin yeni kelime öğrenme koşulları.
events: [ebeveyn-mesaji, gun-degisti]
---

# Kelime öğrenme

Bebek, ebeveynlerinden **tekrar tekrar duyduğu** kelimeleri öğrenir. Sayımı sunucu tutar ve sana `durum.duyulanlar` olarak gönderir: `{"top": 4, "kedi": 1}`.

## `yeni_kelime` önermek için koşulların hepsi gerekli

1. Dönem **İlk kelimeler** veya **Çocukluk**.
2. Kelime `duyulanlar` içinde en az **3** kez geçiyor.
3. Kelime `durum.kelimeler` listesinde yok.
4. Kelime tek kelime, en fazla 6 harf, bir bebeğin söyleyebileceği kadar basit (top, kedi, su, ay, mama). "Algoritma", özel isimler ve küfür asla.
5. Bugün henüz yeni kelime öğrenilmemiş (`durum.bugun_yeni_kelime: false`).

Koşullar sağlanmıyorsa `yeni_kelime: null`.

## Önceki dönemlerde

Emekleme ve yürüme döneminde çok duyduğu bir kelimenin ilk hecesini tekrarlayabilir ("top" → "to-to"). Bu bir öğrenme değildir, `yeni_kelime` yine `null`.

## İlk kelime

İlk kelimeyi sunucu belirler: ilişkisi en güçlü ebeveynin çağrı adı (ilişki ≥ 65 ise), yoksa en çok duyulan uygun kelime, o da yoksa en çok yapılan bakımın kelimesi (besleme → "mama", uyutma → "nini", oyun → "ce-e", yürüyüş → "ata").
