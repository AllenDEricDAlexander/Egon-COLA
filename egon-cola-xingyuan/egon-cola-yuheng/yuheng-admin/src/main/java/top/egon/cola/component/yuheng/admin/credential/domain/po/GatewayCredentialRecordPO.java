package top.egon.cola.component.yuheng.admin.credential.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.time.Instant;

/**
 * 中文说明：{@code GatewayCredentialRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_application_credential} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayCredentialRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_application_credential} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；只保存密文与外部密钥引用，明文不出持久层，有效期以 UTC {@code Instant} 表示；{@code status} 是开放 varchar 列，故按原 wire 字符串存取。/ Use it only at the persistence boundary; only ciphertext and an external key reference are stored, validity is kept as UTC {@code Instant}, and the open {@code status} column persists its original wire string.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_application_credential", autoResultMap = true)
public class GatewayCredentialRecordPO extends EgonModel<GatewayCredentialRecordPO> {

    @TableField("application_id")
    private Long applicationId;

    @TableField("access_key")
    private String accessKey;

    @TableField("secret_ciphertext")
    private String secretCiphertext;

    @TableField("secret_reference")
    private String secretReference;

    @TableField("key_version")
    private String keyVersion;

    @TableField("status")
    private String status;

    @TableField("valid_from")
    private Instant validFrom;

    @TableField("valid_until")
    private Instant validUntil;
}
