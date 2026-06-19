package com.TugasBesar.demo.domain.user;

import java.util.Set;

public final class AdminKasir extends User {
	public AdminKasir(int id, String name, String username, String email) {
		super(id, name, username, email);
	}

	@Override
	public String role() {
		return "Admin Kasir";
	}

	@Override
	public String defaultPath() {
		return "/transaksi-penjualan";
	}

	@Override
	protected Set<String> allowedMenus() {
		return Set.of("sales");
	}

	@Override
	protected Set<AppScope> allowedScopes() {
		return Set.of(AppScope.SALES);
	}
}
