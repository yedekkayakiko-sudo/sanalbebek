# Ürün incelemesi: kullanıcı gözünden ve ürün yöneticisi gözünden

Bu belge üç soruya cevap veriyor:
1. Kullanıcı uygulamayla tanıştığı ilk dakikadan ikinci aya kadar ne yaşıyor, nerede bırakıp gidiyor?
2. Kullanıcıyı içeri alan, karakterle bağ kurduran şey ne?
3. Bizi rakiplerden ne ayırıyor?

## 1. Senin fikirlerin ve benim görüşüm

| Fikir | Görüşüm | Prototipte |
|---|---|---|
| Karakter sahibini tanıyıp onun gibi konuşsun | **Katılıyorum, ürünün kalbi bu.** Karakter senin dolgu kelimelerini ("abi", "yaa", "valla"), gülüşünü ve tonunu haftalar içinde kapar. Aynı uygulamada iki karakter aynı konuşmaz | Var: `konusma-uslubu` skill'i, "Gelişim → Kimliği → Senden kaptıkları" |
| Bazıları küfür etsin | **Farklı düşünüyorum.** Açık küfür yaş sınırını yükseltir, "küfreden bebek" diye haber olur ve paylaşımı utandırıcı yapar. Onun yerine **"bip" mekaniği**: karakter küfrü duyar, söylemeye çalışır, ağzından sadece "bip!" çıkar ve utanır. Daha komik ve paylaşılabilir | Var: küfür saklanmıyor, sadece sayılıyor. En az 3 küfür duyunca günde en fazla bir "bip" |
| Takım seçsin, anne-baba ikna etmeye çalışsın, forma giydirsin | **Çok iyi, özellikle Türkiye'de.** Evde derbi, ortak ebeveynliği gerçek bir rekabete çevirir ve en güçlü paylaşım anı olur. Tek şart: kulüp adı, logo ve resmi forma lisans ister; ilk sürümde sadece renkler | Var: `takim-tutma` skill'i, konuşmadan takım algılama, forma hediyesi, Çocuklukta seçim ve anı |
| İnsan bebeği yerine tatlı anime figürleri | **Büyük ölçüde katılıyorum.** Chibi "küçük bir varlık": yumurtadan çıkar, büyür, konuşmayı öğrenir ama gerçek insan bebeği değildir. Hassas kitle riski düşer, kitle genişler, oyuncak ve ürün satmaya çok daha uygun olur. **Tek şart:** oranlar her zaman çocuksu ve tatlı kalmalı; yetişkinleştirilmiş anime estetiği hem Google Play politikası hem itibar açısından ciddi risk | Var: varsayılan stil chibi (kedi ve tavşan kulaklı varyasyonlar dahil), yumurtadan çıkış, çizgi figür de seçilebiliyor |

## 2. Kullanıcı yolculuğu: nerede kaybediyoruz?

| An | Kullanıcı ne hissetmeli | Şu an ne oluyor | Eksik |
|---|---|---|---|
| **Keşif** (mağaza, arkadaş) | "Bu ne, benim telefonumda mı yaşayacak?" | Tanıtım yok | 15 saniyelik video: kilit ekranını açıyorsun, karakter seni görüp el sallıyor. Arkadaşından gelen "evde derbi" ya da "bip" ekran görüntüsü |
| **Kurulum** (ilk 60 sn) | "Benim olan bir şey doğuyor" | Form doldurma, sonra duvar kağıdı ayarı | Canlı duvar kağıdı ayar ekranı her markada farklı, **en büyük kayıp noktası**. Markaya göre adım adım rehber ve "kilit ekranını aç, onu gördün mü?" doğrulaması |
| **Doğum** (ilk 5 dk) | Büyülenme | Yumurta çatlıyor, ilk ses geliyor (yeni) | Ona adını yazınca adına dönmeli. İlk dokunuşa kahkahayla tepki vermeli |
| **1. gün** | "Tatlı" | Uyuyor, acıkıyor, ilk gülümseme anısı | Günün sonunda tek cümlelik bir "bugün" özeti bildirimi |
| **2.–3. gün** | "Beni tanıyor!" | Ritim 3. günde oluşuyor, sabah seninle uyanıyor | Bu **sihirli an** fark edilmiyor. İlk kez ritmine uyduğunda bunu açık bir anı olarak göstermeli: "Bu sabah seninle aynı dakikada uyandı" |
| **1. hafta** | Merak | Hareketler, saklambaç, emekleme | Her gün en az bir yeni şey gerekiyor; şu an 56 günde yaklaşık 30 olay var (içerik takvimi) |
| **2.–4. hafta** | Sahiplik | Senin kelimelerinden heceler, laflarını kapma | Duvar kağıdı körlüğü riski burada başlar. Karakter her açılışta farklı bir yerde, farklı bir şey yaparken bulunmalı |
| **8. hafta** | Zirve | İlk kelime: en çok söylediğin kelime | Bu an için özel bir tören: sesli, paylaşılabilir, dönencede kalıcı |
| **3. ay ve sonrası** | Bağlılık | Çocukluk, takım seçimi, iki kelime | Büyük boşluk: çocukluktan sonra ne olacak? Yaş ilerledikçe yeni sistemler gerekir: fikir sahibi olma ("hayır!" dönemi), arkadaşlar, okul |

