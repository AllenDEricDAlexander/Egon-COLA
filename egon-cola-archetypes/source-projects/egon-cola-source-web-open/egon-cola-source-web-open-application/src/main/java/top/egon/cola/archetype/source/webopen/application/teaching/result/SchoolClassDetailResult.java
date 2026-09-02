package top.egon.cola.archetype.source.webopen.application.teaching.result;

import java.util.List;

public record SchoolClassDetailResult(
        Long id, String name, String gradeCode, String gradeName, String status, List<Long> userIds) {
    public SchoolClassDetailResult { userIds = List.copyOf(userIds); }
}
