package ${package}.facade.user.dto;

import java.io.Serializable;
import java.util.List;

public record PermissionDetailDTO(
        String code,
        String name,
        List<PermissionDetailDTO> children) implements Serializable {
    public PermissionDetailDTO {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
