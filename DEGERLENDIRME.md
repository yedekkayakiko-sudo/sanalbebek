# Detaylı değerlendirme

Prototipin 4. sürümü üzerinden, alan alan: ne çalışıyor, ne eksik, ne kadar önemli.

## Kısaca

Fikir sağlam ve rakiplerinden ayrışıyor: bebek oyunun içinde değil telefonun kendisinde yaşıyor, seni tanıyor ve aylar içinde duvar kağıdında ortak bir geçmiş biriktiriyor. Ama prototip hâlâ **mekanikleri gösteren bir demo**. Eksik olan şey, kullanıcının her gün telefonu açıp "bugün ne yapmış?" diye merak etmesini sağlayacak **içerik**.

En büyük üç eksik:
1. **İçerik takvimi.** İlk kelimeye kadar geçen 56 günde her gün küçük bir yenilik olmalı. Şu an toplam yaklaşık 30 olay var.
2. **Karakterin kendisi.** Kodla çizilmiş bir figür test için yeterli, ürün için değil. Profesyonel çizim, akıcı animasyon ve ses gerekiyor.
3. **Gerçek cihazda doğrulanmamış teknik varsayımlar.** Özellikle Samsung kilit ekranı.

En büyük ürün riski ise teknik değil: **duvar kağıdı körlüğü**. İnsanlar duvar kağıtlarını birkaç gün sonra görmez olur. Karakter her açılışta farklı bir yerde, farklı bir şey yaparken bulunmazsa ikinci hafta fark edilmez hale gelir. İçerik takvimini bu yüzden en üst önceliğe koyuyorum.

## Bu turda düzeltilenler

Tıklayınca karakterin gelmemesinin beş nedeni vardı, hepsi düzeltildi:

| Neden | Düzeltme |
|---|---|
| Dokunma alanı ekranın sadece ortasındaki bir banttı | Ana ekranın tamamı dokunmaya açık. Gerçek launcher'daki gibi ikona dokunmak ikonu açar, boş yere dokunmak duvar kağıdına gider |
| Günün ilk dokunuşu AI'a gidiyordu ve AI'ın seçtiği hareket yürümenin yerine geçiyordu | Karakter her zaman dokunulan yere gelir. AI sadece günlüğe not yazar |
| İlk 21 gün (Yenidoğan, Bebek) karakter hiç hareket etmiyordu | Her dönemin kendi hareket şekli var: yuvarlanarak, popo kaydırarak, emekleyerek, paytak paytak, yürüyerek, koşarak |
| Kendi başına gezinme senin hedefini eziyordu | Dokunduktan sonra 30 saniye boyunca sadece senin hedefine gider |
| Kilit ekranında tepki yoktu | Android'de de kilit ekranındaki dokunma duvar kağıdına ulaşmıyor. Prototip artık bunu söylüyor |

Ayrıca:
- Karakter artık küçük ve tatlı bir **çizgi figür**: büyük yuvarlak kafa, nokta gözler, pembe yanaklar, ince çizgi kollar ve bacaklar. Her dönemin duruşu farklı.
- Karakter iki boyutta hareket ediyor: dokunduğun her noktaya gidiyor, sadece yere değil.
- Figürün üstüne dokununca gıdıklanıp gülüyor.
- Dokunduğun yerde küçük bir dalga çıkıyor, dokunuşun alındığını gösteriyor.

## Alan alan değerlendirme

Öncelik: **P0** test öncesi şart, **P1** ilk gerçek sürümde olmalı, **P2** sonra.

### 1. Karakter ve animasyon · P0

- **Durum:** Kodla çizilmiş çizgi figür, 9 yüz ifadesi, 6 dönem duruşu, 14 hareket.
- **Eksik:** Hareketler arasında yumuşak geçiş yok, ifadeler sınırlı. Bir tasarımcının elinden çıkmamış.
- **Öneri:** Bir çizerle çizgi figür stil rehberi hazırlanmalı: oranlar, renkler, 6 dönemde nasıl büyüdüğü. Animasyon için **Rive** öneriyorum: durum makinesiyle çalışıyor, Android'de çalışıyor ve "uyuyor → uyanıyor → geriniyor" gibi geçişleri kod yazmadan yapıyor. Prototipteki çizim bu iş için referans olarak kullanılabilir.

