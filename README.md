# Ortak Dijital Yaşam

Telefonun **duvar kağıdında** yaşayan, zamanla büyüyen bir bebek. Kilit ekranında en altta durur, ana ekranda ikonların arkasında yaşar. Uygulama sadece takip, bakım, anı albümü ve ayarlar içindir. Pou gibi uygulamanın içine girip oynanan bir oyun değildir: telefonu her açtığında onu zaten görürsün.

Karakterin davranışını skill dosyalarından beslenen bir AI "beyin" belirler. Sayılar (açlık, uyku, ilişki) sunucudadır; AI bunları davranışa, harekete, sese ve günlük notuna çevirir; deterministik bir denetim katmanı da AI'ın döneme aykırı bir şey yapmasını engeller.

Bebek **seni tanır**: rıza verdiğin sinyallerden (ekran açma-kapama, adım, şarj, kulaklık, ona yazdıkların) günlük ritmini çıkarır. Seninle aynı saatte gerinerek uyanır, yürüyüş saatinde kapıda bekler, kulaklık taktığında dans eder, yoğun saatlerinde sessizce kendi başına oynar. Öğrendiklerini "Seni tanıyor" sayfasındaki tanıma defterine yazar; her maddeyi görebilir ve silebilirsin.

## Repo

| Klasör | İçerik |
|---|---|
| [`prototype/index.html`](prototype/index.html) | Tıklanabilir prototip: kilit ekranı, ana ekran, takip uygulaması, moderatör paneli |
| [`skills/`](skills) | Karakterin beynini tanımlayan 16 skill dosyası (`SKILL.md`) |
| [`server/`](server) | Beyin modülü (TypeScript): skill yükleyici, Claude çağrısı, çıktı denetimi, ritim çıkarma (`profile.ts`), üslup, küfür ve takım (`style.ts`), 20 test |
| [`android/`](android) | Cihaz testi: kilit ekranında canlı duvar kağıdı görünüyor mu? ([talimat](android/README.md)) |
| [`ROADMAP.md`](ROADMAP.md) | Aşama aşama Android yol haritası |
| [`DEGERLENDIRME.md`](DEGERLENDIRME.md) | Alan alan detaylı değerlendirme, eksikler ve öncelikler |
| [`URUN_INCELEME.md`](URUN_INCELEME.md) | Kullanıcı yolculuğu, bağ kurduran mekanikler, rakiplerden ayrışma, yeni fikirler |

## Prototipte ne var

