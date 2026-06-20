package com.TugasBesar.demo.service;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.TugasBesar.demo.domain.Product;
import com.TugasBesar.demo.domain.Purchase;
import com.TugasBesar.demo.domain.SaleItem;
import com.TugasBesar.demo.domain.StockOpname;
import com.TugasBesar.demo.domain.user.UserFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Business service for SIPART. All mutable state is persisted through JdbcTemplate;
 * the nested records are kept as stable API contracts for the existing frontend.
 */
@Service
public class SipartService {
	private static final List<String> MENUS = List.of(
			"dashboard", "products", "stock", "suppliers", "purchases", "sales", "users");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

	private final JdbcTemplate jdbc;

	public SipartService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	// Dashboard and owner reports
	@Transactional(readOnly = true)
	public Map<String, Object> dashboard() {
		return dashboard("all", null, null);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> dashboard(String period, LocalDate from, LocalDate to) {
		List<Product> allProducts = loadProducts();
		DateRange range = resolveRange(period, from, to);
		PeriodSummary summary = salesSummary(range);
		List<ReportProduct> reportProducts = reportProducts(range);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("metrics", List.of(
				metric("Total Pendapatan", rupiah(summary.revenue())),
				metric("Total Modal", rupiah(summary.capital())),
				metric("Total Keuntungan", rupiah(summary.profit())),
				metric("Total Produk Terjual", String.valueOf(summary.sold()))));
		response.put("lowStock", allProducts.stream()
				.filter(product -> product.stock <= product.minimumStock)
				.sorted(Comparator.comparingInt(product -> product.stock))
				.map(ProductView::from)
				.toList());
		response.put("bestSellers", reportProducts.stream()
				.limit(5)
				.map(product -> new BestSeller(product.name(), product.sold() + " Unit", rupiah(product.profit())))
				.toList());
		response.put("profits", reportProducts.stream()
				.limit(4)
				.map(product -> new ProfitRow(product.name(), product.sold() + " Unit",
						rupiah(product.revenue()), rupiah(product.profit())))
				.toList());
		response.put("purchaseHistories", purchases(null, null).stream().limit(5).toList());
		response.put("recentSales", sales(range).stream().limit(5).toList());
		response.put("totalTransactions", summary.transactions());
		response.put("period", range.label());
		response.put("from", range.from() == null ? null : range.from().toLocalDate().toString());
		response.put("to", range.toExclusive() == null ? null : range.toExclusive().minusDays(1).toLocalDate().toString());
		return response;
	}

	// Product catalog, categories, and price history
	@Transactional(readOnly = true)
	public List<ProductView> products(String query, String category, String status) {
		return loadProducts().stream()
				.filter(product -> isBlank(query)
						|| containsIgnoreCase(product.name, query)
						|| containsIgnoreCase(product.sku, query))
				.filter(product -> isBlank(category)
						|| "Semua Kategori".equalsIgnoreCase(category)
						|| product.category.equalsIgnoreCase(category))
				.filter(product -> isBlank(status)
						|| "Semua Status".equalsIgnoreCase(status)
						|| status(product).equalsIgnoreCase(status))
				.map(ProductView::from)
				.toList();
	}

	@Transactional
	public ProductView createProduct(ProductRequest request) {
		if (isBlank(request.name())) {
			throw badRequest("Nama produk wajib diisi.");
		}
		String category = normalizedCategory(request.category());
		int categoryId = ensureCategory(category);
		long price = strictlyPositiveOrDefault(request.price(), 50_000);
		int stock = positiveOrDefault(request.stock(), 0);
		int minStock = positiveOrDefault(request.minStock(), 10);
		long cost = Math.max(1, Math.round(price * 0.62f));
		String icon = iconFor(category);
		String sku = category.substring(0, Math.min(3, category.length()))
				+ "-" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);

		int id = insertAndReturnId("""
				INSERT INTO products
				(category_id, name, sku, selling_price, cost_price, stock, minimum_stock, sold, icon, status)
				VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
				""", categoryId, request.name().trim(), sku, price, cost, stock, minStock, icon,
				status(stock, minStock));
		jdbc.update("""
				INSERT INTO price_histories (product_id, old_price, new_price, reason)
				VALUES (?, 0, ?, 'Harga awal')
				""", id, price);
		return ProductView.from(productById(id));
	}

