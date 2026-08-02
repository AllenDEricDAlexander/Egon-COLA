package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Rbac3RequiredApiSurfaceTest {

    @Test
    void exposesTheApprovedAuthenticationDirectoryAndSessionRoutes() {
        Set<String> routes = routes(
                AuthController.class,
                RoleActivationController.class,
                TenantUserDirectoryController.class,
                SessionController.class);

        assertEquals(Set.of(
                "POST /api/rbac3/v1/auth/logout",
                "GET /api/rbac3/v1/auth/bootstrap",
                "GET /api/rbac3/v1/auth/role-activation-candidates",
                "GET /api/rbac3/v1/auth/role-activations",
                "PUT /api/rbac3/v1/auth/role-activations",
                "GET /api/rbac3/v1/platform/tenants",
                "POST /api/rbac3/v1/platform/tenants",
                "GET /api/rbac3/v1/platform/tenants/{tenantId}",
                "PUT /api/rbac3/v1/platform/tenants/{tenantId}/status",
                "GET /api/rbac3/v1/users",
                "GET /api/rbac3/v1/users/{userId}",
                "PUT /api/rbac3/v1/users/{userId}/status",
                "GET /api/rbac3/v1/org-units",
                "GET /api/rbac3/v1/positions",
                "POST /api/rbac3/v1/internal/directory-snapshots",
                "GET /api/rbac3/v1/directory-snapshots/{snapshotId}",
                "GET /api/rbac3/v1/sessions/me",
                "POST /api/rbac3/v1/sessions/{sessionId}/revoke",
                "POST /api/rbac3/v1/users/{userId}/sessions/revoke-all"
        ), routes);
    }

    private Set<String> routes(Class<?>... controllers) {
        Set<String> routes = new LinkedHashSet<>();
        for (Class<?> controller : controllers) {
            String base = controller.getAnnotation(RequestMapping.class).value()[0];
            for (Method method : controller.getDeclaredMethods()) {
                add(routes, "GET", base, values(method, GetMapping.class));
                add(routes, "POST", base, values(method, PostMapping.class));
                add(routes, "PUT", base, values(method, PutMapping.class));
                add(routes, "DELETE", base, values(method, DeleteMapping.class));
            }
        }
        return routes;
    }

    private String[] values(Method method, Class<?> annotationType) {
        if (annotationType == GetMapping.class && method.isAnnotationPresent(GetMapping.class)) {
            return method.getAnnotation(GetMapping.class).value();
        }
        if (annotationType == PostMapping.class && method.isAnnotationPresent(PostMapping.class)) {
            return method.getAnnotation(PostMapping.class).value();
        }
        if (annotationType == PutMapping.class && method.isAnnotationPresent(PutMapping.class)) {
            return method.getAnnotation(PutMapping.class).value();
        }
        if (annotationType == DeleteMapping.class && method.isAnnotationPresent(DeleteMapping.class)) {
            return method.getAnnotation(DeleteMapping.class).value();
        }
        return new String[0];
    }

    private void add(Set<String> routes, String verb, String base, String[] paths) {
        for (String path : paths) {
            routes.add(verb + " " + base + path);
        }
    }
}
