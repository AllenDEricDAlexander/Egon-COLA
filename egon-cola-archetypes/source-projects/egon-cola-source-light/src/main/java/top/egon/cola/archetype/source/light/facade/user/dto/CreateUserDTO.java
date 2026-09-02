package top.egon.cola.archetype.source.light.facade.user.dto;

import java.io.Serializable;

public record CreateUserDTO(
        String externalId,
        String name,
        String email,
        String operatorId,
        String requestId) implements Serializable {
}
