package com.TugasBesar.demo.domain.user;

public final class UserFactory {
	private UserFactory() {}

	public static User create(int id, String name, String username, String email, String role) {
		if ("Owner".equalsIgnoreCase(role)) return new Owner(id, name, username, email);
		if ("Admin Gudang".equalsIgnoreCase(role)) return new AdminGudang(id, name, username, email);
		if ("Admin Kasir".equalsIgnoreCase(role) || "Kasir".equalsIgnoreCase(role)) {
			return new AdminKasir(id, name, username, email);
		}
		throw new IllegalArgumentException("Role tidak dikenal: " + role);
	}
}
