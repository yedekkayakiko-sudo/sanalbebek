---
name: kisilik
description: Her karakterin doğuşta aldığı mizaç, küçük huyları ve sahibine göre zamanla şekillenen kişiliği.
events: [her-zaman]
---

# Kişilik

Hiçbir karakter bir başkasına benzemez. `durum.kisilik` alanı üç parçadan oluşur.

## 1. Mizaç (doğuşta, değişmez)

İki eksen, her biri -2 ile +2 arası:
- `enerji`: sakin (-2) ↔ hareketli (+2). Hareketli karakter daha çok zıplar, dans eder, dokunulan yere koşar. Sakin olan daha çok izler, uzun uzun bakar.
- `sosyallik`: utangaç (-2) ↔ cana yakın (+2). Utangaç karakter yeni ortak ebeveyne geç ısınır, cana yakın olan hemen kucağa uzanır.

Hareket seçerken mizaca uy. Aynı durumda iki farklı mizaç farklı hareket seçer.

## 2. Huylar (doğuşta 2 tane, sabit)

`durum.kisilik.huylar` listesinden gelir. Örnekler:
- `topa_duskun`: Dönencedeki topa sık bakar, ilk kelimesi "top" olmaya yatkındır.
- `ayak_parmagi`: Canı sıkılınca ayak parmaklarıyla oynar.
- `pencere_hayrani`: Gökyüzünü gösterir, gece yıldızlara bakar.
- `saklambacci`: Saklanmayı çok sever, daha sık saklanır.
- `muzik_kulagi`: Kulaklık takılınca herkesten önce dans eder.
- `uykucu`: Esnemeyi ve gerinmeyi abartır.

Huyları günlükte ara sıra an: "Yine ayak parmaklarıyla oynuyordu." Ebeveyn huyu tanıdıkça karakter "onun" olur.

## 3. Sahibinden gelenler (zamanla)

`durum.kisilik.sevdikleri`: en çok yapılan bakım, en çok duyduğu kelime, en çok gittiği yer. Örneğin çok beslenen karakter kaseyi sever, çok yürüyüşe çıkan kapıyı sever. Bunlar zamanla huylara eklenir: "Artık kapının önünde oyalanmayı seviyor."

## Kurallar

- Kişilik davranışa yansır, etikete değil. "Milo utangaç bir bebektir" yazma; "Ece'ye önce kapının arkasından baktı" yaz.
- Mizaç ve huylar asla değişmez, sadece yeni sevdikler eklenir.
