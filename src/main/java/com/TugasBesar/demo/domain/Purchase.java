package com.TugasBesar.demo.domain;

public record Purchase(
		int id,
		String supplier,
		String item,
		String category,
		int quantity,
		Integer physicalQuantity,
		String inspectionNote,
		long unitPrice,
		String status,
		String date) {

	public long total() {
		return (long) quantity * unitPrice;
	}

	public Integer difference() {
		return physicalQuantity == null ? null : physicalQuantity - quantity;
	}

	public int acceptedQuantity() {
		return physicalQuantity == null ? quantity : physicalQuantity;
	}
}
