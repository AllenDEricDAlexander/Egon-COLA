package top.egon.cola.component.yuheng.admin.knowledge.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

/**
 * 中文说明：{@code KnowledgeUploadCommandDTO} 是原 API-015 multipart 上传的 CQE Command 载体，
 * 承载 {@code fileName/mediaType/content/documentId/expectedRevision}；file 部分以字节进入，1–20MiB。
 * English summary: {@code KnowledgeUploadCommandDTO} is the API-015 multipart upload command carrier; the
 * binary part is held as bytes bounded by 1–20MiB.
 *
 * 用法 / Usage: {@code documentId} 缺失表示创建新资料，存在时 {@code expectedRevision} 必填且 ≥1；
 * 原件字节在 setter/getter 上各自复制一次，避免异步摄取任务读取到被修改的数组；
 * mime 与实际内容联合验证、扩展名白名单属于 Service/Strategy 职责，不在注解中伪造。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeUploadCommandDTO {

    /** 原始文件显示名，1–255，不作为路径 / original display file name, never a path. */
    @NotBlank
    @Size(max = 255)
    private String fileName;

    /** 受支持媒体类型，不能仅信任客户端 Content-Type / validated media type. */
    @Size(max = 128)
    private String mediaType;

    /** 原件字节，1–20971520 / original bytes, 1–20MiB. */
    @NotNull
    @Size(min = 1, max = 20_971_520)
    private byte[] content;

    /** 更新已有资料时必填，须属于路径 kbId / set only when adding a revision to an existing document. */
    @Schema(nullable = true)
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String documentId;

    /** 有 documentId 时必填且 ≥1，创建新资料时不传 / required with documentId, absent on creation. */
    @Schema(nullable = true)
    @Min(1)
    private Long expectedRevision;

    /**
     * 中文说明：返回 content 的副本，调用方修改数组不会影响已进入摄取任务的对象。
     * English summary: Returns a copy of the stored bytes.
     */
    public byte[] getContent() {
        return content == null ? null : content.clone();
    }

    /**
     * 中文说明：写入时复制 content，阻断异步摄取期间的输入篡改。
     * English summary: Defensively copies the incoming bytes.
     */
    public KnowledgeUploadCommandDTO setContent(byte[] content) {
        this.content = content == null ? null : content.clone();
        return this;
    }
}
