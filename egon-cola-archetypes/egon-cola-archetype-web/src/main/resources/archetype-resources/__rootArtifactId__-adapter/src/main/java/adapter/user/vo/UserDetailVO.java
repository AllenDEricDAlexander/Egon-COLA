package ${package}.adapter.user.vo;

import java.util.List;

public record UserDetailVO(
        String id,
        String name,
        String email,
        String status,
        List<String> roleCodes) {

    public UserDetailVO {
        roleCodes = List.copyOf(roleCodes);
    }
}
