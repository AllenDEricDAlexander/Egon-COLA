package top.egon.cola.archetype.source.webopen.application.user.pojo.result;

import java.util.List;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record UserDetailResult(
        Long id,
        String name,
        String email,
        String status,
        List<String> roleCodes) implements BasePojo {

    public UserDetailResult {
        roleCodes = List.copyOf(roleCodes);
    }
}
