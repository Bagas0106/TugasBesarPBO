package com.TugasBesar.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.TugasBesar.demo.service.SipartService;

@RestController
@RequestMapping("/api/stock")
public class InventoryController {
	private final SipartService service;

	public InventoryController(SipartService service) {
		this.service = service;
	}

	@GetMapping
	public List<SipartService.StockCheckView> stockChecks(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status) {
		return service.stockChecks(q, status);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SipartService.StockCheckView createStockCheck(@RequestBody SipartService.StockOpnameRequest request) {
		return service.createStockCheck(request);
	}

	@PutMapping("/{id}")
	public SipartService.StockCheckView updateStockCheck(
			@PathVariable int id,
			@RequestBody SipartService.StockOpnameUpdateRequest request) {
		return service.updateStockCheck(id, request);
	}
}
