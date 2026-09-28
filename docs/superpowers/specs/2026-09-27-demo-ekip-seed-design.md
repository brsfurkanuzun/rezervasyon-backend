# Demo Ekip ve Hizmet Seed Tasarimi

- Mevcut 10 demo isletmesi korunur; her isletmede 3-5 aktif hizmet, 2-4 farkli sayida ekip uyesi bulunur.
- Her seeded ekip uyesinde Pexels profil fotografi olur; ekip uyeleri bir veya daha fazla hizmete atanir.
- Var olan seed isletmeleri slug, hizmetleri ad, ekip uyelerini ad-soyad ile eslestirilerek eksikleri tamamlanir; hicbir isletme/hizmet/ekip kaydi silinmez.
- Calisma saatleri yalnizca saati bulunmayan yeni ekip uyelerine eklenir.
- Gecici eklemeler seed tekrar calistiginda cogalmaz.

## Dogrulama

Seed katalog dagilimi veritabanina baglanmayan bir JUnit testiyle kontrol edilir. Paket derlemesi ve gelistirme seed'i bir kez calistirildiktan sonra yalniz-okunur SQL ile isletme/hizmet/ekip/fotograf sayilari dogrulanir.