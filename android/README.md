# Cihaz testi: canlı duvar kağıdı kilit ekranında görünüyor mu?

Bu proje ürün değil, **1 günlük teknik denemedir**. Ürünün en büyük teknik riskini cevaplıyor: bazı üreticiler kilit ekranında başka uygulamaların canlı duvar kağıdını göstermiyor olabilir. Cevap "hayır" çıkarsa ürün "sadece ana ekran" olarak tasarlanır.

Uygulama artık prototipteki chibi karakteri ve etkileşimleri (kap, yatak, top, pile asılma, şarj, taşıma, yumurtadan çıkış) içeriyor; test soruları için günlük tutmaya devam ediyor.

## Kurulum

Android Studio gerekmez. GitHub her değişiklikte APK'yı kendisi derleyip [deneme sürümüne](https://github.com/yedekkayakiko-sudo/sanalbebek/releases/tag/deneme) koyuyor (`.github/workflows/android.yml`). Telefona kurulum adımları: [KURULUM.md](../KURULUM.md).

Geliştirici için: Android Studio'da `android/` klasörünü açıp ▶ ile de yüklenebilir.

## Her cihazda yapılacak test

Uygulamayı aç, "Duvar kağıdı olarak ayarla"ya bas. Seçim ekranında **"Ana ekran ve kilit ekranı"** seçeneğini seç (üreticiye göre adı değişir).

| # | Test | Nasıl bakılır |
|---|---|---|
| 1 | Kilit ekranında görünüyor mu? | Ekranı kilitle, güç tuşuyla aç. Bebek en altta görünüyor mu? |
| 2 | Kilit ekranında hareket ediyor mu? | Bebek göz kırpıyor, geziniyor mu? |
| 3 | Ekran kapanınca duruyor mu? | Günlükte "gizlendi · X sn · Y kare" satırı var mı? |
| 4 | Ana ekranda dokunma geliyor mu? | Boş alana dokun: bebek oraya yürüyor mu? Günlükte "dokunma · boş alana" var mı? |
| 5 | Sayfa kaydırma geliyor mu? | Ana ekranda sağa kaydır: bebek ve eşyalar kayıyor mu? Günlükte "sayfa · xOffset" var mı? |
| 6 | Bebeğe dokunma ve pil | Bebeğe dokun: gülüyor mu (günlükte "BEBEĞE")? Pilin altına dokun: tırmanıp asılıyor mu, pil ikonuyla hizalı mı? |
| 7 | Şarj ve kulaklık okunuyor mu? | Şarja tak veya kulaklık tak, ekranı kilitle-aç. "GÖRÜNÜR" satırında "şarj: true" ve "kulaklık: true" görünüyor mu? |
| 8 | Sürükleme geliyor mu? | Ana ekranda boş bir yerde parmağını sürükle (sayfa değiştirmeden, yukarı-aşağı). Bebeği tutup sürükle: taşınıyor mu? Günlükte "SÜRÜKLEME geldi" var mı? |
| 9 | Pil | Bir gün boyunca kullan. Ayarlar → Pil ekranında "Ortak Yaşam Test" kaç % harcamış? |

Sonunda uygulamada **"Günlüğü paylaş"** ile günlüğü bana gönder (WhatsApp ya da e-posta).

## Hangi cihazlar

En az şu üçü: **bir Samsung** (One UI), **bir Xiaomi / Redmi / POCO** (HyperOS), **bir Pixel ya da stok Android'e yakın** bir cihaz. Varsa Oppo/Realme ve Huawei de eklenebilir (Huawei'de Google servisleri olmadığı için ürün zaten ayrı bir karar).

## Sonuç tablosu

| Cihaz | Android | 1 Kilit | 2 Hareket | 3 Durma | 4 Dokunma | 5 Sayfa | 6 Dokunma/pil | 7 Sinyaller | 8 Sürükleme | 9 Pil % |
|---|---|---|---|---|---|---|---|---|---|---|
| | | | | | | | | | | |
