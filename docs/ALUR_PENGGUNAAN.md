# Alur Penggunaan SIPART

## Alur umum

```text
Jalankan XAMPP MySQL
        |
Jalankan backend Spring Boot
        |
Buka /login dan pilih role
        |
Sistem memvalidasi akun, role, status, dan permission
        |
User diarahkan ke ruang kerja sesuai role
```

## Owner

1. Login menggunakan role **Owner**.
2. Dashboard menampilkan omset, modal, keuntungan, jumlah produk terjual, stok kritis, pembelian, produk terlaris, dan profit produk.
3. Pilih laporan Harian, Mingguan, Bulanan, Tahunan, atau tentukan rentang tanggal.
4. Buka Produk & Kategori untuk memeriksa stok dan riwayat harga.
5. Buka Supplier dan Transaksi Pembelian untuk memantau aktivitas gudang.
6. Buka Manajemen User untuk menambah user, mengganti role, aktivasi akun, reset akun, dan mengatur permission.

Owner tidak menjalankan transaksi kasir. Pembatasan ini sesuai pemisahan tanggung jawab pada proposal.

## Admin Gudang

1. Login menggunakan role **Admin Gudang**.
2. Tambah atau perbarui produk, kategori, harga jual, stok minimum, dan data supplier.
3. Buat transaksi pembelian dengan memilih supplier, produk, jumlah, dan harga satuan.
4. Ketika barang datang, buka pemeriksaan pembelian dan masukkan jumlah fisik.
5. Sistem menghitung selisih antara jumlah pesanan dan jumlah fisik.
6. Konfirmasi barang masuk. Stok produk bertambah dan aktivitas supplier tercatat.
7. Gunakan Stock Opname untuk membandingkan stok sistem dengan stok fisik gudang.

## Admin Kasir

1. Login menggunakan role **Admin Kasir**.
2. Cari produk berdasarkan nama atau SKU.
3. Tambahkan produk yang tersedia ke keranjang dan tentukan jumlahnya.
4. Pilih metode pembayaran Tunai atau QRIS.
5. Untuk pembayaran tunai, masukkan jumlah pembayaran. Sistem menghitung pajak, total, dan kembalian.
6. Proses transaksi. Stok produk otomatis berkurang dan jumlah terjual bertambah.
7. Nota digital ditampilkan setelah transaksi berhasil.

## Alur pembelian sampai stock opname

```text
Input pembelian
      |
Status Pending
      |
Barang datang dan diperiksa
      |
Jumlah sesuai? ---- tidak ----> Status Selisih + catatan
      | ya
      v
Status Barang Datang
      |
Konfirmasi barang masuk
      |
Stok produk bertambah + riwayat supplier tersimpan
      |
Stock opname membandingkan stok sistem dan fisik
```

## Alur transaksi penjualan

```text
Pilih produk -> Keranjang -> Hitung subtotal dan pajak
      -> Pilih pembayaran -> Validasi pembayaran
      -> Simpan transaksi -> Kurangi stok -> Tampilkan nota
```
