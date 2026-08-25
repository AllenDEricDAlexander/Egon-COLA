package ${package}.facade.user.dto;

import java.io.Serializable;

public record UserDetailDTO(Long id, String name, String email, String status) implements Serializable {
}
