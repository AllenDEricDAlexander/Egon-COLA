package ${package}.domain.entities.user;

import ${package}.domain.enums.user.RoleStatus;
import ${package}.domain.vos.user.PermissionCode;
import ${package}.domain.vos.user.RoleCode;

import java.util.ArrayList;
import java.util.List;

public final class Role {

    private final String id;
    private final RoleCode code;
    private final String name;
    private final RoleStatus status;
    private final List<PermissionCode> permissionCodes;

    public Role(String id, RoleCode code, String name, RoleStatus status) {
        this(id, code, name, status, List.of());
    }

    public Role(String id, RoleCode code, String name, RoleStatus status, List<PermissionCode> permissionCodes) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.permissionCodes = new ArrayList<>(permissionCodes);
    }

    public void grant(PermissionCode permissionCode) { permissionCodes.add(permissionCode); }

    public String id() { return id; }
    public RoleCode code() { return code; }
    public String name() { return name; }
    public RoleStatus status() { return status; }
    public List<PermissionCode> permissionCodes() { return List.copyOf(permissionCodes); }
}
