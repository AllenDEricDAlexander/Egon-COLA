package top.egon.light.application.manage.student;

import java.util.List;

public record StudentView(
        String id,
        String name,
        String email,
        String status,
        List<String> courseIds
) {
}
