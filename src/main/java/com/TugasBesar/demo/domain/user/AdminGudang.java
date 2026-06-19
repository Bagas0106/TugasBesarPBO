package com.TugasBesar.demo.domain.user;

import java.util.Set;

public final class AdminGudang extends User {
	private static final Set<String> MENUS = Set.of(
			"dashboard", "products", "stock", "suppliers", "purchases");

	public AdminGudang(int id, String name, String username, String email) {
		super(id, name, username, email);
	}

	@Override
	public String role() {
		return "Admin Gudang";
	}

	@Override
	public String defaultPath() {
		return "/dashboard";
	}

	@Override
	protected Set<String> allowedMenus() {
		return MENUS;
	}

	@Override
	protected Set<AppScope> allowedScopes() {
		return Set.of(AppScope.INVENTORY);
	}
}
