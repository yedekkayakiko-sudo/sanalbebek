# Ortak Dijital Yaşam — prototip

Telefonda yaşayan, zamanla büyüyen, bir çiftin ortak bakabildiği dijital bebek fikri için **tıklanabilir, zaman simülasyonlu prototip** ve değerlendirme.

- Prototip: [`prototype/index.html`](prototype/index.html). Tarayıcıda aç, kurulum yok. Solda telefon, sağda moderatör paneli var.
- Bu belge: fikrin dürüst değerlendirmesi, teknik mimari notları, test planı.

## Neden Figma değil de bu?

Fikrin asıl mekaniği **"ben yokken ne oldu?"** sorusu. Figma zamanı simüle edemez, geçen süreye göre durumu yeniden hesaplayamaz. Bu yüzden prototip, spesifikasyondaki `lastInteractionAt + rate` modelini birebir uygular:

- Ekran açıkken zaman akar (1×, 60× veya 600× hız).
- "Ekranı kapat" dediğinde hiçbir şey hesaplanmaz. Sonra 10 dk, 3 saat, sabaha kadar, 1 gün, 3 gün veya 1 hafta ileri sarılır.
- Kilidi açınca aradaki süre 10 dakikalık adımlarla yeniden simüle edilir: uyku, acıkma, gece uyanması, büyüme, ilişki kaybı.
- Kilit ekranında o süre boyunca **gerçekten gönderilecek** bildirimler görünür. Sessiz saat, günlük sınır ve gece ayarı uygulanır, bastırılanlar sayılır.

## Prototipte ne var

| MVP maddesi | Prototipteki karşılığı |
|---|---|
| 1. Tek karakter, rastgele görünüm | 5 ten, 5 saç rengi, 3 saç tipi, 4 göz rengi. Başlangıçta "Başka bir bebek" ile yeniden üretilir |
| 2. İhtiyaçlar davranışla gösterilir | Bar yok. Mutfağa bakma + biberon düşünce balonu, göz ovuşturma, kol uzatma, gece ağlama, çekingen bakış. Sayılar sadece moderatör panelinde |
| 3. Doğrusal olmayan büyüme | Yenidoğan 0–2 gün, Bebek 2–4, Emekleme 4–7, Yürüme 7–10, İlk kelimeler 10–13, Çocukluk 13+. Aşamalar büyüdükçe uzuyor. Her aşamanın kendi pozu var |
| 4. Konuşma gelişimi | Önce "hıı", sonra "agu", "ma-ma-ma", "ta-ta". **İlk kelime en çok yaptığın şeye göre belirlenir**: çok beslediysen "mama", çok yürüdüyseniz "ata". Bağ güçlüyse seni çağırdığı kelime ("anne", "baba") olur. Sonra her gün bir kelime ekler, çocuklukta iki kelimelik cümleler kurar |
| 5. Kalıcı anılar | İlk gülümseme, emekleme, adım, kelime, cümle, park gezisi, ortak ebeveyn katılımı. O anın ekran görüntüsü, tarih, yaş ve yanında olan kişi kaydedilir |
| 6. Yürüyüş | Health Connect izin ekranı (sadece adım, konum yok). 500 m ve 1 km eşikleri. İzin vermeyen için hiçbir gelişim kilitlenmez: yürüyüş ilk adımı hızlandırır ama ilk adım yürümeden de gelir |
| 7. Ortak ebeveyn | Davet linki, kabul simülasyonu, ayrı ilişki puanı, "şu an kim bakıyor" geçişi |
| 8. Ölüm yok | İlişki gündüz ihmalde yavaşça düşer, tabanı 5. Günlük kazanım sınırlı (grind yok), ama toparlanma kaybetmekten hızlı |
| 9. Bildirimler | Günde 1–3, arada en az 3 saat, 22:00–08:00 sessiz. Gece uyanma bildirimi **varsayılan kapalı**. Gece yanıt vermemek hiçbir şeyi kötüleştirmez |
| Monetizasyon | Dolap: ücretsiz tulumlar, premium kozmetikler ve "reklam izle" bonusu **sahte kapı** olarak çalışır. Tıklamalar fiyat ilgisi metriği olarak sayılır |
| Test ölçümü | Açılış sayısı, açılan günler, 7. gün kontrolü, iki açılışta bir "Neden açtın?" sorusu (bildirim / merak / bakım / sıkıntı), davet durumu, bildirim iletilen ve bastırılan, JSON dışa aktarma |

