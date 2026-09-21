package top.egon.cola.archetype.source.webopen.adapter.user.pojo.vo;

import java.util.List;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record UserDetailVO(
        String id,
        String name,
        String email,
        String status,
        List<String> roleCodes) implements BasePojo {

    public UserDetailVO {
        roleCodes = List.copyOf(roleCodes);
    }
}
