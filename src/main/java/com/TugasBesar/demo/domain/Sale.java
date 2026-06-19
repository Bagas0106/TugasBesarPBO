package com.TugasBesar.demo.domain;

import java.time.LocalDateTime;
import java.util.List;

public record Sale(
		int id,
		List<SaleItem> items,
		long subtotal,
		long tax,
		long total,
		long paid,
		String paymentMethod,
		LocalDateTime createdAt) {

	public long change() {
		return paid - total;
	}
}
