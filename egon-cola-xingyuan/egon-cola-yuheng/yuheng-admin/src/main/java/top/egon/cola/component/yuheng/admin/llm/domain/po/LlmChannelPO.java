package top.egon.cola.component.yuheng.admin.llm.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmProtocolEnum;

/**
 * 中文说明：{@code LlmChannelPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_llm_channel} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code LlmChannelPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_llm_channel} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界由 guarded repository 与 converter 使用；业务 {@code revision} 与不透明协议 key 独立于 MP 的 {@code version}/{@code id}，封闭列以 {@code @EnumValue} 枚举而非 ordinal 落库。/ Use it only at the persistence boundary through the guarded repository and its converter; the business revision and opaque protocol keys stay distinct from the MP version and id, and closed columns persist as {@code @EnumValue} enums rather than ordinals.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_llm_channel", autoResultMap = true)
public class LlmChannelPO extends EgonModel<LlmChannelPO> {

    @TableField("channel_key")
    private String channelKey;

    @TableField("name")
    private String name;

    @TableField("deployment")
    private LlmDeploymentEnum deployment;

    @TableField("protocol")
    private LlmProtocolEnum protocol;

    @TableField("base_url")
    private String baseUrl;

    @TableField(value = "secret_ref", updateStrategy = FieldStrategy.ALWAYS)
    private String secretRef;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("connect_timeout_ms")
    private Integer connectTimeoutMs;

    @TableField("header_timeout_ms")
    private Integer headerTimeoutMs;

    @TableField("idle_timeout_ms")
    private Integer idleTimeoutMs;

    @TableField("total_timeout_ms")
    private Integer totalTimeoutMs;

    @TableField("max_concurrent")
    private Integer maxConcurrent;

    @TableField("revision")
    private Long revision;
}
