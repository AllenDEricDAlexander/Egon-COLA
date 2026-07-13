package ${package}.adapter.teaching.vo;

import java.util.List;

public record SchoolClassDetailVO(
        String id, String name, String gradeCode, String gradeName, String status, List<String> userIds) {
    public SchoolClassDetailVO { userIds = List.copyOf(userIds); }
}
