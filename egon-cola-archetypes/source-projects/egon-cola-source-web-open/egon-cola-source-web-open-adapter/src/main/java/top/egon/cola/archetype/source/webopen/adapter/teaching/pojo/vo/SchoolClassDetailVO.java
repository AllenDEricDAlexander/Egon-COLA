package top.egon.cola.archetype.source.webopen.adapter.teaching.pojo.vo;

import java.util.List;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record SchoolClassDetailVO(
        String id, String name, String gradeCode, String gradeName, String status, List<String> userIds) implements BasePojo {
    public SchoolClassDetailVO { userIds = List.copyOf(userIds); }
}
