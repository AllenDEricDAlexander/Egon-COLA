package top.egon.cola.component.yuheng.admin.wiki.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 中文说明：{@code WikiPublicationCommandDTO} 是 CQE Command 载体，承载原 API-027 直接发布草稿的请求体；
 * 内部使用的 expectedPublicationVersion 由短事务读取，不要求前端另取参数。
 * English summary: {@code WikiPublicationCommandDTO} is the CQE command carrier of the API-027 direct
 * publication request; the internal publication CAS version is read in a short transaction, not supplied here.
 *
 * 用法 / Usage: 发布状态、审核状态与策略快照始终服务端派生，caller 无法覆写。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiPublicationCommandDTO {

    /** 待发布的草稿版本 ID / draft revision id to publish. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String draftRevisionId;

    /** 须等于页面当前 revision / must equal the current page revision. */
    @NotNull
    @Min(0)
    private Long expectedRevision;
}