Sunucu mantığı dosyada ayrı bir `Server` modülü. İstemci sadece istek gönderir ve kopyayı çizer, gerçek ürünün sınırını taklit eder.

## Dürüst değerlendirme: mantıklı mı?

**Kısa cevap: test etmeye değer, doğrudan geliştirmeye değmez.** Senin prompt'unda da yazdığı gibi asıl risk kod değil, bağlanma. Ama prompt'ta atlanmış ya da hatalı olan birkaç nokta var.

### Güçlü taraflar
- "Stat oyunu değil, ortak geçmiş" iyi bir ayrışma. Tamagotchi ve Pou tarzı oyunlar bakım döngüsünde kalıyor. Kalıcı anı albümü ve ilk kelimenin sizin davranışınıza göre çıkması, üç ay sonra gösterilebilecek bir şey üretiyor.
- Etik kurallar (ölüm yok, suçluluk yok, pay-to-survive yok) doğru ve mağaza incelemesinde de işe yarar.
- Teknik kısıtlar gerçekçi. Özellikle ekranı zorla açmamak doğru bir karar (aşağıya bak).

### Riskler ve düzeltilmesi gerekenler

1. **Çiftlerin yarısı iPhone kullanıyor.** Adım verisi sadece Android Health Connect olursa ortak ebeveynlik özelliği karma çiftlerde yarım kalır. MVP en baştan çapraz platform olmalı (Flutter veya React Native), iOS'ta HealthKit ile. Ya da adım özelliği iOS'ta "yakında" olarak kalır, ama uygulama iki platformda da çıkar.

2. **"Arka planda işlem yok" ile "Milo uyandı" bildirimi çelişiyor.** Telefon uygulamayı çalıştırmıyorsa bebeğin uyandığını kim bilecek? İki yol var, ikisi de gerekli:
   - Simülasyon deterministik olduğu için uygulama kapanırken sonraki olaylar hesaplanır ve **yerel bildirim olarak zamanlanır**.
   - Ortak ebeveyn bebeği besleyince bu tahminler bozulur. Sunucu diğer telefonun zamanlanmış bildirimlerini FCM/APNs sessiz push ile yeniler.
   Aynı simülasyon kodu hem istemcide hem sunucuda çalışmalı (tek TypeScript modülü gibi), yoksa iki taraf farklı sonuç üretir.

3. **Gece ağlaması için sınırlar.** Android 14'ten itibaren tam ekran bildirim izni (`USE_FULL_SCREEN_INTENT`) Google Play'de sadece arama ve alarm uygulamalarına veriliyor. Yani spesifikasyondaki "yüksek öncelikli bildirim + ses + titreşim" zaten yapılabilecek maksimum. iOS'ta "Time Sensitive" bildirim kullanılabilir, "Critical Alert" bu tür bir uygulamaya verilmez.

4. **Çocuk kitlesi politikası.** Sevimli çizgi bebek, Google Play'in Families politikasına ve COPPA'ya takılabilir: uygulama "çocuklara da hitap ediyor" sayılırsa sadece sertifikalı reklam SDK'ları, kişiselleştirilmiş reklam yasağı ve ek inceleme gelir. Hedef yaş 16+ olarak açıkça belirlenmeli ve pazarlama da buna uymalı. Aksi halde "opsiyonel reklam" gelir modeli pratikte çöker.

5. **Hassas kitle.** Sanal bebek; kısırlık, düşük ya da bebek kaybı yaşamış kullanıcılar için tetikleyici olabilir. Mağaza açıklamasında ve onboarding'de nazik bir not, bir de bebeği "uyutup arşivleme" (silmeden ara verme) seçeneği düşünülmeli. Bu bir engel değil ama yorumlarda ilk görülecek eleştiri bu olur.

