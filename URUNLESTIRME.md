# Ürünleştirme planı (taslak, tartışmaya açık)

Prototip fikrin çalıştığını gösterdi: telefonda yaşayan karakter, gerçek zamanlı büyüme, Pou tarzı bakım, bebek ya da evcil hayvan seçimi, seni tanıyan sohbet. Bundan sonrası "çalışıyor"dan "mağazada, binlerce telefonda sorunsuz"a geçiş.

## 1. Prototip ile ürün arasındaki fark

| Konu | Prototipte | Üründe olması gereken |
|---|---|---|
| Görseller | Kodla çizilen karakter, emoji eşyalar | Çizerin hazırladığı karakter, animasyonlar, eşyalar, ikon; ses efektleri |
| Kod | Hızlı yazılmış tek parça ekranlar | Düzenli mimari, otomatik testler, hata raporlama |
| Veri | Telefonda basit kayıt | Telefon değişince kaybolmayan bulut yedeği |
| Pil | Ölçülmedi, ana ekranda gereğinden hızlı çiziyor | Ölçülmüş, hedef: günlük pil kullanımında fark edilmeyecek kadar |
| Cihaz uyumu | Tek telefonda denendi | Samsung, Xiaomi, Oppo, Pixel dahil 8–10 cihazda test |
| Reklam | Sahte 5 saniyelik ekran | Gerçek ödüllü reklam (AdMob) |
| Yapay zekâ | Kurallı deneme beyni | Sunucu + gerçek yapay zekâ (ücret kararına bağlı) |
| Yasal | Yok | Şirket, gizlilik politikası, KVKK metni, Play politika beyanları |
| Ölçüm | Yok | Kaç kişi 1., 7., 30. gün geri geliyor, nerede bırakıyor |

## 2. Sürüm 1.0 kapsamı (önerim)

**Girenler:**
- Karakter seçimi: bebek, köpek, kedi (tavşan sonra)
- Gerçek zamanlı büyüme, dönüm noktaları, doğum günleri
- Ana ekranda ve kilit ekranında yaşama (duvar kağıdı + ikonların üstü)
- 5 oda: mutfak, yatak, banyo, revir, oyun
- Altın, market, ödüllü reklam
- 2–3 mini oyun
- Kurallı sohbet ve "seni tanıyor"
- Bildirimler: acıktı, hastalandı, doğum günü (az ve kibar)

**Sonraya kalanlar:**
- Gerçek yapay zekâ: 1.1'de, ücretli seviye olarak
- "Birlikte izle"
- Arkadaşlarla oynama
- Kardeş karakter
- iOS

**Neden:** Her ek özellik lansmanı haftalarca geciktirir. 1.0'ın tek sorusu var: insanlar bu karakterle bağ kurup 30 gün sonra hâlâ bakıyor mu? Cevap evetse gerisine yatırım yapılır.

## 3. İş kalemleri

### A. Marka ve sanat (en kritik)
- Ürün adı ve logo (şu an "Ortak Yaşam Test")
- Çizim tarzı kararı: yumuşak chibi mi, piksel art mı? Karar çizerle birlikte, 2–3 deneme çizimle verilir. Pil sorunu tarzdan bağımsız olarak kodda çözülür.
- Karakter başına animasyon listesi: durma, yürüme, koşma, oturma, uyuma, yeme, gülme, ağlama, asılma, taşınma, dans, hasta, banyo (her biri 4–8 kare)
- Her dönem için boy ve oran çizimi (yenidoğan → çocukluk)
- Eşyalar, odalar, uygulama ikonu, mağaza görselleri
- Kısa ses efektleri

### B. Teknik
- Android'de Kotlin ile devam: duvar kağıdı ve ikon üstü penceresi zaten Android'e özel. iOS ürünü farklı olacağı için (Dynamic Island + widget) ayrı yazılır.
- Uygulama ekranları Jetpack Compose ile yeniden yazılır; bakım ve oyun mantığı ekranlardan ayrılır ve test edilir.
- Firebase: anonim giriş, bulut yedeği, hata raporları (Crashlytics), kullanım ölçümü (Analytics)
- Pil: ana ekranda kare sayısını düşürme, dururken hiç çizmeme, pil ölçüm testi
- Cihaz test listesi ve üreticiye özel ayarlar (Xiaomi otomatik başlatma, Samsung kilit ekranı)

