package top.egon.cola.component.ddc.admin.manifest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DdcComponentManifest {

    private String component;

    private String displayName;

    private String version;

    private boolean enabled;

    private String baseApiPath;

    private String frontendModuleKey;

    private String routeBase;
}
