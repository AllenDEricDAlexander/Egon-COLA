package ${package}.application.user.result;

import java.util.List;

public record UserDetailResult(
        String id,
        String name,
        String email,
        String status,
        List<String> roleCodes) {

    public UserDetailResult {
        roleCodes = List.copyOf(roleCodes);
    }
}