6. **İçerik tükenmesi.** "Aylar içinde biriken geçmiş" vaadi, aylarca yeni şey gerektirir. MVP'de 7 anı ve 6 aşama var. Çocukluk aşamasına gelen kullanıcıya ne olacağı belli değil. Test bunu gösteremez çünkü 2 hafta içinde kimse içeriği bitiremez. Test sonrası, geliştirmeden önce bir içerik takvimi (hangi ay hangi yeni davranış) yazılmalı.

7. **Rakipler.** Widgetable gibi uygulamalar zaten arkadaş veya partnerle ortak sanal evcil hayvan besletiyor. Finch, suçluluk yaratmayan bakım mekaniğiyle yüksek tutunma gösterdi. Ayrışma "hayvan yerine bebek" olamaz; "zamanla ortak bir hikâye birikiyor" olmalı. Anı albümü bu yüzden merkezde.

### Test planındaki sorunlar

- **20–30 kişi az.** 25 kişide %50'lik bir tutunma oranının güven aralığı yaklaşık ±20 puan. Eşikleri kesin karar gibi değil, güçlü sinyal gibi oku. Görüşmeler (neden açtın, neyi merak ettin) sayılardan daha değerli olacak.
- **Tanıdık yanlılığı.** Arkadaş çevresinden toplanan testçiler nezaketen açmaya devam eder. En az yarısı seni tanımayan kişilerden olmalı.
- **Davet oranı ölçümü bozuk.** Çiftleri baştan birlikte işe alırsan davet oranı yapay olarak yükselir. %20 eşiği sadece **tek başına katılan** kullanıcılar üzerinden hesaplanmalı.
- **Prototip zaman hızı ürünle aynı değil.** Testte 2 haftada tüm aşamalar görülsün diye büyüme sıkıştırılmış (1 gün ≈ ürünün 1–2 ayı). Bu, gerçek ürünün yavaş temposunda merakın sürüp sürmeyeceğini ölçmez. Testten sonra "tempo" ayrı bir soru olarak kalır.
- **Bildirimler web prototipinde gerçek değil.** Bu HTML prototip telefonda açılabilir ama gerçek push göndermez. 2 haftalık saha testi için en ucuz çözüm: bildirimleri moderatörün elle (veya zamanlanmış bir Telegram/WhatsApp mesajıyla) göndermesi. Açılış nedenini ölçmek için bildirimlerin gerçekten gelmesi şart.
- **"Açılış nedeni" her açılışta sorulmamalı.** Prototip iki açılışta bir soruyor ve "Geç" seçeneği var. Her seferinde sorarsan sorunun kendisi davranışı bozar.

### Önerilen sıra

1. Bu prototiple 5–6 kişiyle 30 dakikalık yüz yüze oturum: anlaşılıyor mu, "sen yokken" kartı merak yaratıyor mu, bir anıyı birine göstermek istiyorlar mı?
2. Çıkan sorunları düzelt, sonra 20–30 kişilik 2 haftalık testi yap (prototipi PWA olarak telefona eklet, bildirimleri elle gönder).
3. Başarı kriterini geçerse: çapraz platform, paylaşılan simülasyon modülü, sunucu, yerel + push bildirim mimarisiyle MVP.

## Prototipi çalıştırma

`prototype/index.html` dosyasını tarayıcıda aç. Durum tarayıcıda saklanır; panelden "Sıfırla" ile baştan başlanır. Denemek için:

1. Bebeği başlat, hızı 600× yap, "İlgilen"e bas: ilk gülümseme anısı.
2. "Ekranı kapat" → "Sabaha kadar" → "Kilidi aç": gece ne olduğunu gör. Aynısını Ayarlar'da gece bildirimini açıp tekrarla.
3. "1 hafta" ileri sar: ihmal sonrası çekingen davranışı ve toparlanmayı gör.
4. "Sonraki aşamaya atla" ile emekleme, yürüme ve ilk kelimeye geç. İlk kelimenin neye göre çıktığına bak.
5. Aile sekmesinden davet oluştur, kabul et, iki ebeveyn arasında geçiş yap.
