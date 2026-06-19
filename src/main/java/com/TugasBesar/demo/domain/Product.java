package com.TugasBesar.demo.domain;

public final class Product {
	public final int id;
	public final String name;
	public final String sku;
	public final String category;
	public final long sellingPrice;
	public final long costPrice;
	public final int stock;
	public final int minimumStock;
	public final int sold;
	public final String icon;

	public Product(int id, String name, String sku, String category, long sellingPrice, long costPrice,
			int stock, int minimumStock, int sold, String icon) {
		this.id = id;
		this.name = name;
		this.sku = sku;
		this.category = category;
		this.sellingPrice = sellingPrice;
		this.costPrice = costPrice;
		this.stock = stock;
		this.minimumStock = minimumStock;
		this.sold = sold;
		this.icon = icon;
	}

	public String stockStatus() {
		if (stock <= 0) return "Habis";
		if (stock <= minimumStock) return "Stok Kritis";
		return "Tersedia";
	}

	public long revenue() {
		return (long) sold * sellingPrice;
	}

	public long profit() {
		return (long) sold * (sellingPrice - costPrice);
	}
}
