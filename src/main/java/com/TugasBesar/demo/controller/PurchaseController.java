package com.TugasBesar.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.TugasBesar.demo.service.SipartService;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
	private final SipartService service;

	public PurchaseController(SipartService service) {
		this.service = service;
	}

	@GetMapping
	public List<SipartService.PurchaseView> purchases(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status) {
		return service.purchases(q, status);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SipartService.PurchaseView create(@RequestBody SipartService.PurchaseRequest request) {
		return service.createPurchase(request);
	}

	@PostMapping("/{id}/inspect")
	public SipartService.PurchaseView inspect(
			@PathVariable int id, @RequestBody SipartService.InspectionRequest request) {
		return service.inspectPurchase(id, request);
	}

	@PostMapping("/{id}/confirm")
	public SipartService.PurchaseView confirm(@PathVariable int id) {
		return service.confirmPurchase(id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable int id) {
		service.deletePurchase(id);
	}
}
