package com.TugasBesar.demo.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.TugasBesar.demo.service.SipartService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final SipartService service;

	public AuthController(SipartService service) {
		this.service = service;
	}

	@PostMapping("/login")
	public SipartService.AuthView login(@RequestBody SipartService.LoginRequest request, HttpSession session) {
		SipartService.AuthView user = service.authenticate(request);
		session.setAttribute("sipartUser", user);
		return user;
	}

	@GetMapping("/session")
	public SipartService.AuthView session(HttpSession session) {
		Object user = session.getAttribute("sipartUser");
		if (user instanceof SipartService.AuthView authView) return authView;
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Belum login.");
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpSession session) {
		session.invalidate();
	}
}
