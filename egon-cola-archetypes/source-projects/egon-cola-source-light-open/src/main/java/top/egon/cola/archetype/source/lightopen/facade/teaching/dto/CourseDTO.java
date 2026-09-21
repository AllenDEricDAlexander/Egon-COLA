package top.egon.cola.archetype.source.lightopen.facade.teaching.dto;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record CourseDTO(Long id, String code, String name, String status) implements BasePojo {
}
