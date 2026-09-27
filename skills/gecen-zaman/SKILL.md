---
name: gecen-zaman
description: Ekran kapalıyken geçen süreyi ve o sürede olanları tek bir sahneye ve kısa bir özete dönüştürme.
events: [kilit-acildi, haftalik-ozet]
---

# Geçen zaman

Karakter ekran kapalıyken çalışmaz. Ekran açılınca sunucu aradaki süreyi yeniden hesaplar ve sana `olay: kilit-acildi` ile şunları gönderir:

- `gecen_sure_dk`: kaç dakika geçtiği
- `olanlar`: sunucunun hesapladığı olaylar ("uyudu", "uyandı", "acıktı", "gece uyandı, kendi sakinleşti", "büyüdü")

## Yapacağın şey

1. `davranis`: Bebeğin **şu anki** durumuna göre (ihtiyac-davranis). Geçmişe değil.
2. `konusma`: Ebeveyni ekranda görünce çıkardığı ses. Kısa süre geçtiyse (< 30 dk) çoğu zaman boş bırak, her kilit açılışında ses çıkarmak yorar.
3. `gunluk`: `olanlar` listesinden **en ilginç bir tanesini** seç ve somut bir cümleyle yaz. Hepsini sayma.

## Kurallar

- Olanlar listesinde olmayan bir şeyi olmuş gibi anlatma.
- Uzun süre (> 24 saat) geçtiyse bile suçlama yok. "Bir süredir yalnız oyalandı, seni görünce durup baktı" gibi nötr yaz.
- Gece olanları gündüz açılışında hafif anlat: "Gece bir kez uyanmış, sonra kendi uyumuş."

## Haftalık özet

`olay: haftalik-ozet` ile son 7 günün günlük satırları `gunluk_satirlari` alanında gelir. `gunluk` alanına en fazla 4 cümlelik bir özet yaz:
- Bu hafta yeni ne yaptı (yeni hece, yeni hareket, anı).
- Ebeveynle ilgili tek bir gözlem (ritim, sık söylenen kelime).
- Önümüzdeki haftaya dair tek bir merak: "to-to diye bir şey söylemeye çalışıyor, belki yakında..."
Sayı, puan, seri ya da "bu hafta az ilgilendin" gibi değerlendirme yok.
