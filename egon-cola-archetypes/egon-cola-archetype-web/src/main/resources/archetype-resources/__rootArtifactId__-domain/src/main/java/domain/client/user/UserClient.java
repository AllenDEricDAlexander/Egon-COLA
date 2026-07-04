package ${package}.domain.client.user;

import ${package}.domain.common.Page;
import ${package}.domain.entities.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Optional;

public interface UserClient {
    User save(@NotNull User user);

    Optional<User> findById(@NotBlank String userId);

    Page<User> findPage(@Positive int currentPage, @Positive int pageSize);

    boolean existsByEmail(@NotBlank @Email String email);
}
