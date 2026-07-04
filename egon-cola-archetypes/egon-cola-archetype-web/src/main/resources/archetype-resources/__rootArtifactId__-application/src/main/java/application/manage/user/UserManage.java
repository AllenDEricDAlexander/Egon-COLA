package ${package}.application.manage.user;

import ${package}.domain.common.Page;
import ${package}.domain.entities.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public interface UserManage {
    User create(@NotBlank String name, @NotBlank @Email String email);

    User getById(@NotBlank String userId);

    Page<User> getPage(@Positive int currentPage, @Positive int pageSize);
}
