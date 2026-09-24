package top.egon.cola.component.yuheng.llm.proxy.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmModelKindEnum;

/**
 * 中文说明：{@code LlmModelPO} 是 LLM 引擎进程自有的 MyBatis-Plus 行模型，负责 {@code gateway_llm_model} 表业务列的
 * <b>只读</b>持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。它与控制面同名类是两个进程
 * 各自声明的同构副本，按 {@code yuheng-mcp-gateway} 先例不互相导入；{@code routes[].channelKey} 只是被 Service 校验的
 * 逻辑引用，数据库里没有伪外键，因此读到的 route 完全可能是悬空的，由 {@code MpLlmConfigurationRepository} 与其
 * 消费者如实处理。
 * English summary: {@code LlmModelPO} is the LLM engine process' own MyBatis-Plus row model owning the <b>read-only</b>
 * persistence boundary of the business columns of {@code gateway_llm_model}, while id, tenant, audit, soft-delete and
 * version stay declared once in {@link EgonModel}. It is the engine-side isomorphic twin of the control-plane class of
 * the same name, and following the {@code yuheng-mcp-gateway} precedent neither imports the other;
 * {@code routes[].channelKey} is only a Service-validated logical reference with no fake foreign key in the database, so a
 * route read back may well dangle, which {@code MpLlmConfigurationRepository} and its consumers handle honestly.
 *
 * 用法 / Usage: 只在持久边界由 {@code MpLlmConfigurationRepository} 与 {@code LlmModelDAO.xml} 的具名语句使用。
 * 三个 jsonb 列（{@code protocols}/{@code allowed_subjects}/{@code routes}）在本进程按 {@code String} 承载已编码 JSON
 * 文本，读路径由 PostgreSQL 的 {@code getString} 原样取回、再由 repository 用 Jackson 解成类型化 BO：引擎对这两张表
 * 只有 SELECT 授权、永不写 jsonb，因此不引入控制面自有的 jsonb 处理器，也不跨进程引用其类名，避免把列绑定与一个
 * 不属于本部署单元的类耦合。{@code kind} 以 {@code @EnumValue} 文本落库，业务 {@code revision} 独立于 MP
 * {@code version}。/ Use it only at the persistence boundary through {@code MpLlmConfigurationRepository} and the named
 * statements of {@code LlmModelDAO.xml}. The three jsonb columns ({@code protocols}/{@code allowed_subjects}/
 * {@code routes}) are carried here as {@code String} holding the encoded JSON text: the read path takes them back
 * verbatim through PostgreSQL {@code getString} and the repository decodes them with Jackson into typed BOs. Because the
 * engine holds SELECT-only grants on these two tables and never writes jsonb, it neither adopts the control plane's own
 * jsonb handler nor references a class name across processes, keeping column binding free of a type outside this
 * deployable. {@code kind} persists as {@code @EnumValue} text and the business {@code revision} stays distinct from the
 * MP {@code version}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_llm_model", autoResultMap = true)
public class LlmModelPO extends EgonModel<LlmModelPO> {

    /** 中文说明：客户端可见的模型 alias（{@code ^[a-z0-9][a-z0-9-]{0,63}$}），不是供应商真实模型名。 English summary: the client-facing model alias, never the vendor's real model name. */
    @TableField("model_key")
    private String modelKey;

    /** 中文说明：展示名称，只在目录与审计中出现，不参与路由判定。 English summary: the display name, used by the catalog and audit only and never part of routing. */
    @TableField("name")
    private String name;

    /** 中文说明：模型种类 CHAT/EMBEDDING，封闭集合；种类决定 dimensions 与 embedding_space_id 的必填形态。 English summary: the closed CHAT/EMBEDDING kind, which decides how dimensions and embedding_space_id must be shaped. */
    @TableField("kind")
    private LlmModelKindEnum kind;

    /** 中文说明：jsonb 协议数组的原始文本，非空、唯一、1–4 项；语义解码在 repository 完成。 English summary: the raw text of the jsonb protocol array, non-empty, unique, 1–4 entries; semantic decoding happens in the repository. */
    @TableField("protocols")
    private String protocols;

    /** 中文说明：alias 启停位；停用后新请求一律 404，且不迁移到任意其他模型。 English summary: the alias enablement flag; once disabled new requests get 404 and are never migrated onto an arbitrary model. */
    @TableField("enabled")
    private Boolean enabled;

    /** 中文说明：向量维度，EMBEDDING 必须 1..16000 且等于部署实际维度，CHAT 必须为 NULL。 English summary: the vector dimension, 1..16000 and equal to the deployed dimension for EMBEDDING, NULL for CHAT. */
    @TableField("dimensions")
    private Integer dimensions;

    /** 中文说明：嵌入空间稳定身份，被资料引用后不得变更；CHAT 为 NULL。 English summary: the stable embedding-space identity, immutable once referenced by material and NULL for CHAT. */
    @TableField("embedding_space_id")
    private String embeddingSpaceId;

    /** 中文说明：jsonb SERVICE subject 数组的原始文本，1–100 项；缺失或空数组表示禁止全部调用者，绝不视为放行。 English summary: the raw text of the jsonb SERVICE subject array, 1–100 entries; absence or an empty array forbids every caller and is never read as permission. */
    @TableField("allowed_subjects")
    private String allowedSubjects;

    /** 中文说明：jsonb route 数组的原始文本，1–16 项有序，字段为 channelKey/upstreamModel/priority/weight/capabilities。 English summary: the raw text of the ordered 1–16 entry jsonb route array whose fields are channelKey/upstreamModel/priority/weight/capabilities. */
    @TableField("routes")
    private String routes;

    /** 中文说明：业务修订号，与管理面 CAS 同步；与 MP 技术 {@code version} 互不替代。 English summary: the business revision synchronised with the control-plane CAS, never interchangeable with the technical MP {@code version}. */
    @TableField("revision")
    private Long revision;
}
