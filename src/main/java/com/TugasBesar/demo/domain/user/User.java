package com.TugasBesar.demo.domain.user;

import java.util.Set;

public abstract sealed class User permits Owner, AdminGudang, AdminKasir {
	private final int id;
	private final String name;
	private final String username;
	private final String email;

	protected User(int id, String name, String username, String email) {
		this.id = id;
		this.name = name;
		this.username = username;
		this.email = email;
	}

	public int id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String username() {
		return username;
	}

	public String email() {
		return email;
	}

	public abstract String role();

	public abstract String defaultPath();

	protected abstract Set<String> allowedMenus();

	protected abstract Set<AppScope> allowedScopes();

	public final boolean canAccessMenu(String menu) {
		return allowedMenus().contains(menu);
	}

	public final boolean canAccessScope(AppScope scope) {
		return allowedScopes().contains(scope);
	}
}
