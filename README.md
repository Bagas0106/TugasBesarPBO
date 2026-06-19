# SIPART

Aplikasi inventaris dan transaksi sparepart berbasis Spring Boot 4, Java 17, Tailwind CSS, dan MySQL.

Dokumentasi proyek:

- [Struktur class dan package](docs/STRUKTUR_PROJECT.md)
- [Alur penggunaan aplikasi](docs/ALUR_PENGGUNAAN.md)

## Menyiapkan database

1. Jalankan **Apache** dan **MySQL** dari XAMPP.
2. Buka `http://localhost/phpmyadmin`.
3. Buat database bernama `sipart_db` jika user MySQL tidak memiliki izin membuat database.

Saat backend dimulai, tabel dan data awal otomatis dibuat oleh:

- `src/main/resources/db/schema.sql`
- `src/main/resources/db/data.sql`

Konfigurasi bawaan menggunakan MySQL `root` tanpa password. Jika password MySQL berbeda, set environment variable sebelum menjalankan aplikasi:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="password_mysql"
```

Untuk alamat database yang berbeda:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/sipart_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Bangkok"
```

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
java -jar target\sipart.jar --server.port=8082
```

Kemudian buka `http://localhost:8082`.

## Menjalankan tes

```powershell
.\mvnw.cmd test
```

Data aplikasi tersimpan permanen di database `sipart_db` dan tidak kembali ke data awal ketika backend dimulai ulang. Tes otomatis menggunakan database H2 terpisah sehingga tidak mengubah data MySQL lokal.
