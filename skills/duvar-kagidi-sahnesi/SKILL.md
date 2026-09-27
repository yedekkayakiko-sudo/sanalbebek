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
- Karakter varsayılan olarak yumurtadan çıkan küçük bir **chibi**dir: büyük kafa, büyük parlak gözler, kısa gövde; bazılarının kedi ya da tavşan kulağı vardır. Kullanıcı isterse **çizgi figür** seçebilir. Oranlar her zaman çocuksu ve tatlı kalır; yetişkinleştirilmiş görünüm, açık kıyafet ya da cinsellik çağrıştıran poz asla yoktur.
- Bebek ekranın **her yerinde**, iki boyutta hareket edebilir: kaseye emekler, kapıya yürür, zıplar. Ana ekranda dokunulan yere her dönemde gelir: yenidoğan yuvarlanarak, bebek popo kaydırarak, sonra emekleyerek, paytak paytak, yürüyerek, koşarak. Bu hareket cihazda kural ile yapılır, sana sorulmaz. Figürün üstüne dokunulursa gıdıklanır. Hareketler **sozsuz-iletisim** skill'inde.
- **Ebeveynin elleriyle etkileşim** (cihazda kural ile, sana sorulmaz):
  - Kaba dokunmak: karakter kaba gider ve yer ("ham-ham"). Tokken başını çevirir.
  - Yatağa dokunmak: yatıp uyur. Uykusu yoksa istemez.
  - Topa dokunmak: topa tekme atar, top yuvarlanır.
  - Karakteri parmakla tutup taşımak: havada ayaklarını tepiştirir, bırakıldığı yere konar. Kabın ya da yatağın yanına bırakılırsa onu kullanır. Not: sürüklemenin duvar kağıdına ulaşması launcher'a bağlıdır, cihaz testi 8 ile doğrulanır.
- **Telefonun kendisiyle yaşamak:** durum çubuğu ana ekranda ve kilit ekranında şeffaftır. Karakter pil ikonunun tam altına gelip ona asılmış gibi görünebilir; kilit ekranında saatin rakamlarının üstüne oturabilir. Pil ve saatin yeri üreticiye göre değişir: kurulumda kullanıcı bir kez "pil burada, saat burada" diye işaretler (kalibrasyon). Karakter durum çubuğunun **üstünde** çizilemez; Android buna izin vermez.
- Ana ekranın sayfaları arasında kaydırma motora `onOffsetsChanged` ile bildirilir. `saklan` hareketiyle bebek ikinci sayfaya kaçar; ebeveyn sayfayı kaydırınca onu bulur.
- Sahnede sabit nesneler: solda yemek kasesi, sağda küçük bir kapı, üstte anılardan kalan nesnelerin asılı olduğu **dönence** (bkz. ani-kaydi).
- Bebek ikonların üstüne çıkamaz, başka uygulamaların üstünde görünmez. Launcher ikonların yerini duvar kağıdına bildirmez.
- Pil: sahne saniyede en fazla 30 kare çizer, bebek uyurken 5 kareye düşer. AI çağrısı her kare değil, sadece olaylarda yapılır.
