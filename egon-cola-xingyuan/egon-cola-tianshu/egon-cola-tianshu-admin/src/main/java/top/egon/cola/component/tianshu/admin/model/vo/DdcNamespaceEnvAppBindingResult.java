package top.egon.cola.component.tianshu.admin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DdcNamespaceEnvAppBindingResult {
    private String id;
    private String bizCode;
    private String namespaceId;
    private String namespaceCode;
    private String env;
    private String appId;
    private String appCode;
    private String appName;
    private boolean enabled;
}
