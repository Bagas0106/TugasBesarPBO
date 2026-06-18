# SIPART

Aplikasi inventaris dan transaksi sparepart berbasis Spring Boot 4, Java 17, dan Tailwind CSS.

## Menjalankan aplikasi

Pastikan Java 17 tersedia, lalu buka PowerShell pada folder proyek ini dan jalankan:

```powershell
.\mvnw.cmd spring-boot:run
```

Buka `http://localhost:8080/login` di browser.

## Akun demo

| Role | Email | Password |
| --- | --- | --- |
| Owner | `admin@sipart.test` | `demo` |
| Admin Gudang | `gudang@sipart.test` | `demo` |
| Admin Kasir | `kasir@sipart.test` | `demo` |

Setiap role diarahkan ke ruang kerja dan API yang sesuai. Owner mengelola inventaris dan user, Admin Gudang mengelola inventaris serta pembelian, sedangkan Admin Kasir menggunakan transaksi penjualan.

Jika port 8080 sedang dipakai, gunakan JAR pada port lain:

```powershell
.\mvnw.cmd clean package
java -jar target\demo-0.0.1-SNAPSHOT.jar --server.port=8082
```

Kemudian buka `http://localhost:8082`.

## Menjalankan tes

```powershell
.\mvnw.cmd test
```

Data aplikasi disimpan di memori untuk kebutuhan demo, sehingga kembali ke data awal setiap kali backend dimulai ulang.
