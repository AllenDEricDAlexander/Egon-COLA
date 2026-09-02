package top.egon.cola.archetype.source.web.application.user.command;

public record GrantPermissionCommand(String requestId, String roleCode, String permissionCode) {
}
