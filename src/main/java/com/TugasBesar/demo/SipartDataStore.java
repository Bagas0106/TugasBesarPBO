package com.TugasBesar.demo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SipartDataStore {
	private final AtomicInteger productSequence = new AtomicInteger(5);
	private final AtomicInteger categorySequence = new AtomicInteger(7);
	private final AtomicInteger supplierSequence = new AtomicInteger(4);
	private final AtomicInteger userSequence = new AtomicInteger(4);
	private final AtomicInteger purchaseSequence = new AtomicInteger(9202);
	private final AtomicInteger saleSequence = new AtomicInteger(1001);
	private final AtomicInteger stockCheckSequence = new AtomicInteger(5);

	private final List<Product> products = new ArrayList<>();
	private final List<Category> categoryList = new ArrayList<>();
	private final List<Supplier> suppliers = new ArrayList<>();
	private final List<SupplierActivity> supplierActivities = new ArrayList<>();
	private final List<UserAccount> users = new ArrayList<>();
	private final List<Purchase> purchases = new ArrayList<>();
	private final List<Sale> sales = new ArrayList<>();
	private final List<StockCheck> stockChecks = new ArrayList<>();
	private final List<PriceChange> priceChanges = new ArrayList<>();

	public SipartDataStore() {
		categoryList.add(new Category(1, "OLI", "oli"));
		categoryList.add(new Category(2, "BAN", "ban"));
		categoryList.add(new Category(3, "MESIN", "mesin"));
		categoryList.add(new Category(4, "AKI", "aki"));
		categoryList.add(new Category(5, "KAMPAS", "kampas"));
		categoryList.add(new Category(6, "LAMPU", "lampu"));

		products.add(new Product(1, "Oli Shell Advance AX7", "OLI-SHL-001", "OLI", 65000, 28000, 156, 20, 320, "oli"));
		products.add(new Product(2, "Ban IRC NR91 80/90", "BAN-IRC-042", "BAN", 215000, 165000, 42, 15, 145, "ban"));
		products.add(new Product(3, "Busi Denso Iridium", "BUS-DNS-992", "MESIN", 105000, 45000, 156, 12, 89, "mesin"));
		products.add(new Product(4, "Aki GS Astra GTZ5S", "AKI-GSA-005", "AKI", 245000, 185000, 3, 10, 42, "aki"));

		suppliers.add(new Supplier(1, "PT Jaya Abadi Tekstil", "Budi Santoso", "081234567890", "AKTIF", "PT", "Sparepart Mesin", "Bandung", "budi@jayaabadi.id", "Hari ini"));
		suppliers.add(new Supplier(2, "CV Maju Terus", "Ani Wijaya", "081234567891", "AKTIF", "CV", "Ban & Kaki-kaki", "Jakarta", "ani@maju-terus.id", "Kemarin"));
		suppliers.add(new Supplier(3, "UD Berkah Jaya", "Agus Setiawan", "081234567892", "NONAKTIF", "UD", "Kelistrikan", "Surabaya", "agus@berkahjaya.id", "3 hari lalu"));
		supplierActivities.add(new SupplierActivity(1, "Supplier Baru Ditambahkan", "PT Global Mandiri terdaftar di sistem.", "Tadi - 09:45", "orange"));
		supplierActivities.add(new SupplierActivity(2, "Pembelian Selesai", "Invoice INV-2023-091 telah dibayar penuh.", "Kemarin - 14:20", "blue"));
		supplierActivities.add(new SupplierActivity(3, "Pembaruan Profil", "Kontak PIC CV Maju Terus diperbarui.", "3 Okt - 11:00", "soft"));

		users.add(new UserAccount(1, "Raihan Haryo", "Owner", "Aktif", "admin_sipart", "admin@sipart.test", "demo", "Hari ini, 08:45", defaultPermissions("Owner")));
		users.add(new UserAccount(2, "Admin Gudang", "Admin Gudang", "Aktif", "budi_gudang", "gudang@sipart.test", "demo", "Kemarin, 21:30", defaultPermissions("Admin Gudang")));
		users.add(new UserAccount(3, "Kasir SIPART", "Admin Kasir", "Aktif", "citra_kasir", "kasir@sipart.test", "demo", "3 hari lalu", defaultPermissions("Admin Kasir")));

		purchases.add(new Purchase(9201, "PT. Global Tekno", "Busi Denso Iridium", "MESIN", 250, 49600, "Barang Datang", "20 Oct 2023"));
		purchases.add(new Purchase(9188, "Sumber Makmur", "Oli Shell Advance AX7", "OLI", 45, 71000, "Pending", "19 Oct 2023"));
		purchases.add(new Purchase(9142, "Anugerah Sparepart", "Ban IRC NR91 80/90", "BAN", 1200, 38166, "Selesai", "18 Oct 2023"));

		stockChecks.add(new StockCheck(1, "Ban IRC NR82 - 90/90-14", "TIRE-IRC-01", 45, 45, "MATCH"));
		stockChecks.add(new StockCheck(2, "Aki Kering GS Astra", "BAT-GS-N540", 150, 138, "DIFFERENCE"));
		stockChecks.add(new StockCheck(3, "Busi NGK Iridium", "PLG-NGK-IR", 80, null, "PENDING"));
		stockChecks.add(new StockCheck(4, "V-Belt Honda Vario", "BELT-HND-VR", 25, 25, "MATCH"));
	}

	public synchronized Map<String, Object> dashboard() {
		long totalRevenue = products.stream().mapToLong(product -> (long) product.sold * product.price).sum();
		long totalCapital = products.stream().mapToLong(product -> (long) product.sold * product.cost).sum();
		long totalProfit = totalRevenue - totalCapital;
		int totalSold = products.stream().mapToInt(product -> product.sold).sum();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("metrics", List.of(
				metric("Total Pendapatan", rupiah(totalRevenue)),
				metric("Total Modal", rupiah(totalCapital)),
				metric("Total Keuntungan", rupiah(totalProfit)),
				metric("Total Produk Terjual", String.valueOf(totalSold))
		));
		response.put("lowStock", products.stream()
				.filter(product -> product.stock <= product.minStock)
				.sorted(Comparator.comparingInt(product -> product.stock))
				.map(ProductView::from)
				.toList());
		response.put("bestSellers", products.stream()
				.sorted(Comparator.comparingInt((Product product) -> product.sold).reversed())
				.limit(5)
				.map(product -> new BestSeller(product.name, product.sold + " Unit", rupiah((long) product.sold * (product.price - product.cost))))
				.toList());
		response.put("profits", products.stream()
				.sorted(Comparator.comparingInt((Product product) -> product.sold).reversed())
				.limit(4)
				.map(product -> new ProfitRow(product.name, product.sold + " Unit", rupiah((long) product.sold * product.price), rupiah((long) product.sold * (product.price - product.cost))))
				.toList());
		response.put("purchaseHistories", purchases().stream().limit(5).toList());
		response.put("recentSales", sales().stream().limit(5).toList());
		response.put("totalTransactions", purchases.size() + sales.size());
		return response;
	}

	public synchronized List<ProductView> products(String query, String category, String status) {
		return products.stream()
				.filter(product -> isBlank(query) || product.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) || product.sku.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
				.filter(product -> isBlank(category) || category.equalsIgnoreCase("Semua Kategori") || product.category.equalsIgnoreCase(category))
				.filter(product -> isBlank(status) || status.equalsIgnoreCase("Semua Status") || status(product).equalsIgnoreCase(status))
				.map(ProductView::from)
				.toList();
	}

	public synchronized ProductView createProduct(ProductRequest request) {
		if (isBlank(request.name())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama produk wajib diisi.");
		}
		String category = isBlank(request.category()) ? "UMUM" : request.category().trim().toUpperCase(Locale.ROOT);
		int id = productSequence.getAndIncrement();
		long price = strictlyPositiveOrDefault(request.price(), 50000);
		int stock = positiveOrDefault(request.stock(), 0);
		int minStock = positiveOrDefault(request.minStock(), 10);
		String sku = category.substring(0, Math.min(3, category.length())) + "-NEW-" + String.format("%03d", id);
		Product product = new Product(id, request.name().trim(), sku, category, price, Math.max(1, Math.round(price * 0.62f)), stock, minStock, 0, iconFor(category));
		ensureCategory(category);
		products.add(product);
		priceChanges.add(0, new PriceChange(id, product.name, 0, price, "Harga awal", timestamp()));
		return ProductView.from(product);
	}

	public synchronized ProductView updateProduct(int id, ProductUpdateRequest request) {
		Product product = productById(id);
		long oldPrice = product.price;
		if (!isBlank(request.name())) {
			product.name = request.name().trim();
		}
		if (!isBlank(request.category())) {
			product.category = request.category().trim().toUpperCase(Locale.ROOT);
			product.icon = iconFor(product.category);
			ensureCategory(product.category);
		}
		if (request.price() != null && request.price() > 0) {
			product.price = request.price();
		}
		if (request.stock() != null && request.stock() >= 0) {
			product.stock = request.stock();
		}
		if (request.minStock() != null && request.minStock() >= 0) {
			product.minStock = request.minStock();
		}
		if (oldPrice != product.price) {
			priceChanges.add(0, new PriceChange(product.id, product.name, oldPrice, product.price, "Pembaruan harga jual", timestamp()));
		}
		return ProductView.from(product);
	}

	public synchronized void deleteProduct(int id) {
		boolean removed = products.removeIf(product -> product.id == id);
		if (!removed) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produk tidak ditemukan.");
		}
	}

	public synchronized List<CategorySummary> categories() {
		Map<String, Long> counts = products.stream().collect(Collectors.groupingBy(product -> product.category, LinkedHashMap::new, Collectors.counting()));
		return categoryList.stream()
				.map(category -> new CategorySummary(category.id, category.name, counts.getOrDefault(category.name, 0L), category.icon))
				.toList();
	}

	public synchronized CategorySummary createCategory(CategoryRequest request) {
		if (isBlank(request.name())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama kategori wajib diisi.");
		}
		String name = request.name().trim().toUpperCase(Locale.ROOT);
		if (categoryList.stream().anyMatch(category -> category.name.equalsIgnoreCase(name))) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Kategori sudah tersedia.");
		}
		Category category = new Category(categorySequence.getAndIncrement(), name, iconFor(name));
		categoryList.add(category);
		return new CategorySummary(category.id, category.name, 0, category.icon);
	}

	public synchronized CategorySummary updateCategory(int id, CategoryRequest request) {
		Category category = categoryById(id);
		if (isBlank(request.name())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama kategori wajib diisi.");
		}
		String oldName = category.name;
		category.name = request.name().trim().toUpperCase(Locale.ROOT);
		category.icon = iconFor(category.name);
		products.stream().filter(product -> product.category.equalsIgnoreCase(oldName)).forEach(product -> {
			product.category = category.name;
			product.icon = category.icon;
		});
		long count = products.stream().filter(product -> product.category.equalsIgnoreCase(category.name)).count();
		return new CategorySummary(category.id, category.name, count, category.icon);
	}

	public synchronized void deleteCategory(int id) {
		Category category = categoryById(id);
		if (products.stream().anyMatch(product -> product.category.equalsIgnoreCase(category.name))) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Kategori masih digunakan oleh produk.");
		}
		categoryList.remove(category);
	}

	public synchronized List<PriceChange> priceHistory(Integer productId) {
		return priceChanges.stream()
				.filter(change -> productId == null || change.productId() == productId)
				.toList();
	}

	public synchronized List<StockCheckView> stockChecks() {
		return stockChecks.stream().map(StockCheckView::from).toList();
	}

	public synchronized List<StockCheckView> stockChecks(String query, String status) {
		return stockChecks.stream()
				.filter(check -> isBlank(query) || check.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) || check.sku.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
				.filter(check -> isBlank(status) || check.status.equalsIgnoreCase(status))
				.map(StockCheckView::from)
				.toList();
	}

	public synchronized StockCheckView createStockCheck(StockOpnameRequest request) {
		if (request.productId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Barang wajib dipilih.");
		}
		Product product = productById(request.productId());
		Integer physicalStock = request.physicalStock();
		if (physicalStock == null || physicalStock < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stok fisik wajib diisi.");
		}
		int systemStock = product.stock;
		product.stock = physicalStock;
		String status = systemStock == physicalStock ? "MATCH" : "DIFFERENCE";
		StockCheck check = new StockCheck(stockCheckSequence.getAndIncrement(), product.name, product.sku, systemStock, physicalStock, status);
		stockChecks.add(0, check);
		return StockCheckView.from(check);
	}

	public synchronized List<Supplier> suppliers() {
		return new ArrayList<>(suppliers);
	}

	public synchronized List<Supplier> suppliers(String query, String status) {
		return suppliers.stream()
				.filter(supplier -> isBlank(query) || supplier.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) || supplier.pic.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
				.filter(supplier -> isBlank(status) || supplier.status.equalsIgnoreCase(status))
				.toList();
	}

	public synchronized List<SupplierActivity> supplierActivities() {
		return new ArrayList<>(supplierActivities);
	}

	public synchronized Supplier createSupplier(SupplierRequest request) {
		if (isBlank(request.name())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama supplier wajib diisi.");
		}
		int id = supplierSequence.getAndIncrement();
		String logo = request.name().trim().replaceAll("[^A-Za-z]", "");
		logo = logo.length() >= 2 ? logo.substring(0, 2).toUpperCase(Locale.ROOT) : "SP";
		Supplier supplier = new Supplier(id, request.name().trim(), defaultText(request.pic(), "PIC Supplier"), defaultText(request.phone(), "081234567890"), "AKTIF", logo, defaultText(request.category(), "Sparepart Umum"), defaultText(request.address(), "Alamat belum diisi"), defaultText(request.email(), "supplier@sipart.test"), "Baru ditambahkan");
		suppliers.add(supplier);
		supplierActivities.add(0, new SupplierActivity(id, "Supplier Baru Ditambahkan", supplier.name + " terdaftar di sistem.", timestamp(), "orange"));
		return supplier;
	}

	public synchronized Supplier updateSupplier(int id, SupplierUpdateRequest request) {
		Supplier supplier = supplierById(id);
		if (!isBlank(request.name())) {
			supplier.name = request.name().trim();
		}
		if (!isBlank(request.pic())) {
			supplier.pic = request.pic().trim();
		}
		if (!isBlank(request.phone())) {
			supplier.phone = request.phone().trim();
		}
		if (!isBlank(request.status())) {
			supplier.status = request.status().trim().toUpperCase(Locale.ROOT);
		}
		if (!isBlank(request.category())) supplier.category = request.category().trim();
		if (!isBlank(request.address())) supplier.address = request.address().trim();
		if (!isBlank(request.email())) supplier.email = request.email().trim();
		supplier.lastUsed = timestamp();
		supplierActivities.add(0, new SupplierActivity(id, "Pembaruan Profil", "Kontak " + supplier.name + " diperbarui.", timestamp(), "soft"));
		return supplier;
	}

	public synchronized void deleteSupplier(int id) {
		boolean removed = suppliers.removeIf(supplier -> supplier.id == id);
		if (!removed) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier tidak ditemukan.");
		}
		supplierActivities.add(0, new SupplierActivity(id, "Supplier Dihapus", "Data supplier dihapus dari sistem.", timestamp(), "red"));
	}

	public synchronized List<PurchaseView> purchases() {
		return purchases.stream().sorted(Comparator.comparingInt((Purchase purchase) -> purchase.id).reversed()).map(PurchaseView::from).toList();
	}

	public synchronized List<PurchaseView> purchases(String query, String status) {
		return purchases.stream()
				.filter(purchase -> isBlank(query) || purchase.supplier.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) || purchase.item.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
				.filter(purchase -> isBlank(status) || purchase.status.equalsIgnoreCase(status))
				.sorted(Comparator.comparingInt((Purchase purchase) -> purchase.id).reversed())
				.map(PurchaseView::from)
				.toList();
	}

	public synchronized PurchaseView createPurchase(PurchaseRequest request) {
		if (isBlank(request.supplier()) || isBlank(request.item())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier dan barang wajib diisi.");
		}
		int id = purchaseSequence.getAndIncrement();
		Purchase purchase = new Purchase(id, request.supplier().trim(), request.item().trim(), defaultText(request.category(), "UMUM"), strictlyPositiveOrDefault(request.quantity(), 1), strictlyPositiveOrDefault(request.unitPrice(), 1), "Pending", DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH).format(LocalDateTime.now()));
		purchases.add(purchase);
		return PurchaseView.from(purchase);
	}

	public synchronized PurchaseView inspectPurchase(int id, InspectionRequest request) {
		Purchase purchase = purchaseById(id);
		if (request.physicalQuantity() == null || request.physicalQuantity() < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jumlah fisik wajib diisi.");
		}
		purchase.physicalQuantity = request.physicalQuantity();
		purchase.inspectionNote = defaultText(request.note(), "Pemeriksaan fisik tersimpan");
		purchase.status = purchase.physicalQuantity == purchase.quantity ? "Barang Datang" : "Selisih";
		return PurchaseView.from(purchase);
	}

	public synchronized PurchaseView confirmPurchase(int id) {
		Purchase purchase = purchaseById(id);
		if ("Selesai".equalsIgnoreCase(purchase.status)) {
			return PurchaseView.from(purchase);
		}
		purchase.status = "Selesai";
		Optional<Product> product = products.stream().filter(item -> item.name.equalsIgnoreCase(purchase.item)).findFirst();
		int acceptedQuantity = purchase.physicalQuantity == null ? purchase.quantity : purchase.physicalQuantity;
		product.ifPresent(item -> item.stock += acceptedQuantity);
		suppliers.stream().filter(supplier -> supplier.name.equalsIgnoreCase(purchase.supplier)).findFirst().ifPresent(supplier -> supplier.lastUsed = timestamp());
		supplierActivities.add(0, new SupplierActivity(0, "Pembelian Selesai", "#TX-" + purchase.id + " dari " + purchase.supplier + " dikonfirmasi.", timestamp(), "blue"));
		return PurchaseView.from(purchase);
	}

	public synchronized void deletePurchase(int id) {
		Purchase purchase = purchaseById(id);
		if ("Selesai".equalsIgnoreCase(purchase.status)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaksi yang selesai tidak dapat dihapus.");
		}
		purchases.remove(purchase);
	}

	public synchronized List<ProductView> salesProducts() {
		return products.stream().map(ProductView::from).toList();
	}

	public synchronized List<SaleView> sales() {
		return sales.stream().map(SaleView::from).toList();
	}

	public synchronized SaleView sale(int id) {
		return sales.stream().filter(sale -> sale.id == id).findFirst().map(SaleView::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaksi penjualan tidak ditemukan."));
	}

	public synchronized SaleView createSale(SaleRequest request) {
		if (request.items() == null || request.items().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keranjang masih kosong.");
		}
		List<SaleLine> lines = new ArrayList<>();
		long subtotal = 0;
		Map<Integer, Integer> quantities = new LinkedHashMap<>();
		for (SaleItemRequest itemRequest : request.items()) {
			if (itemRequest.productId() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produk transaksi wajib dipilih.");
			}
			int quantity = strictlyPositiveOrDefault(itemRequest.quantity(), 1);
			quantities.merge(itemRequest.productId(), quantity, Integer::sum);
		}
		for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
			Product product = productById(entry.getKey());
			int quantity = entry.getValue();
			if (product.stock < quantity) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stok " + product.name + " tidak cukup.");
			}
			subtotal += product.price * quantity;
			lines.add(new SaleLine(product.id, product.name, quantity, product.price));
		}
		long tax = Math.round(subtotal * 0.11);
		long total = subtotal + tax;
		long paid = strictlyPositiveOrDefault(request.paid(), total);
		if (paid < total) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jumlah pembayaran kurang dari total transaksi.");
		}
		for (SaleLine line : lines) {
			Product product = productById(line.productId());
			product.stock -= line.quantity();
			product.sold += line.quantity();
		}
		Sale sale = new Sale(saleSequence.getAndIncrement(), lines, subtotal, tax, total, paid, defaultText(request.paymentMethod(), "Tunai"), LocalDateTime.now());
		sales.add(0, sale);
		return SaleView.from(sale);
	}

	public synchronized List<UserAccount> users() {
		return new ArrayList<>(users);
	}

	public synchronized UserAccount createUser(UserRequest request) {
		if (isBlank(request.name())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama user wajib diisi.");
		}
		String role = normalizeRole(request.role());
		int id = userSequence.getAndIncrement();
		String username = defaultText(request.username(), request.name().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_"));
		String email = defaultText(request.email(), username + "@sipart.test");
		String status = defaultText(request.status(), "Aktif");
		UserAccount user = new UserAccount(id, request.name().trim(), role, status, username, email, "demo", "Belum pernah login", defaultPermissions(role));
		users.add(user);
		return user;
	}

	public synchronized UserAccount updateUser(int id, UserUpdateRequest request) {
		UserAccount user = userById(id);
		if (!isBlank(request.name())) user.name = request.name().trim();
		if (!isBlank(request.role())) {
			user.role = normalizeRole(request.role());
			user.permissions = defaultPermissions(user.role);
		}
		if (!isBlank(request.status())) user.status = request.status().trim();
		if (!isBlank(request.username())) user.username = request.username().trim();
		if (!isBlank(request.email())) user.email = request.email().trim();
		return user;
	}

	public synchronized UserAccount resetUser(int id) {
		UserAccount user = userById(id);
		user.status = "Aktif";
		user.password = "demo";
		user.lastLogin = "Belum login ulang";
		user.permissions = defaultPermissions(user.role);
		return user;
	}

	public synchronized Map<String, PermissionSet> userPermissions(int id) {
		return new LinkedHashMap<>(userById(id).permissions);
	}

	public synchronized Map<String, PermissionSet> updateUserPermissions(int id, PermissionRequest request) {
		UserAccount user = userById(id);
		if (request.permissions() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data permission wajib diisi.");
		}
		user.permissions = new LinkedHashMap<>(request.permissions());
		return new LinkedHashMap<>(user.permissions);
	}

	public synchronized AuthView authenticate(LoginRequest request) {
		String identity = defaultText(request.email(), request.username());
		UserAccount user = users.stream()
				.filter(item -> item.email.equalsIgnoreCase(identity) || item.username.equalsIgnoreCase(identity))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Akun tidak ditemukan."));
		if (!user.password.equals(defaultText(request.password(), "")) || !"Aktif".equalsIgnoreCase(user.status)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kredensial tidak valid atau akun nonaktif.");
		}
		if (!isBlank(request.role()) && !normalizeRole(request.role()).equalsIgnoreCase(user.role)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Akun tidak sesuai dengan role yang dipilih.");
		}
		user.lastLogin = timestamp();
		return AuthView.from(user);
	}

	public synchronized AuthView authView(int id) {
		return AuthView.from(userById(id));
	}

	public synchronized void deleteUser(int id) {
		boolean removed = users.removeIf(user -> user.id == id);
		if (!removed) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User tidak ditemukan.");
		}
	}

	private Product productById(int id) {
		return products.stream()
				.filter(product -> product.id == id)
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produk tidak ditemukan."));
	}

	private Purchase purchaseById(int id) {
		return purchases.stream()
				.filter(purchase -> purchase.id == id)
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaksi pembelian tidak ditemukan."));
	}

	private Supplier supplierById(int id) {
		return suppliers.stream()
				.filter(supplier -> supplier.id == id)
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier tidak ditemukan."));
	}

	private UserAccount userById(int id) {
		return users.stream()
				.filter(user -> user.id == id)
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User tidak ditemukan."));
	}

	private Category categoryById(int id) {
		return categoryList.stream()
				.filter(category -> category.id == id)
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kategori tidak ditemukan."));
	}

	private void ensureCategory(String name) {
		if (categoryList.stream().noneMatch(category -> category.name.equalsIgnoreCase(name))) {
			categoryList.add(new Category(categorySequence.getAndIncrement(), name, iconFor(name)));
		}
	}

	private static Map<String, String> metric(String label, String value) {
		Map<String, String> metric = new LinkedHashMap<>();
		metric.put("label", label);
		metric.put("value", value);
		return metric;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private static String defaultText(String value, String fallback) {
		return isBlank(value) ? fallback : value.trim();
	}

	private static String normalizeRole(String role) {
		String value = defaultText(role, "Admin Kasir").trim();
		if (value.equalsIgnoreCase("Kasir")) return "Admin Kasir";
		if (value.equalsIgnoreCase("System Gudang") || value.equalsIgnoreCase("Gudang")) return "Admin Gudang";
		if (value.equalsIgnoreCase("Admin")) return "Owner";
		return value;
	}

	private static Map<String, PermissionSet> defaultPermissions(String role) {
		String normalized = normalizeRole(role);
		Set<String> allowed = switch (normalized) {
			case "Owner" -> Set.of("dashboard", "products", "stock", "suppliers", "purchases", "users");
			case "Admin Gudang" -> Set.of("dashboard", "products", "stock", "suppliers", "purchases");
			case "Admin Kasir" -> Set.of("sales");
			default -> Set.of("dashboard");
		};
		Map<String, PermissionSet> result = new LinkedHashMap<>();
		for (String menu : List.of("dashboard", "products", "stock", "suppliers", "purchases", "sales", "users")) {
			boolean menuAllowed = allowed.contains(menu);
			boolean destructive = "Owner".equals(normalized);
			result.put(menu, new PermissionSet(menuAllowed, menuAllowed, menuAllowed, menuAllowed && destructive, menuAllowed));
		}
		return result;
	}

	private static String timestamp() {
		return DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(LocalDateTime.now());
	}

	private static int positiveOrDefault(Integer value, int fallback) {
		return value == null || value < 0 ? fallback : value;
	}

	private static long positiveOrDefault(Long value, long fallback) {
		return value == null || value < 0 ? fallback : value;
	}

	private static int strictlyPositiveOrDefault(Integer value, int fallback) {
		return value == null || value <= 0 ? fallback : value;
	}

	private static long strictlyPositiveOrDefault(Long value, long fallback) {
		return value == null || value <= 0 ? fallback : value;
	}

	private static String status(Product product) {
		if (product.stock <= 0) {
			return "Habis";
		}
		if (product.stock <= product.minStock) {
			return "Stok Kritis";
		}
		return "Tersedia";
	}

	private static String iconFor(String category) {
		String normalized = category == null ? "" : category.toUpperCase(Locale.ROOT);
		if (normalized.contains("OLI")) return "oli";
		if (normalized.contains("BAN")) return "ban";
		if (normalized.contains("KAMPAS")) return "kampas";
		if (normalized.contains("AKI")) return "aki";
		if (normalized.contains("LAMPU")) return "lampu";
		return "mesin";
	}

	private static String rupiah(long value) {
		String raw = String.format(Locale.ROOT, "%,d", value).replace(",", ".");
		return "Rp " + raw;
	}

	public record ProductRequest(String name, String category, Long price, Integer stock, Integer minStock) {}
	public record ProductUpdateRequest(String name, String category, Long price, Integer stock, Integer minStock) {}
	public record CategoryRequest(String name) {}
	public record SupplierRequest(String name, String pic, String phone, String category, String address, String email) {}
	public record SupplierUpdateRequest(String name, String pic, String phone, String status, String category, String address, String email) {}
	public record StockOpnameRequest(Integer productId, Integer physicalStock) {}
	public record PurchaseRequest(String supplier, String item, String category, Integer quantity, Long unitPrice) {}
	public record InspectionRequest(Integer physicalQuantity, String note) {}
	public record SaleItemRequest(Integer productId, Integer quantity) {}
	public record SaleRequest(List<SaleItemRequest> items, String paymentMethod, Long paid) {}
	public record UserRequest(String name, String role, String username, String email, String status) {}
	public record UserUpdateRequest(String name, String role, String username, String email, String status) {}
	public record LoginRequest(String username, String email, String password, String role) {}
	public record PermissionRequest(Map<String, PermissionSet> permissions) {}
	public record PermissionSet(boolean page, boolean add, boolean edit, boolean delete, boolean export) {}
	public record AuthView(int id, String name, String username, String email, String role, String defaultPath, Map<String, PermissionSet> permissions) {
		static AuthView from(UserAccount user) {
			String path = "Admin Kasir".equalsIgnoreCase(user.role) ? "/transaksi-penjualan" : "/dashboard";
			return new AuthView(user.id, user.name, user.username, user.email, user.role, path, new LinkedHashMap<>(user.permissions));
		}
	}
	public record PriceChange(int productId, String productName, long oldPrice, long newPrice, String reason, String date) {}
	public record SupplierActivity(int supplierId, String title, String description, String time, String type) {}

	public record ProductView(int id, String name, String sku, String category, long price, String priceText, int stock, int minStock, int sold, String status, String icon) {
		static ProductView from(Product product) {
			return new ProductView(product.id, product.name, product.sku, product.category, product.price, rupiah(product.price), product.stock, product.minStock, product.sold, SipartDataStore.status(product), product.icon);
		}
	}

	public record CategorySummary(int id, String name, long count, String icon) {}
	public record BestSeller(String name, String sold, String profit) {}
	public record ProfitRow(String name, String sold, String revenue, String profit) {}

	public record StockCheckView(int id, String name, String sku, int systemStock, Integer physicalStock, Integer difference, String status) {
		static StockCheckView from(StockCheck check) {
			Integer difference = check.physicalStock == null ? null : check.physicalStock - check.systemStock;
			return new StockCheckView(check.id, check.name, check.sku, check.systemStock, check.physicalStock, difference, check.status);
		}
	}

	public record PurchaseView(int id, String code, String supplier, String item, String category, int quantity, Integer physicalQuantity, Integer difference, String inspectionNote, long unitPrice, long total, String totalText, String status, String date) {
		static PurchaseView from(Purchase purchase) {
			long total = (long) purchase.quantity * purchase.unitPrice;
			Integer difference = purchase.physicalQuantity == null ? null : purchase.physicalQuantity - purchase.quantity;
			return new PurchaseView(purchase.id, "#TX-" + purchase.id, purchase.supplier, purchase.item, purchase.category, purchase.quantity, purchase.physicalQuantity, difference, purchase.inspectionNote, purchase.unitPrice, total, rupiah(total), purchase.status, purchase.date);
		}
	}

	public record SaleView(int id, String code, List<SaleLine> items, long subtotal, long tax, long total, long paid, long change, String subtotalText, String taxText, String totalText, String paidText, String changeText, String paymentMethod, String date) {
		static SaleView from(Sale sale) {
			return new SaleView(sale.id, "#SL-" + sale.id, sale.items, sale.subtotal, sale.tax, sale.total, sale.paid, sale.paid - sale.total, rupiah(sale.subtotal), rupiah(sale.tax), rupiah(sale.total), rupiah(sale.paid), rupiah(sale.paid - sale.total), sale.paymentMethod, DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(sale.createdAt));
		}
	}

	private static class Category {
		int id;
		String name;
		String icon;

		Category(int id, String name, String icon) {
			this.id = id;
			this.name = name;
			this.icon = icon;
		}
	}

	public static class Product {
		public int id;
		public String name;
		public String sku;
		public String category;
		public long price;
		public long cost;
		public int stock;
		public int minStock;
		public int sold;
		public String icon;

		Product(int id, String name, String sku, String category, long price, long cost, int stock, int minStock, int sold, String icon) {
			this.id = id;
			this.name = name;
			this.sku = sku;
			this.category = category;
			this.price = price;
			this.cost = cost;
			this.stock = stock;
			this.minStock = minStock;
			this.sold = sold;
			this.icon = icon;
		}
	}

	public static class Supplier {
		public int id;
		public String name;
		public String pic;
		public String phone;
		public String status;
		public String logo;
		public String category;
		public String address;
		public String email;
		public String lastUsed;

		Supplier(int id, String name, String pic, String phone, String status, String logo, String category, String address, String email, String lastUsed) {
			this.id = id;
			this.name = name;
			this.pic = pic;
			this.phone = phone;
			this.status = status;
			this.logo = logo;
			this.category = category;
			this.address = address;
			this.email = email;
			this.lastUsed = lastUsed;
		}
	}

	public static class UserAccount {
		public int id;
		public String name;
		public String role;
		public String status;
		public String username;
		public String email;
		private String password;
		public String lastLogin;
		public Map<String, PermissionSet> permissions;

		UserAccount(int id, String name, String role, String status, String username, String email, String password, String lastLogin, Map<String, PermissionSet> permissions) {
			this.id = id;
			this.name = name;
			this.role = role;
			this.status = status;
			this.username = username;
			this.email = email;
			this.password = password;
			this.lastLogin = lastLogin;
			this.permissions = permissions;
		}
	}

	private static class Purchase {
		int id;
		String supplier;
		String item;
		String category;
		int quantity;
		long unitPrice;
		String status;
		String date;
		Integer physicalQuantity;
		String inspectionNote;

		Purchase(int id, String supplier, String item, String category, int quantity, long unitPrice, String status, String date) {
			this.id = id;
			this.supplier = supplier;
			this.item = item;
			this.category = category;
			this.quantity = quantity;
			this.unitPrice = unitPrice;
			this.status = status;
			this.date = date;
		}
	}

	private static class StockCheck {
		int id;
		String name;
		String sku;
		int systemStock;
		Integer physicalStock;
		String status;

		StockCheck(int id, String name, String sku, int systemStock, Integer physicalStock, String status) {
			this.id = id;
			this.name = name;
			this.sku = sku;
			this.systemStock = systemStock;
			this.physicalStock = physicalStock;
			this.status = status;
		}
	}

	public record SaleLine(int productId, String name, int quantity, long price) {}

	private static class Sale {
		int id;
		List<SaleLine> items;
		long subtotal;
		long tax;
		long total;
		long paid;
		String paymentMethod;
		LocalDateTime createdAt;

		Sale(int id, List<SaleLine> items, long subtotal, long tax, long total, long paid, String paymentMethod, LocalDateTime createdAt) {
			this.id = id;
			this.items = items;
			this.subtotal = subtotal;
			this.tax = tax;
			this.total = total;
			this.paid = paid;
			this.paymentMethod = paymentMethod;
			this.createdAt = createdAt;
		}
	}
}
