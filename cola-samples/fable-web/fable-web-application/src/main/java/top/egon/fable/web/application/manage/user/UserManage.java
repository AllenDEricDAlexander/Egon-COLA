package top.egon.fable.web.application.manage.user;

import top.egon.fable.web.domain.common.Page;
import top.egon.fable.web.domain.entities.user.User;

public interface UserManage {
    User create(String name, String email);

    User getById(String userId);

    Page<User> getPage(int currentPage, int pageSize);
}
