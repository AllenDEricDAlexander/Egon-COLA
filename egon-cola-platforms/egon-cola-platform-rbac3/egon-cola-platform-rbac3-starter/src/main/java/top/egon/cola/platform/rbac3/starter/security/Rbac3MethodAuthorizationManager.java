package top.egon.cola.platform.rbac3.starter.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Spring Method Security PEP shared by API-resource and generic permission annotations.
 *
 * <p>All declared permissions are evaluated through the existing AuthorizationService. The
 * manager fails closed for denied or indeterminate decisions and never relies on URL rules.</p>
 */
public final class Rbac3MethodAuthorizationManager
        implements AuthorizationManager<MethodInvocation> {

    private final AuthorizationService authorization;

    public Rbac3MethodAuthorizationManager(AuthorizationService authorization) {
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authentication,
            MethodInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        Method method = Objects.requireNonNull(invocation.getMethod(), "method");
        Class<?> targetClass = invocation.getThis() == null
                ? method.getDeclaringClass()
                : invocation.getThis().getClass();
        for (String permission : permissions(method, targetClass)) {
            var decision = authorization.requirePermission(
                    PermissionRequest.of(permission));
            if (decision == null || decision.decision() != Decision.ALLOW) {
                return new AuthorizationDecision(false);
            }
        }
        return new AuthorizationDecision(true);
    }

    /** Resolves method/type/API declarations in stable declaration order. */
    List<String> permissions(MethodInvocation invocation) {
        Method method = Objects.requireNonNull(invocation.getMethod(), "method");
        Class<?> targetClass = invocation.getThis() == null
                ? method.getDeclaringClass()
                : invocation.getThis().getClass();
        return permissions(method, targetClass);
    }

    public boolean supports(Method method, Class<?> targetClass) {
        return !permissions(method, targetClass).isEmpty();
    }

    private List<String> permissions(Method method, Class<?> targetClass) {
        LinkedHashSet<String> required = new LinkedHashSet<>();
        addGeneric(required, targetClass);
        addGeneric(required, method.getDeclaringClass());
        addGeneric(required, method);
        addApi(required, targetClass);
        addApi(required, method.getDeclaringClass());
        addApi(required, method);
        return List.copyOf(required);
    }

    private void addApi(LinkedHashSet<String> required, AnnotatedElement element) {
        RBACAPIResource resource = element.getAnnotation(RBACAPIResource.class);
        if (resource == null) {
            return;
        }
        required(resource.code(), "RBACAPIResource.code");
        required.add(required(resource.permission(), "RBACAPIResource.permission"));
        required(resource.name(), "RBACAPIResource.name");
    }

    private void addGeneric(
            LinkedHashSet<String> required,
            AnnotatedElement element) {
        for (Annotation annotation : element.getAnnotations()) {
            if (!"RequiresRbac3Permission".equals(
                    annotation.annotationType().getSimpleName())) {
                continue;
            }
            try {
                Method permission = annotation.annotationType()
                        .getDeclaredMethod("permission");
                Object value = permission.invoke(annotation);
                required.add(required(
                        value == null ? null : value.toString(),
                        "RequiresRbac3Permission.permission"));
            } catch (ReflectiveOperationException exception) {
                throw new IllegalArgumentException(
                        "invalid RequiresRbac3Permission annotation", exception);
            }
        }
        RequiresPermission generic = element.getAnnotation(RequiresPermission.class);
        if (generic != null) {
            required.add(required(generic.value(), "RequiresPermission.value"));
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