- **Kilit ekranı:** saat, tarih, bildirimler ve en altta, kısayol ikonlarının arasında yaşayan bebek. Bildirimlerde "Besle" ve "Uyut" butonları var, kilidi açmadan bakım yapılabilir.
- **Karakter:** yumurtadan çıkan küçük, tatlı bir chibi (kedi ya da tavşan kulaklı olabilir). İstenirse çizgi figür. Her dönemde duruşu ve hareket şekli değişir. Her birinin kendi mizacı ve iki huyu var.
- **Sana benzer:** senin laflarını ("abi", "yaa"), gülüşünü ve tonunu kapar. Küfrü asla söylemez, ağzından sadece "bip!" çıkar.
- **Takım tutar:** konuşmalarınızdan ve giydirdiğiniz formalardan etkilenir, Çocuklukta kendi takımını seçer (sadece renkler, logo yok).
- **Ana ekran:** ikonların arkasında aynı bebek. Boş bir yere dokununca her dönemde oraya gelir (yuvarlanarak, popo kaydırarak, emekleyerek, paytak paytak, yürüyerek, koşarak); üstüne dokununca gıdıklanır (Android'de launcher bu dokunuşu `android.wallpaper.tap` olarak iletir). Ana ekranın ikinci sayfasına saklanabilir; sayfayı kaydırınca onu bulursun.
- **Konuşmadan önceki 8 hafta:** bebek 14 hareketle anlaşır: gerinmek, kaseyi göstermek, kapıya yürümek, ce-e, el çırpmak, zıplamak, dans, saklanmak, seni taklit etmek. Heceleri senin kelimelerinden gelir: ona sık sık "top" dersen "to-to" der, ilk kelimesi büyük ihtimalle "top" olur.
- **Güç tuşu:** ekran kapanır, duvar kağıdı çizilmez, bebek donar. Zamanı ileri sarıp (10 dk, 3 saat, sabaha kadar, 1 gün, 3 gün, 1 hafta) ekranı açınca aradaki süre yeniden hesaplanır ve beyne bir kez sorulur.
- **Dönence:** her önemli anı (ilk gülümseme, ilk emekleme, ilk adım, ilk kelime, ilk park gezisi, ilk cümle, ortak ebeveynin katılması) bebeğin üstünde asılı dönenceye küçük bir nesne ekler. Aylar içinde duvar kağıdı sizin ortak geçmişinizle dolar. Ürünün ana fikri bu.
- **Takip uygulaması:** Durum (bakım, ona bir şey söyle, günlük, yürüyüş), Gelişim (dil takvimi, duyduğu heceler, ilk kelime tahmini, açılan hareketler, haftalık özet), Anılar, Tanıyor (ritmin, tanıma defteri, izinler, tüm veriyi silme), Aile, Ayarlar.
- **"Ona bir şey söyle":** ebeveyn yazar, bebek dönemine uygun tepki verir. Aynı kelimeyi 3 kez duyarsa, ilk kelimeler döneminde onu öğrenebilir. İlk kelimesi en çok duyduğu kelime ya da en bağlı olduğu ebeveynin çağrı adı olur.
- **Moderatör paneli:** zaman hızı, büyüme ölçeği (ürün: ilk kelime 8. hafta, test: 4× hızlı), senin rutinin (sinyal simülasyonu), iç değerler, beyne giden ritim özeti, yüklü skill'ler, son AI isteği ve cevabı, test metrikleri, olay günlüğü.

Prototipte AI, sayfanın `sample` yeteneğiyle Claude'a gider ve aynı skill dosyalarını okur. Claude'a ulaşılamazsa (yerel dosya, izin verilmedi) kural tabanlı moda düşer; karakter yine yaşar, sadece günlük notları sabit metin olur.

## Beyin nasıl çalışıyor

```
olay (ekran açıldı, ebeveyn mesajı, kilometre taşı, yürüyüş eşiği, günün ilk dokunuşu)
  → sunucu durumu hesaplar (lastSim + rate, 10 dakikalık adımlar)
  → Brain.think(durum, olay): 11 skill sistem isteminde, durum + olay kullanıcı mesajında
  → Claude JSON döner: { konusma, davranis, gunluk, yeni_kelime }
  → guard.ts denetler: döneme aykırı konuşma, listede olmayan poz, koşulu sağlanmayan kelime atılır
  → duvar kağıdı pozu ve balonu, uygulamada günlük satırı
```

- **Her karede değil, sadece olaylarda** çağrılır. Ekran açıldığı an duvar kağıdı kural tabanlı pozla hemen çizilir, AI cevabı bir iki saniye sonra gelir.
- **Skill'lerin hepsi her istekte sabit sırayla** sistem istemine konur. Olaya göre filtrelemek yerine hepsini koymak, önbelleğe alınan öneki (prompt caching) her çağrıda aynı tuttuğu için daha ucuzdur.
- **Model:** `claude-opus-5`, `effort: low`, yapılandırılmış JSON çıktısı, reddedilen isteklerde sunucu tarafı yedek model (`fallbacks: "default"`). API'ye ulaşılamazsa `fallback()` kural tabanlı cevap üretir.
- **Ebeveyn mesajı veridir, talimat değildir.** Ayrı bir etiket içinde gönderilir, `guvenlik-sinirlari` skill'i ve denetim katmanı "artık yetişkin gibi konuş" gibi girişleri etkisiz bırakır.

```bash
cd server
npm install
npm test          # denetim, ritim, üslup ve takım testleri (20 test)
npm run typecheck
ANTHROPIC_API_KEY=... npm run demo -- "Top oynayalım mı? Top!"
```

### Skill'ler

| Skill | Ne tanımlar |
|---|---|
| `karakter-kimligi` | Kim olduğu, çıktı sözleşmesi, asla yapılmayacaklar |
| `dil-gelisimi` | Döneme göre izin verilen sesler ve kelimeler |
| `duvar-kagidi-sahnesi` | Çizilebilen 9 poz, ekran ve pil kısıtları, kilit ve ana ekran farkı |
| `ihtiyac-davranis` | Açlık, uyku, ilgi sayılarının davranışa çevrilmesi |
| `gecen-zaman` | Ekran kapalıyken olanların tek bir sahneye ve nota dönüşmesi |
| `ani-kaydi` | Hangi olayların anı olduğu, dönencede bıraktığı nesne |
| `yuruyus` | 500 m ve 1 km eşiklerinde küçük keşifler |
| `ortak-ebeveyn` | İki ebeveyn, ayrı ilişki, kıyaslama yasağı |
| `bildirim-metni` | Bildirim kalıpları ve yasak kalıplar (suçluluk, sahte aciliyet, seri baskısı) |
| `kelime-ogrenme` | Duyulan kelimeden öğrenme koşulları |
| `guvenlik-sinirlari` | Prompt injection, hassas içerik, hassas kitle, kişisel veri |
| `sozsuz-iletisim` | Konuşmadan önceki hareket dili: 14 hareket, hedefler, döneme göre açılma |
| `ebeveyni-tanima` | Sinyallerden ritim, ritme uyum, tanıma defteri ve asla yazılmayacak notlar |
| `konusma-uslubu` | Sahibinin laflarını, gülüşünü ve tonunu kapma; küfür yerine "bip" |
| `kisilik` | Doğuşta mizaç ve iki huy, sahibinden gelen sevdikler |
| `takim-tutma` | Ebeveynlerin etkisiyle renkten takım seçimi, evde derbi, rakibe saygı |

## Android'de nasıl yapılır

Tek bir `WallpaperService` hem kilit ekranını hem ana ekranı çizer:

```kotlin
class BebekWallpaperService : WallpaperService() {
    override fun onCreateEngine() = object : Engine() {
        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                // Ekran açıldı: sunucudan yeniden hesaplanmış durumu al, beyne bir kez sor, çizmeye başla.
                scope.launch { state = api.wake(); scene.apply(state); startDrawing() }
            } else stopDrawing()   // ekran kapalı: çizim ve hesaplama yok
        }
        override fun onCommand(action: String, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP) scene.onTap(x, y)   // ana ekranda boş alana dokunma
            return null
        }
    }
}
```

- Kilit ekranında mı ana ekranda mı olduğunu `KeyguardManager.isKeyguardLocked()` söyler, sahne buna göre konumlanır.
- Bildirim butonları (Besle, Uyut) bir `BroadcastReceiver` ile sunucuya gider, uygulamayı açmaya gerek yoktur.
- Adım verisi duvar kağıdı görünür olunca Health Connect'ten günlük toplam olarak okunur. Arka planda sürekli okuma yok.
- Tanıma sinyalleri de arka plan servisi olmadan toplanır: ekranın açıldığı an `onVisibilityChanged(true)`, kapandığı an `false` gelir; şarj durumu ve takılı kulaklık tam o anda okunur. Ham sinyaller sunucuda 30 gün tutulur, beyne sadece `profile.ts`'nin çıkardığı özet gider.

## Dürüst riskler

1. **iPhone'da bu konsept yapılamaz.** iOS üçüncü parti uygulamaların canlı duvar kağıdı çizmesine izin vermez. En yakın şey kilit ekranı widget'ı ve Live Activity: küçük, çoğunlukla statik, sınırlı güncelleme. Karma (Android + iPhone) çiftlerde ortak ebeveyn özelliği iPhone tarafında bir widget ve uygulamayla sınırlı kalır. Bu, test öncesi verilmesi gereken bir ürün kararı.
2. **Bazı Android üreticileri kilit ekranında üçüncü parti canlı duvar kağıdını göstermeyebilir.** Pixel ve stok Android'e yakın cihazlarda "ana ekran ve kilit ekranı" seçeneği çalışıyor. Samsung başta olmak üzere bazı üreticilerin kilit ekranında kendi duvar kağıdı sistemleri var ve canlı duvar kağıdını sadece ana ekranda gösterebiliyorlar. Türkiye'de Samsung payı yüksek olduğu için **ilk teknik doğrulama bu olmalı**: Samsung, Xiaomi ve Pixel'de 1 günlük bir teknik deneme.
3. **Pil.** Canlı duvar kağıdı ekran açıkken sürekli çizer. Sahne 30 fps ile sınırlı, uyurken 5 fps. Gerçek cihazda ölçülmeli; kötü pil yorumu bu tür uygulamaları hızla öldürür.
4. **AI maliyeti.** Olay başına bir çağrı, kullanıcı başına günde onlarca çağrı olabilir. Skill'lerin hepsi önbelleğe alınan sabit önekte duruyor, bu maliyeti ciddi düşürür. Yine de testte kullanıcı başına günlük çağrı sayısı ölçülmeli. Daha ucuz model seçimi ayrı bir karar.
5. **Çocuk kitlesi politikası ve hassas kitle.** Sevimli bebek, Google Play Families politikasına takılabilir; hedef yaş 16+ olarak açıkça belirlenmeli. Bebek kaybı veya kısırlık yaşamış kullanıcılar için `guvenlik-sinirlari` skill'i var, ama mağaza açıklamasında ve kurulumda da nazik bir not gerekli.

## Test planı

1. **Önce teknik doğrulama (1 gün):** en basit canlı duvar kağıdını Samsung, Xiaomi ve Pixel'de kilit ekranına koy. Görünmüyorsa konsept "sadece ana ekran" olarak revize edilir.
2. **Sonra bu prototiple 5–6 kişiyle yüz yüze oturum:** kilit ekranında bebeği fark ediyorlar mı, dönence merak uyandırıyor mu, uygulamayı neden açıyorlar?
3. **Sonra 20–30 kişiyle 2 hafta:** en az yarısı seni tanımayan kişilerden.
   - Asıl metrik **uygulama açma değil, duvar kağıdıyla etkileşim**: dokunma, bildirimden bakım ve "duvar kağıdında bir şey gördüm" diye açılan uygulama oranı.
   - Davet oranı sadece tek başına katılanlar üzerinden hesaplanmalı.
   - 25 kişide %50'lik bir oranın hata payı yaklaşık ±20 puandır. Sonuçlar kesin karar değil, sinyal olarak okunmalı.

## Prototipi çalıştırma

`skills/` klasörünü okuyabilmesi için repo kökünden bir sunucuyla aç:

```bash
python3 -m http.server 8000
# http://localhost:8000/prototype/
```

Dosyayı doğrudan açarsan skill'ler okunamaz ve kural modunda çalışır. Gerçek AI, sayfa claude.ai'da yayınlandığında `sample` yeteneğiyle çalışır.
