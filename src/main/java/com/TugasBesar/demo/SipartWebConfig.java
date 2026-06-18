package com.TugasBesar.demo;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SipartWebConfig implements WebMvcConfigurer {
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ApiRoleInterceptor())
				.addPathPatterns("/api/**")
				.excludePathPatterns("/api/auth/**");
	}

	private static final class ApiRoleInterceptor implements HandlerInterceptor {
		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
			HttpSession session = request.getSession(false);
			Object value = session == null ? null : session.getAttribute("sipartUser");
			if (!(value instanceof SipartDataStore.AuthView user)) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Silakan login terlebih dahulu.");
				return false;
			}

			String path = request.getRequestURI();
			String role = user.role();
			String menu = menuFor(path);
			boolean allowed;
			if (path.startsWith("/api/sales")) {
				allowed = "Admin Kasir".equalsIgnoreCase(role);
			} else if (path.startsWith("/api/users")) {
				allowed = "Owner".equalsIgnoreCase(role);
			} else {
				allowed = "Owner".equalsIgnoreCase(role) || "Admin Gudang".equalsIgnoreCase(role);
			}
			SipartDataStore.PermissionSet permission = user.permissions().get(menu);
			if (allowed && permission != null) {
				String method = request.getMethod();
				allowed = switch (method) {
					case "GET" -> permission.page();
					case "DELETE" -> permission.delete();
					case "PUT", "PATCH" -> permission.edit();
					case "POST" -> isActionPath(path) ? permission.edit() : permission.add();
					default -> permission.page();
				};
			}
			if (!allowed) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Role tidak memiliki akses ke fitur ini.");
			}
			return allowed;
		}

		private String menuFor(String path) {
			if (path.startsWith("/api/products") || path.startsWith("/api/categories") || path.startsWith("/api/price-history")) return "products";
			if (path.startsWith("/api/stock")) return "stock";
			if (path.startsWith("/api/suppliers")) return "suppliers";
			if (path.startsWith("/api/purchases")) return "purchases";
			if (path.startsWith("/api/sales")) return "sales";
			if (path.startsWith("/api/users")) return "users";
			return "dashboard";
		}

		private boolean isActionPath(String path) {
			return path.endsWith("/confirm") || path.endsWith("/inspect") || path.endsWith("/reset");
		}
	}
}
