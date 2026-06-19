package com.TugasBesar.demo.domain;

public record SaleItem(int productId, String name, int quantity, long price) {
	public long subtotal() {
		return price * quantity;
	}
}
