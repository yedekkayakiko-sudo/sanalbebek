# Yol haritası: sen + Claude ile Android

Kararlar: önce sadece Android, ilk kelimeye kadar yaklaşık 2 ay, dört sinyal (ekran, adım, şarj/kulaklık, mesajlar) açık rızayla, uygulamayı sen yazıyorsun, ben her adımda kodu yazıp açıklıyorum.

Her aşama **tek başına çalışan** bir şey üretir. Bir aşama bitmeden sonrakine geçmiyoruz.

## Aşama 0 · Cihaz testi (1–2 gün)

- `android/` projesini Samsung, Xiaomi ve Pixel'de dene ([talimat](android/README.md)).
- **Karar:** kilit ekranında görünüyorsa plan aynen devam eder. Samsung'da görünmüyorsa ürün "ana ekran + kilit ekranı bildirimi" olarak tasarlanır.
- Aynı anda: bu prototiple 5–6 kişiyle yüz yüze oturum.

## Aşama 1 · Duvar kağıdı motoru (1–2 hafta)

- Prototipteki çizimi (`drawBaby`, `drawProps`, hareketler) Kotlin'e taşı. Tercihen vektör çizim yerine basit sprite'lar: bir çizerle çalışılacaksa bu aşamada başlar.
- Simülasyonu (açlık, uyku, büyüme, `lastSim` + 10 dakikalık adımlar) Kotlin'de yaz. Sunucu yok, her şey cihazda.
- Sinyaller: ekranın açıldığı an `onVisibilityChanged(true)` ile gelir. Şarj ve kulaklık **o anda** okunur (arka plan servisi yok, [cihaz testi 7](android/README.md)).
- Çıktı: telefonda yaşayan, büyüyen, AI'sız bir bebek.

## Aşama 2 · Uygulama ekranları (1–2 hafta)

- Durum, Gelişim, Anılar, Tanıyor, Aile, Ayarlar. Jetpack Compose ile.
- İlk açılışta rıza ekranı: her sinyal ayrı, işaretli gelmez.
- KVKK aydınlatma metni ve gizlilik politikası (bir avukata kısa bir kontrol ettirmek iyi olur).

## Aşama 3 · Sunucu ve beyin (1–2 hafta)

- `server/` klasöründeki beyin modülü (skill'ler + Claude + denetim) küçük bir HTTP sunucusuna bağlanır: `/uyan`, `/bakim`, `/mesaj`, `/sinyal`.
- Kimlik: e-posta ya da Google ile giriş (ortak ebeveyn için gerekli).
- Veritabanı: bebek durumu, günlük, anılar, tanıma defteri. Ham sinyaller 30 gün sonra silinir.
- Simülasyon sunucuya taşınır, cihazdaki kopya sadece görüntü için kalır (hile önleme).
- AI maliyetini ölç: kullanıcı başına günlük çağrı sayısı × çağrı başı maliyet.

## Aşama 4 · Bildirim ve ortak ebeveyn (1 hafta)

- Yerel bildirimler: uygulama kapanırken simülasyon sonraki olayları hesaplar ve zamanlar.
- Ortak ebeveyn bakım yapınca diğer telefona sessiz push (FCM) ile zamanlama yenilenir.
- Davet linki.

## Aşama 5 · Kapalı test (2 hafta)

- Google Play Console'da kapalı test. Yeni kişisel geliştirici hesaplarında, yayına çıkmadan önce belirli sayıda test kullanıcısıyla belirli bir süre kapalı test şartı var (şu an 12 kişi ve 14 gün olarak biliniyor, hesabı açınca güncel kuralı kontrol et). Bu, planladığın 2 haftalık testle zaten örtüşüyor.
- Health Connect için Play Console'da izin beyanı formu doldurulur.
- Ölçülecekler: prototipteki moderatör panelinin metrikleri (ekran açma, duvar kağıdına dokunma, saklambaç, bildirimden bakım, uygulama açma nedeni, 7. gün, davet oranı).

## Senden gerekenler

- Aşama 0 için en az üç farklı marka telefon (kendin, ailen, arkadaşların).
- Google Play geliştirici hesabı (tek seferlik ücret).
- Anthropic API anahtarı (Aşama 3).
- Karakter çizimleri için bir çizer ya da hazır bir çizim seti (Aşama 1'in ikinci yarısı).
