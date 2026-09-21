package top.egon.cola.archetype.source.web.application.teaching.pojo.result;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record GradeDetailResult(Long id, String code, String name, String status) implements BasePojo {
}
