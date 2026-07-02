package top.egon.light.facade.dto;

import java.util.List;

public record StudentDTO(
        String id,
        String name,
        String email,
        String status,
        List<String> courseIds
) {
}
