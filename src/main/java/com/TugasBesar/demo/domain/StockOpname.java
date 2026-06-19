package com.TugasBesar.demo.domain;

public record StockOpname(
		int id,
		String productName,
		String sku,
		int systemStock,
		Integer physicalStock) {

	public Integer difference() {
		return physicalStock == null ? null : physicalStock - systemStock;
	}

	public String status() {
		if (physicalStock == null) return "PENDING";
		return difference() == 0 ? "MATCH" : "DIFFERENCE";
	}
}
