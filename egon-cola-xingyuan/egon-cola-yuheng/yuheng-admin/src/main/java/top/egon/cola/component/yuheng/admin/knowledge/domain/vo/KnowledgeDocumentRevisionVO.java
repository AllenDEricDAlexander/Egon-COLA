package top.egon.cola.component.yuheng.admin.knowledge.domain.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeRevisionStatusEnum;

import java.time.Instant;

/**
 * 中文说明：{@code KnowledgeDocumentRevisionVO} 是原 API-016 的真实原文版本投影，只暴露抽取文本与
 * 规范化 hash；按合同不返回 raw_bytes 原件字节，也不返回服务端路径。
 * English summary: {@code KnowledgeDocumentRevisionVO} is the API-016 revision projection exposing the extracted
 * text and the canonical hash only; raw_bytes and server-side paths are deliberately absent.
 *
 * 用法 / Usage: 历史版本仅对仍有 KB 权限且资料未 tombstone 的主体可读；
 * hash 为小写十六进制 SHA-256，sourceHash/chunkHash/contentHash 不混用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeDocumentRevisionVO {

    /** 版本 ID / revision id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String id;

    /** 资料稳定 ID，同 KB 校验 / owning document id, validated inside the KB. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String documentId;

    /** 所属知识库 ID / owning KB id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String kbId;

    /** 该版本原文件名，1–255 / stored file name of this revision. */
    @NotBlank
    @Size(max = 255)
    private String fileName;

    /** 验证过的媒体类型 / validated media type. */
    @NotBlank
    @Size(max = 128)
    private String mediaType;

    /** 原件字节数，1–20971520 / original byte count, 1–20MiB. */
    @NotNull
    @Min(1)
    @Max(20_971_520)
    private Long byteCount;

    /** 授权版本的完整抽取文本，有界 UTF-8 / authorized extracted text of this revision, bounded UTF-8. */
    @NotNull
    private String text;

    /** 规范化 SHA-256 小写 hex64 / canonical SHA-256 in lowercase hexadecimal. */
    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String hash;

    /** 版本状态枚举 / revision lifecycle status. */
    @NotNull
    private KnowledgeRevisionStatusEnum status;

    /** 服务端创建时刻，UTC / server-side UTC creation instant. */
    @NotNull
    private Instant createdAt;
}
