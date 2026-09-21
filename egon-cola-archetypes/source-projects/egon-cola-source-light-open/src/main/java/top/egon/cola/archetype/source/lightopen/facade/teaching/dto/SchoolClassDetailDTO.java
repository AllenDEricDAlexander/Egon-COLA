package top.egon.cola.archetype.source.lightopen.facade.teaching.dto;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record SchoolClassDetailDTO(
        Long id,
        String name,
        String semester,
        String status,
        int scheduleCount) implements BasePojo {
}
