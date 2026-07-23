package top.egon.cola.organization.facade.teaching.dto;

import java.util.List;

public record SchoolClassDetailDTO(
        String id, String name, String gradeCode, String gradeName, String status, List<String> userIds) {
    public SchoolClassDetailDTO { userIds = List.copyOf(userIds); }
}
