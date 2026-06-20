package com.TugasBesar.demo.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.TugasBesar.demo.domain.user.User;
import com.TugasBesar.demo.domain.user.UserFactory;
import com.TugasBesar.demo.service.SipartService;

@Configuration
public class WebConfig implements WebMvcConfigurer {
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
			if (!(value instanceof SipartService.AuthView user)) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Silakan login terlebih dahulu.");
				return false;
			}

			String path = request.getRequestURI();
			String menu = menuFor(path);
			User roleUser = UserFactory.create(user.id(), user.name(), user.username(), user.email(), user.role());
			boolean allowed = roleUser.canAccessMenu(menu);
			SipartService.PermissionSet permission = user.permissions().get(menu);
			if (allowed && permission != null) {
				String method = request.getMethod();
				allowed = switch (method) {
					case "GET" -> permission.page();
					case "DELETE" -> permission.delete();
					case "PUT", "PATCH" -> permission.edit();
					case "POST" -> isActionPath(path) ? permission.edit() : permission.add();
					default -> permission.page();
				};
				if (path.startsWith("/api/stock")) {
					if ("POST".equals(method)) allowed = "Admin Gudang".equals(user.role());
					if ("PUT".equals(method) || "PATCH".equals(method)) allowed = "Owner".equals(user.role());
				}
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
