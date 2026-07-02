package top.egon.fable-web.facade.dto.user;

import java.util.List;

public record UserDTO(String id, String name, String email, String status, List<String> schoolClassIds) {
}
