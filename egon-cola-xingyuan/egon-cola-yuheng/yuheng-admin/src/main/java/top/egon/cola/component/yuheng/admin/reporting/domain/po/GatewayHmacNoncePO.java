package top.egon.cola.component.yuheng.admin.reporting.domain.po;

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
 * 中文说明：{@code GatewayHmacNoncePO} 是 MyBatis-Plus 行模型，负责 {@code gateway_hmac_nonce} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayHmacNoncePO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_hmac_nonce} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；nonce 只作为重放防护的不透明键，过期时刻以 UTC {@code Instant} 表示。/ Use it only at the persistence boundary; the nonce stays an opaque replay-protection key and expiry is a UTC {@code Instant}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_hmac_nonce", autoResultMap = true)
public class GatewayHmacNoncePO extends EgonModel<GatewayHmacNoncePO> {

    @TableField("access_key")
    private String accessKey;

    @TableField("nonce")
    private String nonce;

    @TableField("expires_at")
    private Instant expiresAt;
}
