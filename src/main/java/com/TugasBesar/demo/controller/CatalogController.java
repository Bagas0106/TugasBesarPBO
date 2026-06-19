package com.TugasBesar.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.TugasBesar.demo.service.SipartService;

@RestController
@RequestMapping("/api")
public class CatalogController {
	private final SipartService service;

	public CatalogController(SipartService service) {
		this.service = service;
	}

	@GetMapping("/products")
	public List<SipartService.ProductView> products(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String status) {
		return service.products(q, category, status);
	}

	@PostMapping("/products")
	@ResponseStatus(HttpStatus.CREATED)
	public SipartService.ProductView createProduct(@RequestBody SipartService.ProductRequest request) {
		return service.createProduct(request);
	}

	@PutMapping("/products/{id}")
	public SipartService.ProductView updateProduct(
			@PathVariable int id, @RequestBody SipartService.ProductUpdateRequest request) {
		return service.updateProduct(id, request);
	}

	@PatchMapping("/products/{id}")
	public SipartService.ProductView patchProduct(
			@PathVariable int id, @RequestBody SipartService.ProductUpdateRequest request) {
		return service.updateProduct(id, request);
	}

	@DeleteMapping("/products/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProduct(@PathVariable int id) {
		service.deleteProduct(id);
	}

	@GetMapping("/categories")
	public List<SipartService.CategorySummary> categories() {
		return service.categories();
	}

	@PostMapping("/categories")
	@ResponseStatus(HttpStatus.CREATED)
	public SipartService.CategorySummary createCategory(@RequestBody SipartService.CategoryRequest request) {
		return service.createCategory(request);
	}

	@PutMapping("/categories/{id}")
	public SipartService.CategorySummary updateCategory(
			@PathVariable int id, @RequestBody SipartService.CategoryRequest request) {
		return service.updateCategory(id, request);
	}

	@DeleteMapping("/categories/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCategory(@PathVariable int id) {
		service.deleteCategory(id);
	}

	@GetMapping("/price-history")
	public List<SipartService.PriceChange> priceHistory(@RequestParam(required = false) Integer productId) {
		return service.priceHistory(productId);
	}
}
