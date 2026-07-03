package ${package}.application.manage.user;

import ${package}.domain.common.Page;
import ${package}.domain.entities.user.User;

public interface UserManage {
    User create(String name, String email);

    User getById(String userId);

    Page<User> getPage(int currentPage, int pageSize);
}
