# Cihaz testi: canlı duvar kağıdı kilit ekranında görünüyor mu?

Bu proje ürün değil, **1 günlük teknik denemedir**. Ürünün en büyük teknik riskini cevaplıyor: bazı üreticiler kilit ekranında başka uygulamaların canlı duvar kağıdını göstermiyor olabilir. Cevap "hayır" çıkarsa ürün "sadece ana ekran" olarak tasarlanır.

> Bu kod bulut ortamında derlenemedi (Google'ın Android sunucularına erişim yoktu). İlk açışta Android Studio küçük düzeltmeler isteyebilir. Hata çıkarsa hata metnini bana gönder, birlikte düzeltiriz.

## Kurulum

1. [Android Studio](https://developer.android.com/studio)'yu kur.
2. `File → Open` ile bu `android/` klasörünü aç. Android Studio Gradle'ı ve SDK'yı kendisi indirir. "Gradle wrapper eksik" gibi bir uyarı verirse önerdiği düzeltmeyi kabul et.
3. Telefonda **Geliştirici seçenekleri → USB hata ayıklama**'yı aç, telefonu USB ile bağla.
4. Yukarıdaki yeşil ▶ düğmesiyle uygulamayı telefona yükle.

## Her cihazda yapılacak test

Uygulamayı aç, "Duvar kağıdı olarak ayarla"ya bas. Seçim ekranında **"Ana ekran ve kilit ekranı"** seçeneğini seç (üreticiye göre adı değişir).

| # | Test | Nasıl bakılır |
|---|---|---|
| 1 | Kilit ekranında görünüyor mu? | Ekranı kilitle, güç tuşuyla aç. Bebek ve turuncu yaylar görünüyor mu? |
| 2 | Kilit ekranında hareket ediyor mu? | Bebek hafifçe nefes alıyor mu (yukarı aşağı)? |
| 3 | Ekran kapanınca duruyor mu? | Günlükte "gizlendi · X sn · Y kare" satırı var mı? |
| 4 | Ana ekranda dokunma geliyor mu? | Boş alana dokun: bebek oraya yürüyor mu? Günlükte "dokunma · boş alana" var mı? |
| 5 | Sayfa kaydırma geliyor mu? | Ana ekranda sağa kaydır: bebek ve yaylar kayıyor mu? Günlükte "sayfa · xOffset" var mı? |
| 6 | Bebeğe dokununca uygulama açılıyor mu? | Bebeğin üstüne dokun. Uygulama açıldı mı? Günlükte "MainActivity AÇILDI" var mı? |
| 7 | Şarj ve kulaklık okunuyor mu? | Şarja tak veya kulaklık tak, ekranı kilitle-aç. "GÖRÜNÜR" satırında "şarj: true" ve "kulaklık: true" görünüyor mu? |
| 8 | Sürükleme geliyor mu? | Ana ekranda boş bir yerde parmağını sürükle (sayfa değiştirmeden, yukarı-aşağı). Günlükte "SÜRÜKLEME geldi" var mı? Varsa bebeği parmakla taşıyabiliriz. |
| 9 | Pil | Bir gün boyunca kullan. Ayarlar → Pil ekranında "Ortak Yaşam Test" kaç % harcamış? |

Sonunda uygulamada **"Günlüğü paylaş"** ile günlüğü bana gönder (WhatsApp ya da e-posta).

## Hangi cihazlar

En az şu üçü: **bir Samsung** (One UI), **bir Xiaomi / Redmi / POCO** (HyperOS), **bir Pixel ya da stok Android'e yakın** bir cihaz. Varsa Oppo/Realme ve Huawei de eklenebilir (Huawei'de Google servisleri olmadığı için ürün zaten ayrı bir karar).

## Sonuç tablosu

| Cihaz | Android | 1 Kilit | 2 Hareket | 3 Durma | 4 Dokunma | 5 Sayfa | 6 Uygulama açma | 7 Sinyaller | 8 Sürükleme | 9 Pil % |
|---|---|---|---|---|---|---|---|---|---|---|
| | | | | | | | | | | |
