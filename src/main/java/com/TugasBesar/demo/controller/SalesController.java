package com.TugasBesar.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.TugasBesar.demo.service.SipartService;

@RestController
@RequestMapping("/api/sales")
public class SalesController {
	private final SipartService service;

	public SalesController(SipartService service) {
		this.service = service;
	}

	@GetMapping("/products")
	public List<SipartService.ProductView> products() {
		return service.salesProducts();
	}

	@GetMapping
	public List<SipartService.SaleView> sales() {
		return service.sales();
	}

	@GetMapping("/{id}")
	public SipartService.SaleView sale(@PathVariable int id) {
		return service.sale(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SipartService.SaleView create(@RequestBody SipartService.SaleRequest request) {
		return service.createSale(request);
	}
}
