package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3LegacyAuthenticationRemovedIT {

    @Test
    void personnelLoginRefreshAndJwksEndpointsAreGone() {
        assertThat(mappedPaths()).doesNotContain("/login", "/refresh", "/jwks");
    }

    private Stream<String> mappedPaths() {
        return Arrays.stream(AuthController.class.getDeclaredMethods())
                .flatMap(method -> {
                    PostMapping post = method.getAnnotation(PostMapping.class);
                    GetMapping get = method.getAnnotation(GetMapping.class);
                    if (post != null) {
                        return Arrays.stream(post.value());
                    }
                    if (get != null) {
                        return Arrays.stream(get.value());
                    }
                    return Stream.empty();
                });
    }
}
