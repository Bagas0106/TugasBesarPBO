# Struktur Project SIPART

## Source dan hasil build

Kode yang harus dibaca dan diedit berada di `src/main`. Folder `target` bukan source code. Maven membuat `target` secara otomatis ketika aplikasi dikompilasi atau dikemas menjadi JAR.

Entry point aplikasi:

```text
src/main/java/com/TugasBesar/demo/SipartApplication.java
```

Method `main()` di class tersebut menjalankan Spring Boot.

## Pembagian package

```text
src/main/java/com/TugasBesar/demo/
|-- SipartApplication.java        Entry point
|-- config/
|   `-- WebConfig.java            Session, role, dan permission API
|-- controller/
|   |-- PageController.java       Routing halaman HTML
|   |-- AuthController.java       Login, session, logout
|   |-- DashboardController.java  Dashboard dan laporan periodik
|   |-- CatalogController.java    Produk, kategori, riwayat harga
|   |-- InventoryController.java  Stock opname
|   |-- SupplierController.java   Supplier dan aktivitas
|   |-- PurchaseController.java   Transaksi pembelian
|   |-- SalesController.java      Transaksi penjualan
|   `-- UserController.java       User dan hak akses
|-- domain/
|   |-- Product.java
|   |-- Category.java
|   |-- Supplier.java
|   |-- Purchase.java
|   |-- StockOpname.java
|   |-- Sale.java
|   |-- SaleItem.java
|   `-- user/
|       |-- User.java             Parent class
|       |-- Owner.java            Child class
|       |-- AdminGudang.java      Child class
|       |-- AdminKasir.java       Child class
|       `-- UserFactory.java
`-- service/
    `-- SipartService.java        Aturan bisnis dan transaksi database
```

File frontend berada di `src/main/resources/static`. Schema dan data awal database berada di `src/main/resources/db`.

## Alur pemanggilan kode

```text
HTML + JavaScript
      |
      v
Controller API
      |
      v
SipartService
      |
      v
JdbcTemplate
      |
      v
MySQL sipart_db
```

Domain model menangani perilaku objek, misalnya `Product.stockStatus()`, `Purchase.total()`, `StockOpname.difference()`, dan `SaleItem.subtotal()`.

## Konsep OOP dari proposal

- Encapsulation: data dan perilaku dikelompokkan di domain class.
- Inheritance: `Owner`, `AdminGudang`, dan `AdminKasir` mewarisi `User`.
- Polymorphism: routing memanggil `canAccessMenu()`, `canAccessScope()`, dan `defaultPath()` melalui tipe `User` tanpa perlu mengetahui child class secara langsung.
- Composition: `Sale` memiliki kumpulan `SaleItem`.
- Association: produk terhubung ke kategori, pembelian terhubung ke supplier dan produk.
