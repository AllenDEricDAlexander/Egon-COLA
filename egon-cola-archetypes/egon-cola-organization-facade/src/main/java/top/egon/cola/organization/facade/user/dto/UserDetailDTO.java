package top.egon.cola.organization.facade.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.util.List;

public record UserDetailDTO(
        @NotNull @Positive Long id,
        String name,
        String email,
        String status,
        List<String> roleCodes) implements Serializable {

    public UserDetailDTO {
        roleCodes = List.copyOf(roleCodes);
    }
}
