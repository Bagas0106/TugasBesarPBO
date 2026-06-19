package com.TugasBesar.demo.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.TugasBesar.demo.service.SipartService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
	private final SipartService service;

	public DashboardController(SipartService service) {
		this.service = service;
	}

	@GetMapping
	public Map<String, Object> dashboard(
			@RequestParam(defaultValue = "all") String period,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return service.dashboard(period, from, to);
	}
}
