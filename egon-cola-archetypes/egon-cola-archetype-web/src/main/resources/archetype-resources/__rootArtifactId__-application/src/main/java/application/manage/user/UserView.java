package ${package}.application.manage.user;

import java.util.List;

public record UserView(String id, String name, String email, String status, List<String> schoolClassIds) {
}
