package top.egon.cola.platform.rbac3.admin.iam.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import top.egon.cola.platform.rbac3.admin.iam.user.service.UserCrudFacade;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTest {

    @Test
    void keepsUserCrudFacadeAsTheWriteBoundary() {
        assertThat(Arrays.stream(UserController.class.getDeclaredFields())
                .map(Field::getType))
                .contains(UserCrudFacade.class);
    }

    @Test
    void declaresUserCrudRoutesUnderIam() {
        RequestMapping mapping = UserController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/rbac3/v1/iam");

        Stream<String> routes = Arrays.stream(UserController.class.getDeclaredMethods())
                .flatMap(method -> {
                    if (method.isAnnotationPresent(PostMapping.class)) {
                        return Arrays.stream(method.getAnnotation(PostMapping.class).value());
                    }
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        return Arrays.stream(method.getAnnotation(GetMapping.class).value());
                    }
                    if (method.isAnnotationPresent(PutMapping.class)) {
                        return Arrays.stream(method.getAnnotation(PutMapping.class).value());
                    }
                    if (method.isAnnotationPresent(DeleteMapping.class)) {
                        return Arrays.stream(method.getAnnotation(DeleteMapping.class).value());
                    }
                    return Stream.empty();
                });

        assertThat(routes).contains(
                "/users", "/users/{userId}", "/users/{userId}/status");
    }
}
