package top.egon.cola.component.dtp.admin.manifest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.dtp.admin.types.Response;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dtp")
public class DtpManifestController {

    @Value("${egon.cola.component.dtp.manifest.version:5.2.0-SNAPSHOT}")
    private String version = "5.2.0-SNAPSHOT";

    @GetMapping("/manifest")
    public Response<DtpComponentManifest> manifest() {
        return Response.success(DtpComponentManifest.builder()
                .component("dynamic-thread-pool")
                .name("Dynamic Thread Pool")
                .version(version)
                .enabled(true)
                .baseApi("/api/v1/dtp")
                .frontend(DtpComponentManifest.Frontend.builder()
                        .module("dynamic-thread-pool")
                        .routeBase("/components/dynamic-thread-pool")
                        .menus(List.of(
                                menu("dynamic-thread-pool.apps", "Applications", "/components/dynamic-thread-pool/apps", "dtp:apps:read"),
                                menu("dynamic-thread-pool.events", "Audit Events", "/components/dynamic-thread-pool/events", "dtp:events:read")
                        ))
                        .build())
                .permissions(List.of(
                        permission("dtp:apps:read", "View dynamic thread pool applications"),
                        permission("dtp:executors:read", "View dynamic thread pool executors"),
                        permission("dtp:executors:resize", "Resize platform thread pools"),
                        permission("dtp:executors:virtual-limit", "Update virtual thread concurrency limit"),
                        permission("dtp:events:read", "View dynamic thread pool audit events")
                ))
                .build());
    }

    private DtpComponentManifest.Menu menu(String key, String title, String path, String permission) {
        return DtpComponentManifest.Menu.builder()
                .key(key)
                .title(title)
                .path(path)
                .permission(permission)
                .build();
    }

    private DtpComponentManifest.Permission permission(String code, String name) {
        return DtpComponentManifest.Permission.builder()
                .code(code)
                .name(name)
                .build();
    }
}
