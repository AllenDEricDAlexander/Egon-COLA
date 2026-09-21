package top.egon.cola.archetype.source.web.adapter.teaching.pojo.dto;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateSchoolClassMessage(String requestId, String name, String gradeCode) implements BasePojo {
}
