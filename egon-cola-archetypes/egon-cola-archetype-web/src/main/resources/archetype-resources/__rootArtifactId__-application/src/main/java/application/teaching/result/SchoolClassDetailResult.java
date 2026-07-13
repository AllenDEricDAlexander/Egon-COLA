package ${package}.application.teaching.result;

import java.util.List;

public record SchoolClassDetailResult(
        String id, String name, String gradeCode, String gradeName, String status, List<String> userIds) {
    public SchoolClassDetailResult { userIds = List.copyOf(userIds); }
}
