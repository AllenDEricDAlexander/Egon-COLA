package top.egon.cola.platform.idp.admin.resource.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 对明确应用集合执行 Resource Server 状态变更的输入。
 *
 * <p>Input for changing Resource Server status for an explicit application set.</p>
 *
 * @param bizCode 业务域；business domain
 * @param environment 环境；environment
 * @param appCodes 明确应用集合；explicit application codes
 * @param action 状态动作；status action
 * @param expectedVersions 各应用期望版本；expected version for each application
 */
public record BatchResourceServerActionDTO(
        @NotBlank String bizCode,
        @NotBlank String environment,
        @NotEmpty List<@NotBlank String> appCodes,
        @NotNull Action action,
        @NotNull Map<String, Long> expectedVersions
) {

    /**
     * 复制批量选择和版本映射。
     *
     * <p>Copies the batch selection and version map.</p>
     */
    public BatchResourceServerActionDTO {
        appCodes = appCodes == null ? null : List.copyOf(appCodes);
        expectedVersions = expectedVersions == null
                ? null : Map.copyOf(expectedVersions);
    }

    /**
     * Resource Server 批量状态动作。
     *
     * <p>Resource Server batch status action.</p>
     */
    public enum Action {

        /** 启用明确选中的应用；enable explicitly selected applications. */
        ENABLE,

        /** 禁用明确选中的应用；disable explicitly selected applications. */
        DISABLE
    }
}
