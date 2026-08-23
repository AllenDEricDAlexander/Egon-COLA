package ${package}.adapter.user.vo;

import java.util.List;

public record PermissionTreeVO(String code, String name, List<PermissionTreeVO> children) {
    public PermissionTreeVO {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