### C. Sunucu
- Bulut yedeği için Firebase yeterli
- Yapay zekâ için küçük bir aracı sunucu: anahtar uygulamada durmaz, kullanıcı başına günlük sınır olur
- Ekonomi hilesine karşı basit kontroller (sonra)

### D. Yasal ve mağaza
- Play Console hesabı. Kişisel hesap ya da şirket hesabı; şirket hesabı için D-U-N-S numarası gerekir.
- Kişisel hesaplarda Google, yayından önce belirli sayıda test kullanıcısıyla (bildiğim kadarıyla 12 kişi, 14 gün) kapalı test istiyor. Güncel kural başvuru sırasında kontrol edilecek.
- Reklam geliri almak için vergi ve ödeme bilgisi; büyük ihtimalle şahıs ya da limited şirket
- Gizlilik politikası ve KVKK aydınlatma metni. Toplanan veriler: ekran açılış saatleri, sohbet (sadece telefonda), fotoğraftan renk.
- Play beyanları: "diğer uygulamaların üzerinde gösterme" izninin gerekçesi, yaş derecelendirmesi anketi, veri güvenliği formu
- **Yaş hedefi kararı:** 13 yaş altı hedeflenirse Google'ın aile politikası devreye girer. Bu durumda reklam türleri kısıtlanır, veri toplama ve yapay zekâ sohbeti çok sıkı kurallara bağlanır. Önerim: 1.0'ı "13+" olarak yayınlamak, içeriği yine de herkese uygun tutmak.

### E. Para
- 1.0: ödüllü reklam (mama/duş/altın hediye). Zorla gösterilen reklam yok.
- 1.1: altın paketleri, kıyafet/forma, kardeş karakter, yapay zekâ aboneliği
- Ekonomi dengesi: reklam izlemeden de oynanabilmeli, ama izlemek anlamlı ödül vermeli

### F. Lansman
- Kapalı test: aile ve arkadaşlar, geri bildirim formu
- Mağaza sayfası: kısa video, bebeğin ana ekranda gezdiği ekran kayıtları
- Tanıtım: TikTok ve Instagram. Ürün doğal olarak videoya uygun: "telefonumda yaşayan bebek pile asıldı".
- Önce Türkiye, Türkçe

### G. Başarı ölçüleri
- 1. gün geri gelme ≥ %40, 7. gün ≥ %20, 30. gün ≥ %10. Bunlar başlangıç hedefi; kapalı testte gerçek değerlere göre güncellenir.
- Günlük etkileşim: kullanıcı başına ≥ 5 dokunuş veya bakım
- Hatasız oturum oranı ≥ %99,5
- Pil: Ayarlar → Pil'de uygulama üst sıralarda görünmemeli

## 4. Aşamalar (süreler ekip zamanına göre değişir)

| Aşama | Ne | Tahmini süre |
|---|---|---|
| 0 · Karar | Ad, yaş hedefi, 1.0 kapsamı, şirket ve Play hesabı, çizer seçimi ve ilk deneme çizimleri | 1–2 hafta |
| 1 · Temel | Kodun ürün mimarisine taşınması, Firebase, pil optimizasyonu, gerçek reklam | 3–5 hafta (çizimle paralel) |
| 2 · Sanat | Çizimlerin ve seslerin uygulamaya girmesi, cilalama | 2–4 hafta |
| 3 · Kapalı test | En az 12 kişi, 14 gün; hata düzeltme; mağaza sayfası | 2–3 hafta |
| 4 · Lansman | Türkiye, Android | — |
| 5 · 1.1 | Gerçek yapay zekâ (ücretli), kardeş karakter, yeni mini oyunlar | Lansmandan sonra |
| 6 · iOS | Dynamic Island + widget + oyun | 1.1'den sonra |
