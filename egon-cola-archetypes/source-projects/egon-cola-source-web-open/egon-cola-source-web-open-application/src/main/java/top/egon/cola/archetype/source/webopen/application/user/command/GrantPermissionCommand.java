package top.egon.cola.archetype.source.webopen.application.user.command;

public record GrantPermissionCommand(String requestId, String roleCode, String permissionCode) {
}
