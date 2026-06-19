package com.TugasBesar.demo.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.TugasBesar.demo.domain.user.AppScope;
import com.TugasBesar.demo.domain.user.User;
import com.TugasBesar.demo.domain.user.UserFactory;
import com.TugasBesar.demo.service.SipartService;

@Controller
public class PageController {

	@GetMapping("/")
	public String home(HttpSession session) {
		SipartService.AuthView user = currentUser(session);
		return user == null ? "redirect:/login" : "redirect:" + user.defaultPath();
	}

	@GetMapping("/login")
	public String login(HttpSession session) {
		SipartService.AuthView user = currentUser(session);
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
		SipartService.AuthView user = currentUser(session);
		if (user == null) return "redirect:/login";
		SipartService.PermissionSet permission = user.permissions().get(permissionKey);
		User roleUser = UserFactory.create(user.id(), user.name(), user.username(), user.email(), user.role());
		if (!roleUser.canAccessScope(scope(scope)) || permission == null || !permission.page()) {
			return "redirect:" + user.defaultPath();
		}
		return "forward:/" + file;
	}

	private AppScope scope(String value) {
		return switch (value) {
			case "owner" -> AppScope.OWNER;
			case "sales" -> AppScope.SALES;
			default -> AppScope.INVENTORY;
		};
	}

	private SipartService.AuthView currentUser(HttpSession session) {
		Object value = session.getAttribute("sipartUser");
		return value instanceof SipartService.AuthView user ? user : null;
	}
}
