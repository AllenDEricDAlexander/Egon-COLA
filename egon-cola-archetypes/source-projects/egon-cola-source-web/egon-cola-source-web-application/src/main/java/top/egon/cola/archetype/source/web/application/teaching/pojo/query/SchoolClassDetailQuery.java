package top.egon.cola.archetype.source.web.application.teaching.pojo.query;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record SchoolClassDetailQuery(Long gradeId, Long schoolClassId) implements BasePojo {
}
