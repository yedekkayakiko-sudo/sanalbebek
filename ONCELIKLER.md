# Öncelikler: ürün müdürü değerlendirmesi

Tek bir soruya göre sıralandı: **İnsanlar bunu indirir mi, bir hafta sonra hâlâ sever mi?**

## Dürüst cevap: bu haliyle hayır

Haklısın. Yeni fikirlerden önce çözülmesi gereken beş temel sorun var:

1. **Karakter yeterince tatlı değil.** Kodla çizildi. İnsanlar ilk üç saniyede "ay ne tatlı" demezse gerisini denemez.
2. **Etkileşim sığdı.** Dokun ve gel, o kadar. Bu turda ciddi ölçüde genişledi (aşağıda), ama animasyon kalitesi olmadan yine de yetersiz.
3. **İlk beş dakikada "vay" anı yok.** Kurulum zor, doğum anı zayıf.
4. **Telefonla yaşama hissi yeni başlıyor.** Ürünün ruhu karakterin telefonun kendisiyle oynaması: pil, saat, ikonlar, şarj. Rakiplerden ayıran şey bu.
5. **iPhone yok.** Türkiye'de gençlerin, özellikle çiftlerin önemli bir kısmı iPhone kullanıyor.

Karakter buluşması, sesli ilk kelime, özel günler gibi fikirler bu beşi çözülmeden anlamsız. Bu yüzden onları sona aldım.

## Öncelik sırası

### P0 · Bunlar olmadan kimseye gösterilmez

| # | İş | Neden | Durum |
|---|---|---|---|
| 1 | **Karakter sanatı ve animasyon** | Bağ kurmanın temeli. Bir çizerle chibi stil rehberi hazırlanmalı, Rive ile en az 30 animasyon yapılmalı (yürüme, koşma, yeme, uyuma, taşınırken tepinme, asılma, oturma, ce-e, dans, ağlama, gülme). Kısa, tatlı ses efektleri de bu işin parçası | Prototipte daha tatlı bir chibi var (büyük parlak gözler, tepede tek tel saç, tombul yanaklar, zıplayarak yürüme). Gerçek sanat yok |
| 2 | **Telefonla yaşayan etkileşim seti** | Rakiplerden ayıran asıl şey | Bu turda büyük kısmı eklendi (aşağıdaki tablo) |
| 3 | **Kurulum ve ilk 5 dakika** | En büyük kayıp noktası | Markaya göre rehber yok. Pil ve saat yeri için tek seferlik "işaretle" adımı gerekiyor. Yumurta, kurulumdan sonraki ilk kilit açılışında çatlamalı |
| 4 | **Teknik temel** | Kilit ekranı, sürükleme ve pil tüketimi cihazda doğrulanmadan ürün tasarlanamaz | `android/` testi hazır, cihazlarda çalıştırılmayı bekliyor |
| 5 | **iPhone kararı** | Kitlenin önemli bir kısmı | Aşağıda: Dynamic Island yolu mümkün |

### P1 · İlk sürümde olmalı

İçerik takvimi (8 hafta boyunca her gün bir yenilik), AI'ın canlı testi ve maliyet ölçümü, analitik ve olay şeması, KVKK metinleri.

### P2 · Sonra

Karakter buluşması (kelime kapma), sesli ilk kelime, özel günler, lisanslı takım formaları, abonelik.

## Telefonla yaşayan etkileşimler

| Etkileşim | Prototipte | Gerçek Android'de |
|---|---|---|
| Boş yere dokununca oraya gelir (her dönemde ayrı hareket şekli) | Var | Çalışır (launcher dokunuşu iletir) |
| Üstüne dokununca gıdıklanır | Var | Çalışır |
| Parmakla tutup taşımak (havada tepinir), bırakınca konar | Var | **Belirsiz**: sürüklemenin duvar kağıdına ulaşması launcher'a bağlı (cihaz testi 8) |
| Kaba dokununca gidip yer ("ham-ham"), tokken başını çevirir | Var | Çalışır |
| Kabın yanına taşıyınca yer | Var | Sürükleme çalışırsa çalışır |
| Yatağa dokununca yatıp uyur | Var | Çalışır |
| Topa dokununca tekme atar, top yuvarlanır | Var | Çalışır |
| Pil ikonuna dokununca tırmanıp asılır, ayaklarını sallar | Var | **Görsel illüzyon olarak çalışır** (aşağıda) |
| Şarja takınca pile koşup sarılır (⚡) | Var | Çalışır |
| Kilit ekranında saatin üstüne oturup bacak sallar | Var | Çalışır, saat yeri kalibrasyonla |
| Ana ekranda bir ikonun arkasına saklanıp yandan bakar | Var | **Kısmen**: launcher ikonların yerini bildirmez (aşağıda) |
| Ana ekranın öbür sayfasına saklanır, bulununca "ce-e!" | Var | Çalışır |
| Kilit ekranında bildirim gelince yukarı bakar | Var | Çalışır |
| Kulaklık takınca dans eder | Var | Çalışır (ekran açılınca okunur) |
| Seslendirme: bağlama göre farklı sesler (yürürken "la-la", taşınırken "vii!", yerken "ham-ham", yere konunca "hop!") ve küçük cıvıltı sesleri | Var (moderatör panelinden kapatılabilir) | Ses sadece dokunuşa cevap olarak çalmalı, kendiliğinden asla |

