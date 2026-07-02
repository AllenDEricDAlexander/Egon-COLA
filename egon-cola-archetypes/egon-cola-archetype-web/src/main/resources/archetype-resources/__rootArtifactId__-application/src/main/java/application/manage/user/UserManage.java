package ${package}.application.manage.user;

public interface UserManage {
    UserView create(String name, String email);
    UserView getById(String userId);
}