## 3. Bağ kurduran mekanikler

İnsanlar bir karaktere şu beş şeyle bağlanır. Her birinde neredeyiz?

| Mekanik | Anlamı | Durum |
|---|---|---|
| **Emek** | Kendi emeğini kattığın şeyi daha çok seversin | Kısmen var: bakım yapıyorsun. Ama emek görünür değil. "Bugüne kadar 42 kez besledin" değil, "ilk kelimesini sen öğrettin" gibi sonuçlar görünmeli |
| **Ayna** | Karakter sende bir şey görür ve onu yansıtır | Güçlü: ritmin, kelimelerin, üslubun, takımın |
| **Karşılıklılık** | Karakter de senin için bir şey yapar | Zayıf. Karakter hep ihtiyaç duyuyor, hiç vermiyor. Şunlar eklenmeli: sen geç yatınca o da seninle uyanık kalsın, çok yürüdüğün gün seni alkışlasın, doğum gününü bilsin |
| **Benzersizlik** | Başka kimsede yok | Güçlendi: mizaç, iki huy, görünüm, üslup. Her karakter gerçekten farklı |
| **Ortak hikâye** | Birlikte yaşanmış anlar | Var: dönence, anı albümü, haftalık özet |

## 4. Rakiplerden ayrışma

| | Yaşadığı yer | Seni tanır | Zamanla öğrenir | Birlikte bakım | Kalıcı geçmiş | Yerel kültür |
|---|---|---|---|---|---|---|
| Tamagotchi | Oyuncak / uygulama | Hayır | Sınırlı | Hayır | Sınırlı | Hayır |
| Pou | Uygulamanın içi | Hayır | Hayır | Hayır | Hayır | Hayır |
| My Talking Tom | Uygulamanın içi | Hayır | Hayır (anında tekrar eder) | Hayır | Hayır | Hayır |
| Finch | Uygulamanın içi | Kısmen (hedeflerin) | Evet | Arkadaşlarla | Evet | Hayır |
| Widgetable | Widget | Hayır | Sınırlı | **Evet** (ortak evcil hayvan) | Sınırlı | Hayır |
| **Biz** | **Duvar kağıdı** | **Evet (ritim, üslup)** | **Evet (8 hafta konuşma)** | **Evet + takım rekabeti** | **Evet (dönence)** | **Evet** |

Bu tablo benim genel bilgime dayanıyor. Rakiplerin güncel özellikleri mağazalarda ayrıca kontrol edilmeli.

Tek cümleyle ayrışma: **"Telefonunda yaşayan, seni tanıdıkça sana benzeyen küçük biri."**

## 5. Benim önerdiğim yeni fikirler

Önem sırasına göre:

1. **Karakterler birbirinden kelime kapsın.** İki kullanıcı karakterlerini QR kodla ya da yakınlıkla buluşturur; karakterler birbirinden bir laf kapar. "Arkadaşının karakterinden 'kanka' demeyi öğrendi." Bildiğim kadarıyla rakiplerin hiçbirinde yok ve ürünü kendiliğinden yayar.
2. **Sesli ilk kelime.** Karakter öğrendiği kelimeyi kendi çocuksu sesiyle gerçekten söylesin (çocuk sesli metin okuma). Talking Tom'un keyfi, ama kazanılmış olarak.
3. **Tanıma anını göstermek.** Karakterin seni ilk kez tanıdığı an (sabah seninle uyanması) açık bir anı olmalı. Şu an sessizce oluyor ve fark edilmiyor.
4. **Tatil, ceza değil.** 30 gün açılmazsa karakter "dedesine tatile gider". Döndüğünde bir hediye ve anlatacak bir hikâyeyle gelir. Ölüm ve suçluluk yok, ama geri dönmek için güzel bir sebep var.
5. **Türkiye'ye özel günler.** 23 Nisan'da bayram kıyafeti, yılbaşında atkı, ebeveynin doğum gününde kutlama. Maç günleri, lisanslı fikstür verisi olmadan kullanıcının kendi girdiği maçlarla.
6. **"Hayır!" dönemi.** Çocuklukta karakter bazen istemediği formayı giymez, yemeğini seçer. Kendi iradesi olan bir karakter daha gerçek hissettirir.
7. **Haftalık hikâye kartı.** Instagram hikâyesi boyutunda "bu hafta Umay..." kartı. Organik büyümenin en ucuz yolu.

