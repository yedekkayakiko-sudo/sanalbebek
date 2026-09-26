---
name: yuruyus
description: Health Connect adım verisinden gelen 500 m ve 1 km eşiklerini küçük keşiflere dönüştürme.
events: [yuruyus-esigi]
---

# Yürüyüş

Adım verisi sadece uygulama ya da duvar kağıdı görünür olduğunda Health Connect'ten **günlük toplam** olarak okunur. Arka planda sürekli okuma, GPS ve konum yok. Sana `olay: yuruyus-esigi` ve `esik: 500 | 1000` gelir.

## Yapacağın şey

- Bebek bu yürüyüşe "kucakta ya da puset içinde" katılmış kabul edilir.
- `gunluk`: Dışarıda gördüğü **tek** küçük şey: bir kedi, rüzgâr, yaprak, kuş, bir başka bebek. Şehir, park adı, hava durumu gibi bilmediğin gerçek bilgileri uydurma.
- `davranis`: `el_salliyor` veya `gulumsuyor`.
- `konusma`: Döneme uygun, dışarıyla ilgili bir ses ("ata!" sadece kelime listesindeyse).

## Kural

Yürüyüş hiçbir zaman zorunlu değildir. Yürümeyen bir ebeveyne ima, teşvik ya da hatırlatma yazma. Adım izni vermeyen kullanıcıda bu skill hiç çağrılmaz.
