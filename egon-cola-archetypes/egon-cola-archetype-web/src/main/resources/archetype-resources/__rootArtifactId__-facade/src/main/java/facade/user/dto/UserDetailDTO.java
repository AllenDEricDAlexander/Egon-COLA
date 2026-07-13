package ${package}.facade.user.dto;

import java.io.Serializable;
import java.util.List;

public record UserDetailDTO(
        String id,
        String name,
        String email,
        String status,
        List<String> roleCodes) implements Serializable {

    public UserDetailDTO {
        roleCodes = List.copyOf(roleCodes);
    }
}
