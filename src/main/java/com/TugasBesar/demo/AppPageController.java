package com.TugasBesar.demo;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppPageController {

	@GetMapping("/")
	public String home(HttpSession session) {
		SipartDataStore.AuthView user = currentUser(session);
		return user == null ? "redirect:/login" : "redirect:" + user.defaultPath();
	}

	@GetMapping("/login")
	public String login(HttpSession session) {
		SipartDataStore.AuthView user = currentUser(session);
		return user == null ? "forward:/login.html" : "redirect:" + user.defaultPath();
	}

	@GetMapping({"/dashboard", "/admin/dashboard", "/gudang/dashboard"})
	public String dashboard(HttpSession session) {
		return page(session, "dashboard.html", "inventory", "dashboard");
	}

	@GetMapping({"/produk-kategori", "/admin/produk-kategori", "/gudang/produk-kategori"})
	public String products(HttpSession session) {
		return page(session, "produk-kategori.html", "inventory", "products");
	}

	@GetMapping({"/stock", "/admin/stock", "/gudang/stock"})
	public String stock(HttpSession session) {
		return page(session, "stock.html", "inventory", "stock");
	}

	@GetMapping({"/supplier", "/admin/supplier", "/gudang/supplier"})
	public String supplier(HttpSession session) {
		return page(session, "supplier.html", "inventory", "suppliers");
	}

	@GetMapping({"/transaksi-pembelian", "/admin/transaksi-pembelian", "/gudang/transaksi-pembelian"})
	public String purchase(HttpSession session) {
		return page(session, "transaksi-pembelian.html", "inventory", "purchases");
	}

	@GetMapping({"/transaksi-penjualan", "/kasir/transaksi-penjualan"})
	public String sales(HttpSession session) {
		return page(session, "transaksi-penjualan.html", "sales", "sales");
	}

	@GetMapping({"/manajemen-user", "/admin/users"})
	public String users(HttpSession session) {
		return page(session, "manajemen-user.html", "owner", "users");
	}

	private String page(HttpSession session, String file, String scope, String permissionKey) {
		SipartDataStore.AuthView user = currentUser(session);
		if (user == null) return "redirect:/login";
		SipartDataStore.PermissionSet permission = user.permissions().get(permissionKey);
		if (!hasScope(user.role(), scope) || permission == null || !permission.page()) return "redirect:" + user.defaultPath();
		return "forward:/" + file;
	}

	private boolean hasScope(String role, String scope) {
		if ("owner".equals(scope)) return "Owner".equalsIgnoreCase(role);
		if ("sales".equals(scope)) return "Admin Kasir".equalsIgnoreCase(role);
		return "Owner".equalsIgnoreCase(role) || "Admin Gudang".equalsIgnoreCase(role);
	}

	private SipartDataStore.AuthView currentUser(HttpSession session) {
		Object value = session.getAttribute("sipartUser");
		return value instanceof SipartDataStore.AuthView user ? user : null;
	}
}