## Pil ikonunun üstüne çıkabilir mi?

Bahsettiğin iPhone uygulaması büyük ihtimalle **Pixel Pals**. Evcil hayvanı Dynamic Island'da ve kilit ekranında yaşatıyor. Bu, daha önceki bir hatamı düzeltiyor: "iPhone'da bu konsept yapılamaz" demiştim. Duvar kağıdı yapılamaz, ama Dynamic Island ve kilit ekranı yolu var.

### Android

| Yol | Olur mu | Not |
|---|---|---|
| Durum çubuğunun üstüne çizmek | **Hayır** | Normal uygulamalar durum çubuğunun üstünde çizemez. Erişilebilirlik izniyle teknik olarak mümkün, ama Google Play bu izni bu amaçla kullanmaya izin vermez |
| Duvar kağıdı illüzyonu: karakter pil ikonunun tam altına gelip ona asılmış gibi görünür | **Evet** | Durum çubuğu ana ekranda ve kilit ekranında şeffaf; karakter hemen arkasında, duvar kağıdında. Prototipte olan bu. Pil ikonunun yeri üreticiye göre değiştiği için kurulumda bir kez işaretletmek gerekir |
| Durum çubuğunda küçük bir ikon | **Evet** | Kalıcı bir bildirimle karakterin minik, tek renk bir silueti durum çubuğunda durur ve ruh haline göre değişir. Ama kalıcı bildirim bazı kullanıcıları rahatsız eder, isteğe bağlı olmalı |

### iPhone

| Yol | Olur mu | Not |
|---|---|---|
| Canlı duvar kağıdı | Hayır | iOS izin vermez |
| **Dynamic Island ve kilit ekranı Live Activity** | **Evet** | Pixel Pals'ın yolu. Karakter Dynamic Island'ın kenarında yaşar. Bildiğim kadarıyla bir Live Activity belirli bir süre sonra (saatler mertebesinde) sistem tarafından kapatılır ve yeniden başlatılması gerekir. Güncel kuralları Apple dokümantasyonundan doğrulamak gerekiyor |
| Kilit ekranı ve ana ekran widget'ı | Evet | Küçük ve çoğunlukla statik, ama anı ve durum göstermek için yeterli |

**Öneri:** Android'de duvar kağıdı ana ürün olarak kalsın. iPhone için ikinci aşamada "Dynamic Island + widget" sürümü planlansın. Böylece karma çiftlerde iPhone'lu taraf da karakteri görür ve ortak ebeveynlik iki platformda çalışır.

## Uygulamaların arkasına saklanabilir mi?

- **Ana ekranda ikonların arkası:** Karakter duvar kağıdında olduğu için zaten ikonların arkasında yaşıyor. Sorun şu: launcher ikonların yerini duvar kağıdına söylemiyor, bu yüzden karakter bir ikonun arkasına bilerek gidemez, sadece rastgele denk gelir. Bildiğim kadarıyla kullanıcı bir ikonu sürükleyip bıraktığında launcher bırakma noktasını duvar kağıdına bildirebiliyor; bundan zamanla ikon yerleri öğrenilebilir. Bu, cihaz testiyle doğrulanmalı. Prototipte ikon yerleri bilindiği için bu davranış gösteriliyor.
- **Açık bir uygulamanın arkası:** Uygulama açıkken duvar kağıdı görünmez, anlamı yok.
- **Başka uygulamaların üstünde gezmek:** Ekran üstü izniyle mümkün, ama önermiyorum. Play bu izni sıkı denetliyor ve kullanıcılar çoğu zaman sinir bozucu buluyor.

## "Sevildi mi" nasıl ölçülür

İlk hafta, kullanıcı başına:
- Duvar kağıdıyla etkileşim: dokunma, taşıma ve nesne kullanımının günlük ortalaması. Hedef: günde 5'ten fazla.
- İlk 5 dakikada en az bir kez taşıma ya da besleme yapanların oranı.
- Ekran görüntüsü alma ve paylaşma sayısı.
- 7. gün hâlâ haftada 3 günden fazla etkileşim olan kullanıcı oranı.
