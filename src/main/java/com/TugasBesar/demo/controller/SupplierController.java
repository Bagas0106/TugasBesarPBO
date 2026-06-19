package com.TugasBesar.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/suppliers")
public class SupplierController {
	private final SipartService service;

	public SupplierController(SipartService service) {
		this.service = service;
	}

	@GetMapping
	public List<SipartService.Supplier> suppliers(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status) {
		return service.suppliers(q, status);
	}

	@GetMapping("/activities")
	public List<SipartService.SupplierActivity> activities() {
		return service.supplierActivities();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SipartService.Supplier create(@RequestBody SipartService.SupplierRequest request) {
		return service.createSupplier(request);
	}

	@PutMapping("/{id}")
	public SipartService.Supplier update(
			@PathVariable int id, @RequestBody SipartService.SupplierUpdateRequest request) {
		return service.updateSupplier(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable int id) {
		service.deleteSupplier(id);
	}
}