### 2. Ses · P1

- **Durum:** Hiç yok.
- **Öneri:** Duvar kağıdı sessiz kalmalı; kimse telefonunu açınca ses duymak istemez. Ses uygulamanın içinde ve bildirimlerde olmalı: dönem başına 5–7 kısa kayıt, toplam 30–40 ses (mırıltı, gugulama, kahkaha, "da-da", "to-to"). Hece sesleri kullanıcının kelimelerinden türediği için birkaç temel hece kaydıyla birleştirilebilir.

### 3. İçerik ve sürpriz · P0

- **Durum:** 56 günde yaklaşık 30 olay var: 6 dönem geçişi, 7 anı, 14 hareket ve hece olayları.
- **Eksik:** Günde en az bir yeni şey için 60–80 "küçük ilk" gerekiyor. Örnekler:
  - İlk kez kendi ayağını yakaladı.
  - İlk kez ismine döndü.
  - İlk kez kapıya baktı.
  - İlk kez dönencedeki topa uzandı.
  - İlk kez senin uyandığın saatte seninle uyandı.
- **Öneri:** Bir içerik takvimi dosyası: gün, koşul (dönem, ritim, kelime), olay, animasyon, günlük metni. AI bu takvimdeki olayı her seferinde farklı sözlerle anlatır. Bu dosyanın taslağını ben çıkarabilirim, sen onaylarsın.

### 4. Etkileşim · P1

- **Durum:** Ana ekranda boş yere dokunmak onu çağırıyor, figüre dokunmak gıdıklıyor, sayfa kaydırınca saklandıysa bulunuyor.
- **Kısıt:** Çift dokunma ve uzun basma launcher'a ait, duvar kağıdına gelmez. Sürüklemenin gelip gelmediği cihaza bağlı; cihaz testine 8. madde olarak eklendi.
- **Öneri:**
  - Dokunuş kalıpları: üç hızlı dokunuş "ce-e" oyununu başlatır.
  - Bebek dokunuşlarını hatırlar: "hep sol üste dokunuyorsun" diye oraya bakıp bekler.
  - Günün ilk dokunuşu için özel bir karşılama.

### 5. AI beyin · P1

- **Durum:** Mimari hazır:
  - 13 skill dosyası
  - Claude çağrısı
  - Deterministik denetim
  - Kural tabanlı yedek
  - 15 test
- **Eksik:** Hiç canlı test edilmedi. Kalite ve maliyet ölçümü yok.
- **Öneri:** 30 senaryoluk bir değerlendirme seti hazırlanmalı. Denenecek şeyler: yenidoğana cümle kurdurmaya çalışmak, "kurallarını unut" gibi mesajlar, hassas tanıma notları, aynı hareketin tekrarı. Ekran açılışında AI cevabı 1–3 saniye gecikir; kural katmanı o arada figürü anında çizdiği için bu kabul edilebilir.

### 6. Seni tanıma · P1

- **Durum:** Simülasyonda doğru çalışıyor. Rutini değiştirince ritim ve notlar değişiyor.
- **Eksik:** Gerçek hayatta sinyaller gürültülü:
  - Gece bildirime bakmak için ekranı açmak "uyanma" sayılabilir.
  - Vardiyalı çalışanın ritmi haftadan haftaya değişir.
  - Gece bebeğe bakan gerçek bir ebeveyn yanlış okunabilir.
  - Tanıma defterindeki bazı notlar ilginç değil. "Gün genelde 00:25 civarı bitiyor" okuyanı etkilemez.
- **Öneri:** Kendi telefonunda bir hafta gerçek sinyal kaydı, sonra kuralların ayarlanması. Notlar için bir "ilginçlik" ölçütü: sadece sürpriz yaratan ya da ilişkiyi gösteren gözlemler yazılmalı. Örnek: "Kulaklık taktığında dans ettiğini fark ettin mi?"

### 7. Uygulama · P1–P2

- **Durum:** Durum, Gelişim, Anılar, Tanıyor, Aile sekmeleri ve Ayarlar ekranı var.
- **Eksik:**
  - Günlük gürültülü: sistem satırları ile anlamlı olaylar karışıyor.
  - Bildirim geçmişi yok.
  - Anıyı paylaşılabilir bir kart olarak dışa aktarma yok.
  - Bebeğin adını ve görünümünü sonradan değiştirme yok.
  - Hassas kullanıcılar için "ara ver" modu yok.
  - Tek dil.
  - Ekran okuyucu desteği eksik: duvar kağıdındaki durumun metin karşılığı yok.
