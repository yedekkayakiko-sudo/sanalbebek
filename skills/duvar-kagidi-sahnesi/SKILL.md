---
name: duvar-kagidi-sahnesi
description: Karakterin yaşadığı duvar kağıdı sahnesi, çizilebilen pozlar ve ekran kısıtları.
events: [her-zaman]
---

# Duvar kağıdı sahnesi

Karakter, Android canlı duvar kağıdı (`WallpaperService`) olarak **kilit ekranında ve ana ekranda** yaşar. Ekranın alt kısmında, saatin ve bildirimlerin altında durur. Uygulama sadece takip, ayarlar ve anı albümü içindir.

## Çizilebilen pozlar (`davranis` alanı)

Sadece bu listeden biri seçilebilir. Listede olmayan bir değer gelirse sahne `oynuyor` pozuna düşer.

| Poz | Görünüm |
|---|---|
| `uyuyor` | Gözler kapalı, "z" harfleri yükselir |
| `esniyor` | Yarı kapalı gözler, eli gözünde |
| `mutfaga_bakiyor` | Gözler yandaki yemek kasesine döner, küçük biberon balonu |
| `kollarini_uzatiyor` | Kollar yukarıda, ekrana bakar |
| `gulumsuyor` | Açık gülümseme |
| `el_salliyor` | Bir kol sallanır |
| `oynuyor` | Yere bakar, küçük hareketler |
| `cekingen` | Gözler aşağı ve yana |
| `agliyor` | Gözler sıkılmış, gözyaşı damlaları |

## Ekran kısıtları

- Konuşma balonu en fazla 20 karakter. Uzun metin sahnede gösterilmez, sadece uygulamadaki günlüğe gider.
- Sahne **ekran kapalıyken çizilmez** (`onVisibilityChanged(false)`). Görünür olunca sunucudan yeni durum alınır ve bir kez sana sorulur.
- Kilit ekranında dokunma duvar kağıdına ulaşmaz, sadece izlenir. Ana ekranda boş alana dokunmak launcher üzerinden `android.wallpaper.tap` komutu olarak gelir. Dokunmaların çoğu cihazda kural ile karşılanır (el sallama, kısa ses); sadece günün ilk dokunuşu `olay: ana-ekran-dokunma` olarak sana iletilir.
- Kilit ekranında sahne en alta, kısayol ikonlarının arasına oturur. Ana ekranda dock ikonlarının üstüne kayar (motor `KeyguardManager.isKeyguardLocked()` ile hangisinde olduğunu bilir).
- Bebek ekranın **her yerinde** hareket edebilir: kaseye emekler, kapıya yürür, dokunulan yere gider, zıplar. Hareketler **sozsuz-iletisim** skill'inde.
- Ana ekranın sayfaları arasında kaydırma motora `onOffsetsChanged` ile bildirilir. `saklan` hareketiyle bebek ikinci sayfaya kaçar; ebeveyn sayfayı kaydırınca onu bulur.
- Sahnede sabit nesneler: solda yemek kasesi, sağda küçük bir kapı, üstte anılardan kalan nesnelerin asılı olduğu **dönence** (bkz. ani-kaydi).
- Bebek ikonların üstüne çıkamaz, başka uygulamaların üstünde görünmez. Launcher ikonların yerini duvar kağıdına bildirmez.
- Pil: sahne saniyede en fazla 30 kare çizer, bebek uyurken 5 kareye düşer. AI çağrısı her kare değil, sadece olaylarda yapılır.
