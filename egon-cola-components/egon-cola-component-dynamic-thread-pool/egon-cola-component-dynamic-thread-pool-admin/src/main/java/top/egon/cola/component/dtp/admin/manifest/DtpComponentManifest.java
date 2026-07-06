package top.egon.cola.component.dtp.admin.manifest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtpComponentManifest {

    private String component;

    private String name;

    private String version;

    private boolean enabled;

    private String baseApi;

    private Frontend frontend;

    private List<Permission> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Frontend {

        private String module;

        private String routeBase;

        private List<Menu> menus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Menu {

        private String key;

        private String title;

        private String path;

        private String permission;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Permission {

        private String code;

        private String name;
    }
}
