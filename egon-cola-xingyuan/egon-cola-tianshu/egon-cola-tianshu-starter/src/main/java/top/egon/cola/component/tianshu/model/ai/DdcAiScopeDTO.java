package top.egon.cola.component.tianshu.model.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Immutable four-dimensional business scope; the authenticated tenant is deliberately absent. */
public record DdcAiScopeDTO(
        @NotBlank @Size(max = 128) @Pattern(regexp = "^[^\\p{Cntrl}]+$") String bizCode,
        @NotBlank @Size(max = 128) @Pattern(regexp = "^[^\\p{Cntrl}]+$") String namespaceCode,
        @NotBlank @Size(max = 32) @Pattern(regexp = "^[^\\p{Cntrl}]+$") String env,
        @NotBlank @Size(max = 128) @Pattern(regexp = "^[^\\p{Cntrl}]+$") String appCode) {

    public DdcAiScopeDTO {
        bizCode = trimIdentity(bizCode);
        namespaceCode = trimIdentity(namespaceCode);
        env = trimIdentity(env);
        appCode = trimIdentity(appCode);
    }

    private static String trimIdentity(String value) {
        return value == null ? null : value.trim();
    }
}
