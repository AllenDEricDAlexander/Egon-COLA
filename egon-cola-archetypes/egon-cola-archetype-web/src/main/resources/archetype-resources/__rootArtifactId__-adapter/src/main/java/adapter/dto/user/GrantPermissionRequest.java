package ${package}.adapter.dto.user;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionRequest(@NotBlank String permissionCode) {
}
