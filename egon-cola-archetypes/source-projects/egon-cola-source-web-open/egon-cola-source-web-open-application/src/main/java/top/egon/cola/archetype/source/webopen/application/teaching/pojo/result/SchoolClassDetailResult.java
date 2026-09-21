package top.egon.cola.archetype.source.webopen.application.teaching.pojo.result;

import java.util.List;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record SchoolClassDetailResult(
        Long id, String name, String gradeCode, String gradeName, String status, List<Long> userIds) implements BasePojo {
    public SchoolClassDetailResult { userIds = List.copyOf(userIds); }
}
