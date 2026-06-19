package com.TugasBesar.demo.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.TugasBesar.demo.service.SipartService;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final SipartService service;

	public UserController(SipartService service) {
		this.service = service;
	}

	@GetMapping
	public List<SipartService.UserAccount> users() {
		return service.users();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SipartService.UserAccount create(@RequestBody SipartService.UserRequest request) {
		return service.createUser(request);
	}

	@PutMapping("/{id}")
	public SipartService.UserAccount update(
			@PathVariable int id, @RequestBody SipartService.UserUpdateRequest request, HttpSession session) {
		SipartService.UserAccount user = service.updateUser(id, request);
		refreshSession(id, session);
		return user;
	}

	@PostMapping("/{id}/reset")
	public SipartService.UserAccount reset(@PathVariable int id, HttpSession session) {
		SipartService.UserAccount user = service.resetUser(id);
		refreshSession(id, session);
		return user;
	}

	@GetMapping("/{id}/permissions")
	public Map<String, SipartService.PermissionSet> permissions(@PathVariable int id) {
		return service.userPermissions(id);
	}

	@PutMapping("/{id}/permissions")
	public Map<String, SipartService.PermissionSet> updatePermissions(
			@PathVariable int id, @RequestBody SipartService.PermissionRequest request, HttpSession session) {
		Map<String, SipartService.PermissionSet> permissions = service.updateUserPermissions(id, request);
		refreshSession(id, session);
		return permissions;
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable int id) {
		service.deleteUser(id);
	}

	private void refreshSession(int userId, HttpSession session) {
		Object value = session.getAttribute("sipartUser");
		if (value instanceof SipartService.AuthView current && current.id() == userId) {
			session.setAttribute("sipartUser", service.authView(userId));
		}
	}
}
