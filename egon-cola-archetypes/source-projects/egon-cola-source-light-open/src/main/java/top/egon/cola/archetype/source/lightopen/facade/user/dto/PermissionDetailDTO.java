package top.egon.cola.archetype.source.lightopen.facade.user.dto;

import top.egon.cola.component.common.core.pojo.BasePojo;
import java.util.List;

public record PermissionDetailDTO(
        String code,
        String name,
        List<PermissionDetailDTO> children) implements BasePojo {
    public PermissionDetailDTO {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