	@Transactional
	public ProductView updateProduct(int id, ProductUpdateRequest request) {
		Product product = productById(id);
		long oldPrice = product.sellingPrice;
		String name = isBlank(request.name()) ? product.name : request.name().trim();
		String category = isBlank(request.category()) ? product.category : normalizedCategory(request.category());
		long price = request.price() != null && request.price() > 0 ? request.price() : product.sellingPrice;
		int stock = request.stock() != null && request.stock() >= 0 ? request.stock() : product.stock;
		int minStock = request.minStock() != null && request.minStock() >= 0 ? request.minStock() : product.minimumStock;
		int categoryId = ensureCategory(category);

		jdbc.update("""
				UPDATE products SET category_id = ?, name = ?, selling_price = ?, stock = ?,
				minimum_stock = ?, icon = ?, status = ?, updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", categoryId, name, price, stock, minStock, iconFor(category), status(stock, minStock), id);
		if (oldPrice != price) {
			jdbc.update("""
					INSERT INTO price_histories (product_id, old_price, new_price, reason)
					VALUES (?, ?, ?, 'Pembaruan harga jual')
					""", id, oldPrice, price);
		}
		return ProductView.from(productById(id));
	}

	@Transactional
	public void deleteProduct(int id) {
		if (jdbc.update("DELETE FROM products WHERE id = ?", id) == 0) {
			throw notFound("Produk tidak ditemukan.");
		}
	}

	@Transactional(readOnly = true)
	public List<CategorySummary> categories() {
		return jdbc.query("""
				SELECT c.id, c.name, c.icon, COUNT(p.id) product_count
				FROM categories c LEFT JOIN products p ON p.category_id = c.id
				GROUP BY c.id, c.name, c.icon ORDER BY c.id
				""", (rs, row) -> new CategorySummary(
				rs.getInt("id"), rs.getString("name"), rs.getLong("product_count"), rs.getString("icon")));
	}

	@Transactional
	public CategorySummary createCategory(CategoryRequest request) {
		String name = requiredCategoryName(request);
		try {
			int id = insertAndReturnId("INSERT INTO categories (name, icon) VALUES (?, ?)", name, iconFor(name));
			return new CategorySummary(id, name, 0, iconFor(name));
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Kategori sudah tersedia.");
		}
	}

	@Transactional
	public CategorySummary updateCategory(int id, CategoryRequest request) {
		categoryById(id);
		String name = requiredCategoryName(request);
		try {
			jdbc.update("UPDATE categories SET name = ?, icon = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
					name, iconFor(name), id);
			jdbc.update("UPDATE products SET icon = ?, updated_at = CURRENT_TIMESTAMP WHERE category_id = ?",
					iconFor(name), id);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Kategori sudah tersedia.");
		}
		return categories().stream().filter(category -> category.id() == id).findFirst()
				.orElseThrow(() -> notFound("Kategori tidak ditemukan."));
	}

	@Transactional
	public void deleteCategory(int id) {
		categoryById(id);
		try {
			jdbc.update("DELETE FROM categories WHERE id = ?", id);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Kategori masih digunakan oleh produk.");
		}
	}

	@Transactional(readOnly = true)
	public List<PriceChange> priceHistory(Integer productId) {
		String sql = """
				SELECT ph.product_id, p.name product_name, ph.old_price, ph.new_price,
				ph.reason, ph.changed_at
				FROM price_histories ph JOIN products p ON p.id = ph.product_id
				""" + (productId == null ? "" : " WHERE ph.product_id = ?") + " ORDER BY ph.changed_at DESC, ph.id DESC";
		Object[] arguments = productId == null ? new Object[0] : new Object[] { productId };
		return jdbc.query(sql, (rs, row) -> new PriceChange(
				rs.getInt("product_id"), rs.getString("product_name"), rs.getLong("old_price"),
				rs.getLong("new_price"), rs.getString("reason"), formatDateTime(rs.getTimestamp("changed_at"))),
				arguments);
	}

	// Physical inventory and stock opname
	@Transactional(readOnly = true)
	public List<StockCheckView> stockChecks() {
		return stockChecks(null, null);
	}

	@Transactional(readOnly = true)
	public List<StockCheckView> stockChecks(String query, String status) {
		return jdbc.query("""
				SELECT id, item_name, sku, system_stock, physical_stock, difference, status
				FROM stock_opnames ORDER BY checked_at DESC, id DESC
				""", (rs, row) -> new StockCheckView(
				rs.getInt("id"), rs.getString("item_name"), rs.getString("sku"),
				rs.getInt("system_stock"), nullableInteger(rs, "physical_stock"),
				nullableInteger(rs, "difference"), rs.getString("status"))).stream()
				.filter(check -> isBlank(query)
						|| containsIgnoreCase(check.name(), query)
						|| containsIgnoreCase(check.sku(), query))
				.filter(check -> isBlank(status) || check.status().equalsIgnoreCase(status))
				.toList();
	}

	@Transactional
	public StockCheckView createStockCheck(StockOpnameRequest request) {
		if (request.productId() == null) {
			throw badRequest("Barang wajib dipilih.");
		}
		if (request.physicalStock() == null || request.physicalStock() < 0) {
			throw badRequest("Stok fisik wajib diisi.");
		}
		Product product = productById(request.productId());
		StockOpname opname = new StockOpname(
				0, product.name, product.sku, product.stock, request.physicalStock());
		jdbc.update("""
				UPDATE products SET stock = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
				""", request.physicalStock(), status(request.physicalStock(), product.minimumStock), product.id);
		int id = insertAndReturnId("""
				INSERT INTO stock_opnames
				(product_id, item_name, sku, system_stock, physical_stock, difference, status)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""", product.id, product.name, product.sku, product.stock, request.physicalStock(),
				opname.difference(), opname.status());
		return stockChecks(null, null).stream().filter(check -> check.id() == id).findFirst()
				.orElseThrow(() -> notFound("Stock opname tidak ditemukan."));
	}

	@Transactional
	public StockCheckView updateStockCheck(int id, StockOpnameUpdateRequest request) {
		if (request.physicalStock() == null || request.physicalStock() < 0) {
			throw badRequest("Jumlah stock opname wajib diisi.");
		}
		StockOpnameTarget target = jdbc.query("""
				SELECT so.product_id, so.item_name, so.sku, so.system_stock,
				       COALESCE(p.minimum_stock, 0) AS minimum_stock
				FROM stock_opnames so
				LEFT JOIN products p ON p.id = so.product_id
				WHERE so.id = ?
				""", (rs, row) -> new StockOpnameTarget(
				nullableInteger(rs, "product_id"), rs.getString("item_name"), rs.getString("sku"),
				rs.getInt("system_stock"), rs.getInt("minimum_stock")), id).stream().findFirst()
				.orElseThrow(() -> notFound("Stock opname tidak ditemukan."));
		StockOpname opname = new StockOpname(
				id, target.name(), target.sku(), target.systemStock(), request.physicalStock());
		jdbc.update("""
				UPDATE stock_opnames
				SET physical_stock = ?, difference = ?, status = ?, checked_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", request.physicalStock(), opname.difference(), opname.status(), id);
		if (target.productId() != null) {
			jdbc.update("""
					UPDATE products SET stock = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
					""", request.physicalStock(), status(request.physicalStock(), target.minimumStock()),
					target.productId());
		}
		return stockChecks(null, null).stream().filter(check -> check.id() == id).findFirst()
				.orElseThrow(() -> notFound("Stock opname tidak ditemukan."));
	}

	// Supplier management and activity history
	@Transactional(readOnly = true)
	public List<Supplier> suppliers() {
		return suppliers(null, null);
	}

	@Transactional(readOnly = true)
	public List<Supplier> suppliers(String query, String status) {
		return loadSuppliers().stream()
				.filter(supplier -> isBlank(query)
						|| containsIgnoreCase(supplier.name, query)
						|| containsIgnoreCase(supplier.pic, query))
				.filter(supplier -> isBlank(status) || supplier.status.equalsIgnoreCase(status))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SupplierActivity> supplierActivities() {
		return jdbc.query("""
				SELECT supplier_id, title, description, activity_type, created_at
				FROM supplier_activities ORDER BY created_at DESC, id DESC
				""", (rs, row) -> new SupplierActivity(
				rs.getInt("supplier_id"), rs.getString("title"), rs.getString("description"),
				formatDateTime(rs.getTimestamp("created_at")), rs.getString("activity_type")));
	}

	@Transactional
	public Supplier createSupplier(SupplierRequest request) {
		if (isBlank(request.name())) {
			throw badRequest("Nama supplier wajib diisi.");
		}
		String name = request.name().trim();
		String logo = logoFor(name);
		int id = insertAndReturnId("""
				INSERT INTO suppliers (name, pic, phone, email, address, category, logo, status, last_used_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, 'AKTIF', CURRENT_TIMESTAMP)
				""", name, defaultText(request.pic(), "PIC Supplier"),
				defaultText(request.phone(), "081234567890"),
				defaultText(request.email(), "supplier@sipart.test"),
				defaultText(request.address(), "Alamat belum diisi"),
				defaultText(request.category(), "Sparepart Umum"), logo);
		addSupplierActivity(id, "Supplier Baru Ditambahkan", name + " terdaftar di sistem.", "orange");
		return supplierById(id);
	}

	@Transactional
	public Supplier updateSupplier(int id, SupplierUpdateRequest request) {
		Supplier supplier = supplierById(id);
		String name = defaultText(request.name(), supplier.name);
		String pic = defaultText(request.pic(), supplier.pic);
		String phone = defaultText(request.phone(), supplier.phone);
		String email = defaultText(request.email(), supplier.email);
		String address = defaultText(request.address(), supplier.address);
		String category = defaultText(request.category(), supplier.category);
		String status = defaultText(request.status(), supplier.status).toUpperCase(Locale.ROOT);
		jdbc.update("""
				UPDATE suppliers SET name = ?, pic = ?, phone = ?, email = ?, address = ?, category = ?,
				status = ?, logo = ?, last_used_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", name, pic, phone, email, address, category, status, logoFor(name), id);
		addSupplierActivity(id, "Pembaruan Profil", "Kontak " + name + " diperbarui.", "soft");
		return supplierById(id);
	}

	@Transactional
	public void deleteSupplier(int id) {
		Supplier supplier = supplierById(id);
		jdbc.update("UPDATE supplier_activities SET supplier_id = NULL WHERE supplier_id = ?", id);
		if (jdbc.update("DELETE FROM suppliers WHERE id = ?", id) == 0) {
			throw notFound("Supplier tidak ditemukan.");
		}
		addSupplierActivity(null, "Supplier Dihapus", supplier.name + " dihapus dari sistem.", "red");
	}

	// Purchase ordering, inspection, and stock confirmation
	@Transactional(readOnly = true)
	public List<PurchaseView> purchases() {
		return purchases(null, null);
	}

	@Transactional(readOnly = true)
	public List<PurchaseView> purchases(String query, String status) {
		return jdbc.query("""
				SELECT id, supplier_name, item_name, category_name, quantity, physical_quantity,
				inspection_note, unit_price, status, purchase_date
				FROM purchases ORDER BY id DESC
				""", (rs, row) -> purchaseView(
				rs.getInt("id"), rs.getString("supplier_name"), rs.getString("item_name"),
				rs.getString("category_name"), rs.getInt("quantity"),
				nullableInteger(rs, "physical_quantity"), rs.getString("inspection_note"),
				rs.getLong("unit_price"), rs.getString("status"), rs.getTimestamp("purchase_date"))).stream()
				.filter(purchase -> isBlank(query)
						|| containsIgnoreCase(purchase.supplier(), query)
						|| containsIgnoreCase(purchase.item(), query))
				.filter(purchase -> isBlank(status) || purchase.status().equalsIgnoreCase(status))
				.toList();
	}

	@Transactional
	public PurchaseView createPurchase(PurchaseRequest request) {
		if (isBlank(request.supplier()) || isBlank(request.item())) {
			throw badRequest("Supplier dan barang wajib diisi.");
		}
		Integer supplierId = findId("SELECT id FROM suppliers WHERE LOWER(name) = LOWER(?)", request.supplier().trim());
		Integer productId = findId("SELECT id FROM products WHERE LOWER(name) = LOWER(?)", request.item().trim());
		int id = insertAndReturnId("""
				INSERT INTO purchases
				(supplier_id, product_id, supplier_name, item_name, category_name, quantity, unit_price, status)
				VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending')
				""", supplierId, productId, request.supplier().trim(), request.item().trim(),
				defaultText(request.category(), "UMUM"), strictlyPositiveOrDefault(request.quantity(), 1),
				strictlyPositiveOrDefault(request.unitPrice(), 1));
		return purchaseById(id);
	}

	@Transactional
	public PurchaseView inspectPurchase(int id, InspectionRequest request) {
		purchaseById(id);
		if (request.physicalQuantity() == null || request.physicalQuantity() < 0) {
			throw badRequest("Jumlah fisik wajib diisi.");
		}
		Integer ordered = jdbc.queryForObject("SELECT quantity FROM purchases WHERE id = ?", Integer.class, id);
		String status = ordered != null && ordered.equals(request.physicalQuantity()) ? "Barang Datang" : "Selisih";
		jdbc.update("""
				UPDATE purchases SET physical_quantity = ?, inspection_note = ?, status = ?,
				updated_at = CURRENT_TIMESTAMP WHERE id = ?
				""", request.physicalQuantity(), defaultText(request.note(), "Pemeriksaan fisik tersimpan"), status, id);
		return purchaseById(id);
	}

	@Transactional
	public PurchaseView confirmPurchase(int id) {
		PurchaseView purchase = purchaseById(id);
		if ("Selesai".equalsIgnoreCase(purchase.status())) {
			return purchase;
		}
		Integer productId = findId("SELECT product_id FROM purchases WHERE id = ?", id);
		if (productId == null) {
			productId = findId("SELECT id FROM products WHERE LOWER(name) = LOWER(?)", purchase.item());
		}
		int accepted = purchase.physicalQuantity() == null ? purchase.quantity() : purchase.physicalQuantity();
		if (productId != null) {
			jdbc.update("""
					UPDATE products SET stock = stock + ?,
					status = CASE WHEN stock + ? <= 0 THEN 'Habis'
					              WHEN stock + ? <= minimum_stock THEN 'Stok Kritis'
					              ELSE 'Tersedia' END,
					updated_at = CURRENT_TIMESTAMP WHERE id = ?
					""", accepted, accepted, accepted, productId);
		}
		jdbc.update("UPDATE purchases SET status = 'Selesai', updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
		Integer supplierId = findId("SELECT supplier_id FROM purchases WHERE id = ?", id);
		if (supplierId != null) {
			jdbc.update("UPDATE suppliers SET last_used_at = CURRENT_TIMESTAMP WHERE id = ?", supplierId);
		}
		addSupplierActivity(supplierId, "Pembelian Selesai",
				"#TX-" + id + " dari " + purchase.supplier() + " dikonfirmasi.", "blue");
		return purchaseById(id);
	}

	@Transactional
	public void deletePurchase(int id) {
		PurchaseView purchase = purchaseById(id);
		if ("Selesai".equalsIgnoreCase(purchase.status())) {
			throw conflict("Transaksi yang selesai tidak dapat dihapus.");
		}
		jdbc.update("DELETE FROM purchases WHERE id = ?", id);
	}

	// Point of sale, payment, receipt, and automatic stock out
	@Transactional(readOnly = true)
	public List<ProductView> salesProducts() {
		return loadProducts().stream().map(ProductView::from).toList();
	}

	@Transactional(readOnly = true)
	public List<SaleView> sales() {
		return sales(DateRange.all());
	}

	private List<SaleView> sales(DateRange range) {
		String sql = """
				SELECT id, subtotal, tax, total, paid, payment_method, sale_date
				FROM sales
				""" + range.where("sale_date") + " ORDER BY sale_date DESC, id DESC";
		return jdbc.query(sql, (rs, row) -> saleView(
				rs.getInt("id"), rs.getLong("subtotal"), rs.getLong("tax"), rs.getLong("total"),
				rs.getLong("paid"), rs.getString("payment_method"), rs.getTimestamp("sale_date")), range.arguments());
	}

	@Transactional(readOnly = true)
	public SaleView sale(int id) {
		return sales().stream().filter(sale -> sale.id() == id).findFirst()
				.orElseThrow(() -> notFound("Transaksi penjualan tidak ditemukan."));
	}

	@Transactional
	public SaleView createSale(SaleRequest request) {
		if (request.items() == null || request.items().isEmpty()) {
			throw badRequest("Keranjang masih kosong.");
		}
		Map<Integer, Integer> quantities = new LinkedHashMap<>();
		for (SaleItemRequest item : request.items()) {
			if (item.productId() == null) {
				throw badRequest("Produk transaksi wajib dipilih.");
			}
			quantities.merge(item.productId(), strictlyPositiveOrDefault(item.quantity(), 1), Integer::sum);
		}

		List<SaleItem> lines = new ArrayList<>();
		long subtotal = 0;
		for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
			Product product = productById(entry.getKey());
			if (product.stock < entry.getValue()) {
				throw badRequest("Stok " + product.name + " tidak cukup.");
			}
			SaleItem line = new SaleItem(product.id, product.name, entry.getValue(), product.sellingPrice);
			lines.add(line);
			subtotal += line.subtotal();
		}

		long tax = Math.round(subtotal * 0.11);
		long total = subtotal + tax;
		long paid = strictlyPositiveOrDefault(request.paid(), total);
		if (paid < total) {
			throw badRequest("Jumlah pembayaran kurang dari total transaksi.");
		}

		int saleId = insertAndReturnId("""
				INSERT INTO sales (subtotal, tax, total, paid, payment_method)
				VALUES (?, ?, ?, ?, ?)
				""", subtotal, tax, total, paid, defaultText(request.paymentMethod(), "Tunai"));
		for (SaleItem line : lines) {
			jdbc.update("""
					UPDATE products SET stock = stock - ?, sold = sold + ?,
					status = CASE WHEN stock - ? <= 0 THEN 'Habis'
					              WHEN stock - ? <= minimum_stock THEN 'Stok Kritis'
					              ELSE 'Tersedia' END,
					updated_at = CURRENT_TIMESTAMP WHERE id = ?
					""", line.quantity(), line.quantity(), line.quantity(), line.quantity(), line.productId());
			jdbc.update("""
					INSERT INTO sale_details (sale_id, product_id, product_name, quantity, price, subtotal)
					VALUES (?, ?, ?, ?, ?, ?)
					""", saleId, line.productId(), line.name(), line.quantity(), line.price(),
					line.subtotal());
		}
		return sale(saleId);
	}

	// User accounts, role permissions, and authentication
	@Transactional(readOnly = true)
	public List<UserAccount> users() {
		return jdbc.query("""
				SELECT id, name, role, status, username, email, password, last_login
				FROM users ORDER BY id
				""", (rs, row) -> mapUser(rs.getInt("id"), rs.getString("name"), rs.getString("role"),
				rs.getString("status"), rs.getString("username"), rs.getString("email"),
				rs.getString("password"), rs.getTimestamp("last_login")));
	}

	@Transactional
	public UserAccount createUser(UserRequest request) {
		if (isBlank(request.name())) {
			throw badRequest("Nama user wajib diisi.");
		}
		String role = normalizeRole(request.role());
		String username = defaultText(request.username(),
				request.name().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_"));
		String email = defaultText(request.email(), username + "@sipart.test");
		try {
			int id = insertAndReturnId("""
					INSERT INTO users (name, role, status, username, email, password)
					VALUES (?, ?, ?, ?, ?, 'demo')
					""", request.name().trim(), role, defaultText(request.status(), "Aktif"), username, email);
			replacePermissions(id, defaultPermissions(role));
			return userById(id);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Username atau email sudah digunakan.");
		}
	}

	@Transactional
	public UserAccount updateUser(int id, UserUpdateRequest request) {
		UserAccount user = userById(id);
		String role = defaultText(request.role(), user.role);
		role = normalizeRole(role);
		try {
			jdbc.update("""
					UPDATE users SET name = ?, role = ?, status = ?, username = ?, email = ?,
					updated_at = CURRENT_TIMESTAMP WHERE id = ?
					""", defaultText(request.name(), user.name), role,
					defaultText(request.status(), user.status), defaultText(request.username(), user.username),
					defaultText(request.email(), user.email), id);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Username atau email sudah digunakan.");
		}
		if (!user.role.equalsIgnoreCase(role)) {
			replacePermissions(id, defaultPermissions(role));
		}
		return userById(id);
	}

	@Transactional
	public UserAccount resetUser(int id) {
		UserAccount user = userById(id);
		jdbc.update("""
				UPDATE users SET status = 'Aktif', password = 'demo', last_login = NULL,
				updated_at = CURRENT_TIMESTAMP WHERE id = ?
				""", id);
		replacePermissions(id, defaultPermissions(user.role));
		return userById(id);
	}

	@Transactional(readOnly = true)
	public Map<String, PermissionSet> userPermissions(int id) {
		userById(id);
		Map<String, PermissionSet> permissions = new LinkedHashMap<>();
		jdbc.query("""
				SELECT menu_name, page_access, can_add, can_edit, can_delete, can_export
				FROM user_permissions WHERE user_id = ? ORDER BY id
				""", (RowCallbackHandler) rs -> permissions.put(rs.getString("menu_name"), new PermissionSet(
				rs.getBoolean("page_access"), rs.getBoolean("can_add"), rs.getBoolean("can_edit"),
				rs.getBoolean("can_delete"), rs.getBoolean("can_export"))), id);
		return permissions;
	}

	@Transactional
	public Map<String, PermissionSet> updateUserPermissions(int id, PermissionRequest request) {
		userById(id);
		if (request.permissions() == null) {
			throw badRequest("Data permission wajib diisi.");
		}
		replacePermissions(id, request.permissions());
		return userPermissions(id);
	}

	@Transactional
	public AuthView authenticate(LoginRequest request) {
		String identity = defaultText(request.email(), request.username());
		List<UserAccount> matches = jdbc.query("""
				SELECT id, name, role, status, username, email, password, last_login
				FROM users WHERE LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?)
				""", (rs, row) -> mapUser(rs.getInt("id"), rs.getString("name"), rs.getString("role"),
				rs.getString("status"), rs.getString("username"), rs.getString("email"),
				rs.getString("password"), rs.getTimestamp("last_login")), identity, identity);
		if (matches.isEmpty()) {
			throw unauthorized("Akun tidak ditemukan.");
		}
		UserAccount user = matches.get(0);
		if (!user.password.equals(defaultText(request.password(), "")) || !"Aktif".equalsIgnoreCase(user.status)) {
			throw unauthorized("Kredensial tidak valid atau akun nonaktif.");
		}
		if (!isBlank(request.role()) && !normalizeRole(request.role()).equalsIgnoreCase(user.role)) {
			throw unauthorized("Akun tidak sesuai dengan role yang dipilih.");
		}
		jdbc.update("UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?", user.id);
		return authView(user.id);
	}

	@Transactional(readOnly = true)
	public AuthView authView(int id) {
		return AuthView.from(userById(id));
	}

	@Transactional
	public void deleteUser(int id) {
		if (jdbc.update("DELETE FROM users WHERE id = ?", id) == 0) {
			throw notFound("User tidak ditemukan.");
		}
	}

	private PeriodSummary salesSummary(DateRange range) {
		String salesSql = "SELECT COALESCE(SUM(subtotal), 0), COUNT(*) FROM sales"
				+ range.where("sale_date");
		PeriodSummary sales = jdbc.queryForObject(salesSql, (rs, row) ->
				new PeriodSummary(rs.getLong(1), 0, 0, rs.getInt(2)), range.arguments());

		String detailSql = """
				SELECT COALESCE(SUM(sd.quantity * COALESCE(p.cost_price, 0)), 0),
				       COALESCE(SUM(sd.quantity), 0)
				FROM sale_details sd
				JOIN sales s ON s.id = sd.sale_id
				LEFT JOIN products p ON p.id = sd.product_id
				""" + range.where("s.sale_date");
		long[] details = jdbc.queryForObject(detailSql,
				(rs, row) -> new long[] { rs.getLong(1), rs.getLong(2) }, range.arguments());
		return new PeriodSummary(sales == null ? 0 : sales.revenue(), details == null ? 0 : details[0],
				details == null ? 0 : (int) details[1], sales == null ? 0 : sales.transactions());
	}

	private List<ReportProduct> reportProducts(DateRange range) {
		String sql = """
				SELECT sd.product_name,
				       SUM(sd.quantity) sold,
				       SUM(sd.subtotal) revenue,
				       SUM(sd.subtotal - (sd.quantity * COALESCE(p.cost_price, 0))) profit
				FROM sale_details sd
				JOIN sales s ON s.id = sd.sale_id
				LEFT JOIN products p ON p.id = sd.product_id
				""" + range.where("s.sale_date") + """
				 GROUP BY sd.product_name
				 ORDER BY sold DESC, revenue DESC
				""";
		return jdbc.query(sql, (rs, row) -> new ReportProduct(
				rs.getString("product_name"), rs.getInt("sold"),
				rs.getLong("revenue"), rs.getLong("profit")), range.arguments());
	}

	private static DateRange resolveRange(String period, LocalDate from, LocalDate to) {
		if (from != null || to != null) {
			LocalDate start = from == null ? to : from;
			LocalDate end = to == null ? start : to;
			if (end.isBefore(start)) throw badRequest("Tanggal akhir tidak boleh sebelum tanggal awal.");
			return DateRange.between("custom", start, end);
		}

		LocalDate today = LocalDate.now();
		return switch (defaultText(period, "all").toLowerCase(Locale.ROOT)) {
			case "daily", "harian" -> DateRange.between("daily", today, today);
			case "weekly", "mingguan" -> {
				LocalDate start = today.with(DayOfWeek.MONDAY);
				yield DateRange.between("weekly", start, start.plusDays(6));
			}
			case "monthly", "bulanan" -> {
				YearMonth month = YearMonth.from(today);
				yield DateRange.between("monthly", month.atDay(1), month.atEndOfMonth());
			}
			case "yearly", "tahunan" -> DateRange.between(
					"yearly", LocalDate.of(today.getYear(), 1, 1), LocalDate.of(today.getYear(), 12, 31));
			default -> DateRange.all();
		};
	}

	private List<Product> loadProducts() {
		return jdbc.query("""
				SELECT p.id, p.name, p.sku, c.name category, p.selling_price, p.cost_price,
				p.stock, p.minimum_stock, p.sold, p.icon
				FROM products p JOIN categories c ON c.id = p.category_id ORDER BY p.id
				""", (rs, row) -> new Product(
				rs.getInt("id"), rs.getString("name"), rs.getString("sku"), rs.getString("category"),
				rs.getLong("selling_price"), rs.getLong("cost_price"), rs.getInt("stock"),
				rs.getInt("minimum_stock"), rs.getInt("sold"), rs.getString("icon")));
	}

	private Product productById(int id) {
		return loadProducts().stream().filter(product -> product.id == id).findFirst()
				.orElseThrow(() -> notFound("Produk tidak ditemukan."));
	}

	private List<Supplier> loadSuppliers() {
		return jdbc.query("""
				SELECT id, name, pic, phone, status, logo, category, address, email, last_used_at
				FROM suppliers ORDER BY id
				""", (rs, row) -> new Supplier(
				rs.getInt("id"), rs.getString("name"), rs.getString("pic"), rs.getString("phone"),
				rs.getString("status"), rs.getString("logo"), rs.getString("category"),
				rs.getString("address"), rs.getString("email"), formatLastUsed(rs.getTimestamp("last_used_at"))));
	}

	private Supplier supplierById(int id) {
		return loadSuppliers().stream().filter(supplier -> supplier.id == id).findFirst()
				.orElseThrow(() -> notFound("Supplier tidak ditemukan."));
	}

	private PurchaseView purchaseById(int id) {
		return purchases(null, null).stream().filter(purchase -> purchase.id() == id).findFirst()
				.orElseThrow(() -> notFound("Transaksi pembelian tidak ditemukan."));
	}

	private UserAccount userById(int id) {
		return users().stream().filter(user -> user.id == id).findFirst()
				.orElseThrow(() -> notFound("User tidak ditemukan."));
	}

	private void categoryById(int id) {
		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM categories WHERE id = ?", Integer.class, id);
		if (count == null || count == 0) {
			throw notFound("Kategori tidak ditemukan.");
		}
	}

	private int ensureCategory(String name) {
		Integer existing = findId("SELECT id FROM categories WHERE UPPER(name) = UPPER(?)", name);
		return existing != null ? existing
				: insertAndReturnId("INSERT INTO categories (name, icon) VALUES (?, ?)", name, iconFor(name));
	}

	private void replacePermissions(int userId, Map<String, PermissionSet> permissions) {
		jdbc.update("DELETE FROM user_permissions WHERE user_id = ?", userId);
		permissions.forEach((menu, permission) -> jdbc.update("""
				INSERT INTO user_permissions
				(user_id, menu_name, page_access, can_add, can_edit, can_delete, can_export)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""", userId, menu, permission.page(), permission.add(), permission.edit(),
				permission.delete(), permission.export()));
	}

	private void addSupplierActivity(Integer supplierId, String title, String description, String type) {
		jdbc.update("""
				INSERT INTO supplier_activities (supplier_id, title, description, activity_type)
				VALUES (?, ?, ?, ?)
				""", supplierId, title, description, type);
	}

	private SaleView saleView(int id, long subtotal, long tax, long total, long paid,
			String paymentMethod, Timestamp saleDate) {
		List<SaleLine> lines = jdbc.query("""
				SELECT product_id, product_name, quantity, price FROM sale_details
				WHERE sale_id = ? ORDER BY id
				""", (rs, row) -> new SaleLine(
				rs.getInt("product_id"), rs.getString("product_name"), rs.getInt("quantity"), rs.getLong("price")), id);
		return new SaleView(id, "#SL-" + id, lines, subtotal, tax, total, paid, paid - total,
				rupiah(subtotal), rupiah(tax), rupiah(total), rupiah(paid), rupiah(paid - total),
				paymentMethod, formatDateTime(saleDate));
	}

	private static PurchaseView purchaseView(int id, String supplier, String item, String category,
			int quantity, Integer physicalQuantity, String note, long unitPrice, String status, Timestamp date) {
		Purchase purchase = new Purchase(id, supplier, item, category, quantity, physicalQuantity,
				note, unitPrice, status, formatDate(date));
		return new PurchaseView(id, "#TX-" + id, supplier, item, category, quantity, physicalQuantity,
				purchase.difference(), note, unitPrice, purchase.total(), rupiah(purchase.total()), status, purchase.date());
	}

	private UserAccount mapUser(int id, String name, String role, String status, String username,
			String email, String password, Timestamp lastLogin) {
		return new UserAccount(id, name, role, status, username, email, password,
				lastLogin == null ? "Belum pernah login" : formatDateTime(lastLogin), userPermissionsDirect(id));
	}

	private Map<String, PermissionSet> userPermissionsDirect(int userId) {
		Map<String, PermissionSet> permissions = new LinkedHashMap<>();
		jdbc.query("""
				SELECT menu_name, page_access, can_add, can_edit, can_delete, can_export
				FROM user_permissions WHERE user_id = ? ORDER BY id
				""", (RowCallbackHandler) rs -> permissions.put(rs.getString("menu_name"), new PermissionSet(
				rs.getBoolean("page_access"), rs.getBoolean("can_add"), rs.getBoolean("can_edit"),
				rs.getBoolean("can_delete"), rs.getBoolean("can_export"))), userId);
		return permissions;
	}

	private int insertAndReturnId(String sql, Object... arguments) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, new String[] { "id" });
			for (int index = 0; index < arguments.length; index++) {
				statement.setObject(index + 1, arguments[index]);
			}
			return statement;
		}, keyHolder);
		Number key = keyHolder.getKey();
		if (key == null) {
			throw new IllegalStateException("Database tidak mengembalikan ID baru.");
		}
		return key.intValue();
	}

	private Integer findId(String sql, Object... arguments) {
		List<Integer> ids = jdbc.query(sql, (rs, row) -> {
			int value = rs.getInt(1);
			return rs.wasNull() ? null : value;
		}, arguments);
		return ids.isEmpty() ? null : ids.get(0);
	}

	private int count(String table) {
		if (!Set.of("purchases", "sales").contains(table)) {
			throw new IllegalArgumentException("Tabel tidak diizinkan.");
		}
		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
		return count == null ? 0 : count;
	}

	private static Map<String, String> metric(String label, String value) {
		Map<String, String> metric = new LinkedHashMap<>();
		metric.put("label", label);
		metric.put("value", value);
		return metric;
	}

	private static String requiredCategoryName(CategoryRequest request) {
		if (request == null || isBlank(request.name())) {
			throw badRequest("Nama kategori wajib diisi.");
		}
		return request.name().trim().toUpperCase(Locale.ROOT);
	}

	private static String normalizedCategory(String value) {
		return defaultText(value, "UMUM").toUpperCase(Locale.ROOT);
	}

	private static String logoFor(String name) {
		String letters = name.replaceAll("[^A-Za-z]", "");
		return letters.length() >= 2 ? letters.substring(0, 2).toUpperCase(Locale.ROOT) : "SP";
	}

	private static boolean containsIgnoreCase(String value, String query) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private static String defaultText(String value, String fallback) {
		return isBlank(value) ? fallback : value.trim();
	}

	private static String normalizeRole(String role) {
		String value = defaultText(role, "Admin Kasir");
		if (value.equalsIgnoreCase("Kasir")) return "Admin Kasir";
		if (value.equalsIgnoreCase("System Gudang") || value.equalsIgnoreCase("Gudang")) return "Admin Gudang";
		if (value.equalsIgnoreCase("Admin")) return "Owner";
		if (value.equalsIgnoreCase("Owner")) return "Owner";
		if (value.equalsIgnoreCase("Admin Gudang")) return "Admin Gudang";
		if (value.equalsIgnoreCase("Admin Kasir")) return "Admin Kasir";
		throw badRequest("Role harus Owner, Admin Gudang, atau Admin Kasir.");
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
		for (String menu : MENUS) {
			boolean menuAllowed = allowed.contains(menu);
			boolean canDelete = menuAllowed && "Owner".equals(normalized);
			result.put(menu, new PermissionSet(menuAllowed, menuAllowed, menuAllowed, canDelete, menuAllowed));
		}
		return result;
	}

	private static int positiveOrDefault(Integer value, int fallback) {
		return value == null || value < 0 ? fallback : value;
	}

	private static int strictlyPositiveOrDefault(Integer value, int fallback) {
		return value == null || value <= 0 ? fallback : value;
	}

	private static long strictlyPositiveOrDefault(Long value, long fallback) {
		return value == null || value <= 0 ? fallback : value;
	}

	private static String status(Product product) {
		return product.stockStatus();
	}

	private static String status(int stock, int minStock) {
		if (stock <= 0) return "Habis";
		if (stock <= minStock) return "Stok Kritis";
		return "Tersedia";
	}

	private static String iconFor(String category) {
		String normalized = defaultText(category, "").toUpperCase(Locale.ROOT);
		if (normalized.contains("OLI")) return "oli";
		if (normalized.contains("BAN")) return "ban";
		if (normalized.contains("KAMPAS")) return "kampas";
		if (normalized.contains("AKI")) return "aki";
		if (normalized.contains("LAMPU")) return "lampu";
		return "mesin";
	}

	private static String rupiah(long value) {
		return "Rp " + String.format(Locale.ROOT, "%,d", value).replace(",", ".");
	}

	private static Integer nullableInteger(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
		int value = rs.getInt(column);
		return rs.wasNull() ? null : value;
	}

	private static String formatDate(Timestamp timestamp) {
		return timestamp == null ? "-" : DATE.format(timestamp.toLocalDateTime());
	}

	private static String formatDateTime(Timestamp timestamp) {
		return timestamp == null ? "-" : DATE_TIME.format(timestamp.toLocalDateTime());
	}

	private static String formatLastUsed(Timestamp timestamp) {
		return timestamp == null ? "Belum digunakan" : formatDateTime(timestamp);
	}

	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

	private static ResponseStatusException unauthorized(String message) {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
	}

	private static ResponseStatusException notFound(String message) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
	}

	private static ResponseStatusException conflict(String message) {
		return new ResponseStatusException(HttpStatus.CONFLICT, message);
	}

	private record PeriodSummary(long revenue, long capital, int sold, int transactions) {
		long profit() {
			return revenue - capital;
		}
	}

	private record ReportProduct(String name, int sold, long revenue, long profit) {}

	private record DateRange(String label, LocalDateTime from, LocalDateTime toExclusive) {
		static DateRange all() {
			return new DateRange("all", null, null);
		}

		static DateRange between(String label, LocalDate start, LocalDate end) {
			return new DateRange(label, start.atStartOfDay(), end.plusDays(1).atTime(LocalTime.MIDNIGHT));
		}

		String where(String column) {
			return from == null ? "" : " WHERE " + column + " >= ? AND " + column + " < ?";
		}

		Object[] arguments() {
			return from == null ? new Object[0] : new Object[] { Timestamp.valueOf(from), Timestamp.valueOf(toExclusive) };
		}
	}

	public record ProductRequest(String name, String category, Long price, Integer stock, Integer minStock) {}
	public record ProductUpdateRequest(String name, String category, Long price, Integer stock, Integer minStock) {}
	public record CategoryRequest(String name) {}
	public record SupplierRequest(String name, String pic, String phone, String category, String address, String email) {}
	public record SupplierUpdateRequest(String name, String pic, String phone, String status, String category, String address, String email) {}
	public record StockOpnameRequest(Integer productId, Integer physicalStock) {}
	public record StockOpnameUpdateRequest(Integer physicalStock) {}
	public record PurchaseRequest(String supplier, String item, String category, Integer quantity, Long unitPrice) {}
	public record InspectionRequest(Integer physicalQuantity, String note) {}
	public record SaleItemRequest(Integer productId, Integer quantity) {}
	public record SaleRequest(List<SaleItemRequest> items, String paymentMethod, Long paid) {}
	public record UserRequest(String name, String role, String username, String email, String status) {}
	public record UserUpdateRequest(String name, String role, String username, String email, String status) {}
	public record LoginRequest(String username, String email, String password, String role) {}
	public record PermissionRequest(Map<String, PermissionSet> permissions) {}
	public record PermissionSet(boolean page, boolean add, boolean edit, boolean delete, boolean export) {}

	public record AuthView(int id, String name, String username, String email, String role,
			String defaultPath, Map<String, PermissionSet> permissions) {
		static AuthView from(UserAccount user) {
			String path = UserFactory.create(user.id, user.name, user.username, user.email, user.role).defaultPath();
			return new AuthView(user.id, user.name, user.username, user.email, user.role, path,
					new LinkedHashMap<>(user.permissions));
		}
	}

	public record PriceChange(int productId, String productName, long oldPrice, long newPrice,
			String reason, String date) {}
	public record SupplierActivity(int supplierId, String title, String description, String time, String type) {}
	public record ProductView(int id, String name, String sku, String category, long price,
			String priceText, int stock, int minStock, int sold, String status, String icon) {
		static ProductView from(Product product) {
			return new ProductView(product.id, product.name, product.sku, product.category, product.sellingPrice,
					rupiah(product.sellingPrice), product.stock, product.minimumStock, product.sold,
					SipartService.status(product), product.icon);
		}
	}
	public record CategorySummary(int id, String name, long count, String icon) {}
	public record BestSeller(String name, String sold, String profit) {}
	public record ProfitRow(String name, String sold, String revenue, String profit) {}
	public record StockCheckView(int id, String name, String sku, int systemStock,
			Integer physicalStock, Integer difference, String status) {}
	private record StockOpnameTarget(Integer productId, String name, String sku, int systemStock, int minimumStock) {}
	public record PurchaseView(int id, String code, String supplier, String item, String category,
			int quantity, Integer physicalQuantity, Integer difference, String inspectionNote,
			long unitPrice, long total, String totalText, String status, String date) {}
	public record SaleLine(int productId, String name, int quantity, long price) {}
	public record SaleView(int id, String code, List<SaleLine> items, long subtotal, long tax,
			long total, long paid, long change, String subtotalText, String taxText, String totalText,
			String paidText, String changeText, String paymentMethod, String date) {}

	public static final class Supplier {
		public final int id;
		public final String name;
		public final String pic;
		public final String phone;
		public final String status;
		public final String logo;
		public final String category;
		public final String address;
		public final String email;
		public final String lastUsed;

		private Supplier(int id, String name, String pic, String phone, String status, String logo,
				String category, String address, String email, String lastUsed) {
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

	public static final class UserAccount {
		public final int id;
		public final String name;
		public final String role;
		public final String status;
		public final String username;
		public final String email;
		@JsonIgnore
		private final String password;
		public final String lastLogin;
		public final Map<String, PermissionSet> permissions;

		private UserAccount(int id, String name, String role, String status, String username,
				String email, String password, String lastLogin, Map<String, PermissionSet> permissions) {
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
}
