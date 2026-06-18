package com.TugasBesar.demo;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class SipartApiController {
	private final SipartDataStore store;

	public SipartApiController(SipartDataStore store) {
		this.store = store;
	}

	@GetMapping("/dashboard")
	public Map<String, Object> dashboard() {
		return store.dashboard();
	}

	@GetMapping("/products")
	public List<SipartDataStore.ProductView> products(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String status) {
		return store.products(q, category, status);
	}

	@PostMapping("/products")
	@ResponseStatus(HttpStatus.CREATED)
	public SipartDataStore.ProductView createProduct(@RequestBody SipartDataStore.ProductRequest request) {
		return store.createProduct(request);
	}

	@PutMapping("/products/{id}")
	public SipartDataStore.ProductView updateProduct(@PathVariable int id, @RequestBody SipartDataStore.ProductUpdateRequest request) {
		return store.updateProduct(id, request);
	}

	@PatchMapping("/products/{id}")
	public SipartDataStore.ProductView patchProduct(@PathVariable int id, @RequestBody SipartDataStore.ProductUpdateRequest request) {
		return store.updateProduct(id, request);
	}

	@DeleteMapping("/products/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProduct(@PathVariable int id) {
		store.deleteProduct(id);
	}

	@GetMapping("/categories")
	public List<SipartDataStore.CategorySummary> categories() {
		return store.categories();
	}

	@PostMapping("/categories")
	@ResponseStatus(HttpStatus.CREATED)
	public SipartDataStore.CategorySummary createCategory(@RequestBody SipartDataStore.CategoryRequest request) {
		return store.createCategory(request);
	}

	@PutMapping("/categories/{id}")
	public SipartDataStore.CategorySummary updateCategory(@PathVariable int id, @RequestBody SipartDataStore.CategoryRequest request) {
		return store.updateCategory(id, request);
	}

	@DeleteMapping("/categories/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCategory(@PathVariable int id) {
		store.deleteCategory(id);
	}

	@GetMapping("/price-history")
	public List<SipartDataStore.PriceChange> priceHistory(@RequestParam(required = false) Integer productId) {
		return store.priceHistory(productId);
	}

	@GetMapping("/stock")
	public List<SipartDataStore.StockCheckView> stockChecks(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status) {
		return store.stockChecks(q, status);
	}

	@PostMapping("/stock")
	@ResponseStatus(HttpStatus.CREATED)
	public SipartDataStore.StockCheckView createStockCheck(@RequestBody SipartDataStore.StockOpnameRequest request) {
		return store.createStockCheck(request);
	}

	@GetMapping("/suppliers")
	public List<SipartDataStore.Supplier> suppliers(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status) {
		return store.suppliers(q, status);
	}

	@GetMapping("/suppliers/activities")
	public List<SipartDataStore.SupplierActivity> supplierActivities() {
		return store.supplierActivities();
	}

	@PostMapping("/suppliers")
	@ResponseStatus(HttpStatus.CREATED)
	public SipartDataStore.Supplier createSupplier(@RequestBody SipartDataStore.SupplierRequest request) {
		return store.createSupplier(request);
	}

	@PutMapping("/suppliers/{id}")
	public SipartDataStore.Supplier updateSupplier(@PathVariable int id, @RequestBody SipartDataStore.SupplierUpdateRequest request) {
		return store.updateSupplier(id, request);
	}

	@DeleteMapping("/suppliers/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteSupplier(@PathVariable int id) {
		store.deleteSupplier(id);
	}

	@GetMapping("/purchases")
	public List<SipartDataStore.PurchaseView> purchases(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status) {
		return store.purchases(q, status);
	}

	@PostMapping("/purchases")
	@ResponseStatus(HttpStatus.CREATED)
	public SipartDataStore.PurchaseView createPurchase(@RequestBody SipartDataStore.PurchaseRequest request) {
		return store.createPurchase(request);
	}

	@PostMapping("/purchases/{id}/confirm")
	public SipartDataStore.PurchaseView confirmPurchase(@PathVariable int id) {
		return store.confirmPurchase(id);
	}

	@PostMapping("/purchases/{id}/inspect")
	public SipartDataStore.PurchaseView inspectPurchase(@PathVariable int id, @RequestBody SipartDataStore.InspectionRequest request) {
		return store.inspectPurchase(id, request);
	}

	@DeleteMapping("/purchases/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePurchase(@PathVariable int id) {
		store.deletePurchase(id);
	}

	@GetMapping("/sales/products")
	public List<SipartDataStore.ProductView> salesProducts() {
		return store.salesProducts();
	}

	@GetMapping("/sales")
	public List<SipartDataStore.SaleView> sales() {
		return store.sales();
	}

	@GetMapping("/sales/{id}")
	public SipartDataStore.SaleView sale(@PathVariable int id) {
		return store.sale(id);
	}

	@PostMapping("/sales")
	@ResponseStatus(HttpStatus.CREATED)
	public SipartDataStore.SaleView createSale(@RequestBody SipartDataStore.SaleRequest request) {
		return store.createSale(request);
	}

	@GetMapping("/users")
	public List<SipartDataStore.UserAccount> users() {
		return store.users();
	}

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	public SipartDataStore.UserAccount createUser(@RequestBody SipartDataStore.UserRequest request) {
		return store.createUser(request);
	}

	@PutMapping("/users/{id}")
	public SipartDataStore.UserAccount updateUser(@PathVariable int id, @RequestBody SipartDataStore.UserUpdateRequest request, HttpSession session) {
		SipartDataStore.UserAccount user = store.updateUser(id, request);
		refreshSession(id, session);
		return user;
	}

	@PostMapping("/users/{id}/reset")
	public SipartDataStore.UserAccount resetUser(@PathVariable int id, HttpSession session) {
		SipartDataStore.UserAccount user = store.resetUser(id);
		refreshSession(id, session);
		return user;
	}

	@GetMapping("/users/{id}/permissions")
	public Map<String, SipartDataStore.PermissionSet> userPermissions(@PathVariable int id) {
		return store.userPermissions(id);
	}

	@PutMapping("/users/{id}/permissions")
	public Map<String, SipartDataStore.PermissionSet> updateUserPermissions(@PathVariable int id, @RequestBody SipartDataStore.PermissionRequest request, HttpSession session) {
		Map<String, SipartDataStore.PermissionSet> permissions = store.updateUserPermissions(id, request);
		refreshSession(id, session);
		return permissions;
	}

	@DeleteMapping("/users/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUser(@PathVariable int id) {
		store.deleteUser(id);
	}

	@PostMapping("/auth/login")
	public SipartDataStore.AuthView login(@RequestBody SipartDataStore.LoginRequest request, HttpSession session) {
		SipartDataStore.AuthView user = store.authenticate(request);
		session.setAttribute("sipartUser", user);
		return user;
	}

	@GetMapping("/auth/session")
	public SipartDataStore.AuthView session(HttpSession session) {
		Object user = session.getAttribute("sipartUser");
		if (user instanceof SipartDataStore.AuthView authView) return authView;
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Belum login.");
	}

	@PostMapping("/auth/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpSession session) {
		session.invalidate();
	}

	private void refreshSession(int userId, HttpSession session) {
		Object value = session.getAttribute("sipartUser");
		if (value instanceof SipartDataStore.AuthView current && current.id() == userId) {
			session.setAttribute("sipartUser", store.authView(userId));
		}
	}
}
