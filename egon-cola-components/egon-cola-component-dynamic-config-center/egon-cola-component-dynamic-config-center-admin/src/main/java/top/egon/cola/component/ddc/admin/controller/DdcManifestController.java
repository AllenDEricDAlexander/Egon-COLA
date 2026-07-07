package top.egon.cola.component.ddc.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;
import top.egon.cola.component.ddc.admin.manifest.DdcComponentManifest;

@RestController
@RequestMapping("/api/v1/ddc")
public class DdcManifestController {

    @Value("${egon.cola.component.ddc.admin.manifest.version:5.2.0-SNAPSHOT}")
    private String version;

    @GetMapping("/manifest")
    public ResultDto<DdcComponentManifest> manifest() {
        return ResultDtos.success(DdcComponentManifest.builder()
                .component("dynamic-config-center")
                .displayName("Dynamic Config Center")
                .version(version)
                .enabled(true)
                .baseApiPath("/api/v1/ddc")
                .frontendModuleKey("dynamic-config-center")
                .routeBase("/components/dynamic-config-center")
                .build());
    }
}