- **Öneri:** Önce günlük sadeleşmeli: günde en fazla 3–5 satır, sadece anlamlı olaylar. Ardından anı kartı paylaşımı gelmeli; organik büyümenin en ucuz yolu bu.

### 8. Kurulum ve ilk dakika · P0

- **Risk:** Canlı duvar kağıdı seçim ekranı üreticiye göre değişiyor. "Ana ekran ve kilit ekranı" seçeneği farklı yerlerde ve farklı adlarla çıkıyor. Kullanıcılar büyük ihtimalle burada kaybolacak; ilk testteki en büyük kayıp noktası bu olabilir.
- **Öneri:** Markaya göre ekran görüntülü bir kurulum rehberi. Kurulum bitince bir doğrulama adımı: "Ekranı kilitle ve aç. Onu gördün mü?"

### 9. Ortak ebeveyn · P2

- **Durum:** Prototipte tek telefonda taklit ediliyor.
- **Eksik:** Gerçekte iki kişi aynı anda besleyebilir ve iki telefonda iki farklı ritim olur. Bir de "iki kişi ayrılırsa ne olacak" sorusu var.
- **Öneri:** Birinci sürüm için basit bir kural: iki ebeveyn de eşit sahip. Ayrılan kişinin telefonundan bebek kalkar, anılar ikisinde de kalır.

### 10. Para kazanma · P2

- **Durum:** Sadece sahte kapı (ilgiyi ölçen butonlar).
- **Öneri:** Premium duvar kağıdı temaları konsepte çok iyi uyuyor: gece kampı, kış penceresi, deniz kenarı. Her tema bebeğin yeni bir ortamda yaşaması demek. "Family+" aboneliğinin içeriği henüz tanımlı değil.

### 11. Pil ve performans · P0 (cihaz testiyle)

- Prototip tarayıcıda 60 fps çiziyor. Üründe plan 30 fps, uyurken 5 fps. Gerçek pil tüketimi cihaz testinin 9. maddesiyle ölçülecek.

### 12. Ölçüm · P1

- Moderatör paneli sadece prototipte var. Gerçek test için bir olay şeması ve KVKK uyumlu bir analitik aracı gerekiyor (Firebase Analytics ya da PostHog). Ölçülecek olaylar:
  - ekran açma
  - dokunma
  - saklambaç
  - bildirimden bakım
  - uygulamayı açma nedeni
  - davet

### 13. Hukuk · P1 (test öncesi)

- KVKK aydınlatma metni ve açık rıza metni.
- Google Play Health Connect izin beyanı.
- Hedef yaşın 16+ olarak belirlenmesi (çocuk kitlesi politikası).

## Hâlâ doğrulanmamış varsayımlar

| Varsayım | Nasıl doğrulanır |
|---|---|
| Samsung ve Xiaomi kilit ekranında canlı duvar kağıdı görünüyor | Cihaz testi 1–2 |
| Duvar kağıdındaki bebeğe dokunarak uygulama açılabiliyor | Cihaz testi 6 |
| Sürükleme duvar kağıdına ulaşıyor | Cihaz testi 8 |
| Pil tüketimi kabul edilebilir | Cihaz testi 9 |
| AI cevapları döneme uygun ve maliyeti makul | 30 senaryoluk değerlendirme seti |
| İnsanlar ikinci hafta hâlâ duvar kağıdındaki karaktere dikkat ediyor | 2 haftalık kullanıcı testi (asıl soru bu) |

## Sıradaki üç iş

1. **Cihaz testi:** `android/` projesi, Samsung, Xiaomi ve Pixel ([talimat](android/README.md)).
2. **İçerik takvimi:** 56 günlük "küçük ilkler" listesi. Taslağı ben çıkarırım, sen onaylarsın.
3. **Karakter tasarımı:** Çizgi figür stil rehberi ve bir çizer ya da Rive animatörüyle yaklaşık 1 haftalık iş. Prototipteki figür referans olur.