## 6. Riskler (yeni fikirlerle birlikte)

| Risk | Önlem |
|---|---|
| Pasif takip ürkütücü gelebilir ("beni izliyor") | Dili "seni izliyor" değil "seni tanıyor" olarak kur. Her tanıma bir anı ya da hediye olarak gösterilsin. Tanıma defteri her zaman görünür ve silinebilir olsun |
| Anime karakterin yetişkinleştirilmesi | Tasarım kuralı olarak yazılı: çocuksu oranlar, sade kıyafetler, cinsellik yok. Kullanıcıların yaptığı özelleştirmeler de bu kurala tabi |
| Mevcut bir anime veya karakter markasına benzemek | Özgün tasarım. Referans alınan eserler dokümante edilmeli |
| Takım lisansı | İlk sürümde sadece renkler. Lisanslı forma ileride kulüplerle anlaşmayla premium ürün olabilir |
| Takım rekabetinin çirkinleşmesi | `takim-tutma` skill'i: rakibe olumsuz söz, tezahürat, siyaset yok. Kural denetimle de uygulanmalı |
| Küfür | Karakter asla küfretmez, sadece "bip". Küfrün kendisi sunucuda saklanmaz, sadece sayısı tutulur |
| Duvar kağıdı körlüğü | İçerik takvimi, her açılışta farklı yer ve hareket, haftalık yenilik |

## 7. Bu turda eklenenler

- **Chibi karakter** (varsayılan): yumurtadan çıkış, büyük parlak gözler, perçem, pembe yanaklar, kedi ve tavşan kulaklı varyasyonlar, 6 dönem duruşu. Ayarlar'dan çizgi figüre geçilebiliyor.
- **Kişilik:** her karakterde doğuşta bir mizaç (enerji, sosyallik) ve iki huy (saklambaççı, müzik kulağı, uykucu...). Huyları gezinirken sık görünüyor. Hareketli karakter daha hızlı koşuyor.
- **Üslup kapma:** "abi", "yaa", "valla" gibi laflar, gülüş biçimi ve ton sayılıyor. Emekleme ve yürüme döneminde lafların hecesiyle mırıldanıyor.
- **Bip:** küfürler saklanmıyor, sayılıyor. Karakter arada bir "bip!" deyip utanıyor.
- **Takım:** konuşmalardan takım algılanıyor (renk olarak). Ebeveynler forma giydirebiliyor. Evde derbi gösteriliyor. Çocuklukta seçim bir anı oluyor, dönenceye atkı asılıyor. Seçimden sonra rakip formayı gülerek reddediyor.
- **3 yeni skill** (`konusma-uslubu`, `kisilik`, `takim-tutma`), toplam 16. Sunucu tarafında `style.ts`: küfür, üslup ve takım hesaplama. Toplam 20 test.

## 8. Sıradaki işler

| # | İş | Neden |
|---|---|---|
| 1 | Cihaz testi (`android/`) | Konseptin teknik temeli |
| 2 | İçerik takvimi: 8 haftalık "küçük ilkler" ve özel günler | Duvar kağıdı körlüğüne karşı tek gerçek ilaç |
| 3 | Chibi karakter tasarımı: bir çizerle stil rehberi, Rive ile animasyon | Bağ kurmanın ilk şartı karakterin gerçekten tatlı olması |
| 4 | Kurulum rehberi ve doğum anı | En büyük kayıp noktası |
| 5 | Karakter buluşması (kelime kapma) | Ayrıştırıcı ve kendiliğinden yayılan özellik |
| 6 | Sesli ilk kelime | Duygusal zirve anı |
