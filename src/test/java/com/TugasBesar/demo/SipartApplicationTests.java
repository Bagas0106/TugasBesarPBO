package com.TugasBesar.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SipartApplicationTests {
	private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");
	private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
	private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();

	@LocalServerPort
	private int port;

	@Test
	void contextLoads() {
	}

	@Test
	void roleBasedLoginAndPageAccess() throws Exception {
		HttpResponse<String> loginPage = request("GET", "/login", null);
		assertStatus(200, loginPage);
		assertTrue(loginPage.body().contains("id=\"role-picker\""));
		assertTrue(loginPage.body().contains("@tailwindcss/browser@4"));
		assertStatus(302, request("GET", "/dashboard", null));

		login("admin@sipart.test", "Owner");
		for (String path : new String[] { "/dashboard", "/produk-kategori", "/stock", "/supplier", "/transaksi-pembelian", "/manajemen-user" }) {
			HttpResponse<String> response = request("GET", path, null);
			assertStatus(200, response);
			assertTrue(response.body().contains("/assets/sipart-ui.js"), path);
			assertTrue(!response.body().contains("<style"), path);
		}
		assertStatus(302, request("GET", "/transaksi-penjualan", null));
		assertStatus(200, request("GET", "/api/users", null));
		assertStatus(403, request("GET", "/api/sales/products", null));

		login("gudang@sipart.test", "Admin Gudang");
		assertStatus(200, request("GET", "/dashboard", null));
		assertStatus(200, request("GET", "/api/products", null));
		assertStatus(302, request("GET", "/manajemen-user", null));
		assertStatus(403, request("GET", "/api/users", null));

		login("kasir@sipart.test", "Admin Kasir");
		assertStatus(200, request("GET", "/transaksi-penjualan", null));
		assertStatus(200, request("GET", "/api/sales/products", null));
		assertStatus(302, request("GET", "/dashboard", null));
		assertStatus(403, request("GET", "/api/products", null));

		HttpResponse<String> wrongRole = request("POST", "/api/auth/login", "{\"email\":\"admin@sipart.test\",\"password\":\"demo\",\"role\":\"Admin Kasir\"}");
		assertStatus(401, wrongRole);
	}

	@Test
	void completeInventoryWorkflow() throws Exception {
		login("admin@sipart.test", "Owner");
		HttpResponse<String> productResponse = request("POST", "/api/products", """
				{"name":"Produk Integrasi","category":"MESIN","price":100000,"stock":8,"minStock":3}
				""");
		assertStatus(201, productResponse);
		assertTrue(productResponse.body().contains("Produk Integrasi"));
		int productId = idFrom(productResponse);

		HttpResponse<String> updateProduct = request("PUT", "/api/products/" + productId, "{\"price\":110000,\"stock\":8}");
		assertStatus(200, updateProduct);
		assertTrue(updateProduct.body().contains("\"price\":110000"));
		HttpResponse<String> priceHistory = request("GET", "/api/price-history?productId=" + productId, null);
		assertStatus(200, priceHistory);
		assertTrue(priceHistory.body().contains("\"oldPrice\":100000"));

		HttpResponse<String> categoryResponse = request("POST", "/api/categories", "{\"name\":\"Integrasi\"}");
		assertStatus(201, categoryResponse);
		int categoryId = idFrom(categoryResponse);
		HttpResponse<String> categoryUpdate = request("PUT", "/api/categories/" + categoryId, "{\"name\":\"Integrasi Baru\"}");
		assertStatus(200, categoryUpdate);
		assertTrue(categoryUpdate.body().contains("INTEGRASI BARU"));
		assertStatus(204, request("DELETE", "/api/categories/" + categoryId, null));

		HttpResponse<String> stockCheck = request("POST", "/api/stock", "{\"productId\":" + productId + ",\"physicalStock\":9}");
		assertStatus(201, stockCheck);
		assertTrue(stockCheck.body().contains("DIFFERENCE"));

		HttpResponse<String> supplierResponse = request("POST", "/api/suppliers", "{\"name\":\"Supplier Integrasi\",\"pic\":\"Dina\",\"phone\":\"081200000000\"}");
		assertStatus(201, supplierResponse);
		int supplierId = idFrom(supplierResponse);

		HttpResponse<String> updateSupplier = request("PUT", "/api/suppliers/" + supplierId, "{\"pic\":\"Dina Putri\",\"status\":\"AKTIF\"}");
		assertStatus(200, updateSupplier);
		assertTrue(updateSupplier.body().contains("Dina Putri"));
		assertTrue(request("GET", "/api/suppliers?q=Supplier%20Integrasi&status=AKTIF", null).body().contains("Supplier Integrasi"));
		assertTrue(request("GET", "/api/suppliers/activities", null).body().contains("Pembaruan Profil"));

		HttpResponse<String> purchaseResponse = request("POST", "/api/purchases", """
				{"supplier":"Supplier Integrasi","item":"Produk Integrasi","category":"MESIN","quantity":4,"unitPrice":70000}
				""");
		assertStatus(201, purchaseResponse);
		int purchaseId = idFrom(purchaseResponse);

		HttpResponse<String> inspection = request("POST", "/api/purchases/" + purchaseId + "/inspect", "{\"physicalQuantity\":3,\"note\":\"Satu item rusak\"}");
		assertStatus(200, inspection);
		assertTrue(inspection.body().contains("\"difference\":-1"));
		assertTrue(inspection.body().contains("Selisih"));
		assertStatus(200, request("POST", "/api/purchases/" + purchaseId + "/confirm", ""));
		assertStatus(200, request("POST", "/api/purchases/" + purchaseId + "/confirm", ""));
		assertProductStock(12);

		login("kasir@sipart.test", "Admin Kasir");
		HttpResponse<String> saleResponse = request("POST", "/api/sales", "{\"items\":[{\"productId\":" + productId + ",\"quantity\":2}],\"paymentMethod\":\"Tunai\",\"paid\":300000}");
		assertStatus(201, saleResponse);
		assertTrue(saleResponse.body().contains("\"change\":55800"));
		int saleId = idFrom(saleResponse);
		assertTrue(request("GET", "/api/sales/" + saleId, null).body().contains("Produk Integrasi"));
		assertTrue(request("GET", "/api/sales", null).body().contains("\"id\":" + saleId));

		HttpResponse<String> failedSale = request("POST", "/api/sales", "{\"items\":[{\"productId\":" + productId + ",\"quantity\":1}],\"paymentMethod\":\"Tunai\",\"paid\":1000}");
		assertStatus(400, failedSale);
		login("admin@sipart.test", "Owner");
		assertProductStock(10);
		HttpResponse<String> dailyReport = request("GET", "/api/dashboard?period=daily", null);
		assertStatus(200, dailyReport);
		assertTrue(dailyReport.body().contains("\"period\":\"daily\""));
		assertTrue(dailyReport.body().contains("Produk Integrasi"));
		assertStatus(400, request("GET", "/api/dashboard?from=2026-06-20&to=2026-06-19", null));

		HttpResponse<String> userResponse = request("POST", "/api/users", "{\"name\":\"User Integrasi\",\"role\":\"Kasir\",\"username\":\"user_integrasi\",\"email\":\"user@integrasi.test\"}");
		assertStatus(201, userResponse);
		int userId = idFrom(userResponse);
		HttpResponse<String> userUpdate = request("PUT", "/api/users/" + userId, "{\"name\":\"User Integrasi Baru\",\"role\":\"Admin Gudang\",\"status\":\"Nonaktif\"}");
		assertStatus(200, userUpdate);
		assertTrue(userUpdate.body().contains("Nonaktif"));
		assertTrue(request("POST", "/api/users/" + userId + "/reset", "").body().contains("Aktif"));
		String permission = "{\"permissions\":{\"Dashboard\":{\"page\":true,\"add\":false,\"edit\":false,\"delete\":false,\"export\":true}}}";
		assertStatus(200, request("PUT", "/api/users/" + userId + "/permissions", permission));
		assertTrue(request("GET", "/api/users/" + userId + "/permissions", null).body().contains("\"export\":true"));

		assertStatus(200, request("GET", "/api/auth/session", null));
		assertStatus(204, request("DELETE", "/api/users/" + userId, null));
		assertStatus(204, request("DELETE", "/api/suppliers/" + supplierId, null));
		assertStatus(204, request("DELETE", "/api/products/" + productId, null));
		assertStatus(204, request("POST", "/api/auth/logout", ""));
		assertStatus(401, request("GET", "/api/auth/session", null));
	}

	private void login(String email, String role) throws Exception {
		HttpResponse<String> response = request("POST", "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"demo\",\"role\":\"" + role + "\"}");
		assertStatus(200, response);
		assertTrue(response.body().contains("\"role\":\"" + role + "\""), response.body());
	}

	private void assertProductStock(int expected) throws Exception {
		HttpResponse<String> response = request("GET", "/api/products?q=Produk%20Integrasi", null);
		assertStatus(200, response);
		assertTrue(response.body().contains("\"stock\":" + expected), response.body());
	}

	private HttpResponse<String> request(String method, String path, String body) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
		if (body == null) {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		} else {
			builder.header("Content-Type", "application/json");
			builder.method(method, HttpRequest.BodyPublishers.ofString(body));
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static int idFrom(HttpResponse<String> response) {
		Matcher matcher = ID_PATTERN.matcher(response.body());
		if (!matcher.find()) {
			throw new IllegalStateException("Response tidak memiliki id: " + response.body());
		}
		return Integer.parseInt(matcher.group(1));
	}

	private static void assertStatus(int expected, HttpResponse<String> response) {
		assertEquals(expected, response.statusCode(), response.body());
	}
}
