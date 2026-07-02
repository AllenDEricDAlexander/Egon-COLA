package top.egon.fable-web.facade.dto.teaching;

import java.util.List;

public record SchoolClassDTO(String id, String name, String gradeName, List<String> userIds) {
}
