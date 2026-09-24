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
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;

/**
 * 中文说明：{@code LlmChannelPO} 是 LLM 引擎进程自有的 MyBatis-Plus 行模型，负责 {@code gateway_llm_channel} 表业务列的
 * <b>只读</b>持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。引擎与控制面是两个独立可部署
 * 进程，按 {@code yuheng-mcp-gateway} 的先例各自持有一份同构行模型，因此本类<b>不</b>导入 {@code yuheng-admin} 的任何类型；
 * 列名、类型与封闭枚举必须与 {@code V14} 受管 DDL 逐列一致，任何漂移都应在启动或集成测试中暴露，而不是在运行期兜底。
 * English summary: {@code LlmChannelPO} is the LLM engine process' own MyBatis-Plus row model owning the
 * <b>read-only</b> persistence boundary of the business columns of {@code gateway_llm_channel}, while id, tenant, audit,
 * soft-delete and version stay declared once in {@link EgonModel}. The engine and the control plane are two independently
 * deployable processes and, following the {@code yuheng-mcp-gateway} precedent, each keeps its own isomorphic row model,
 * so this class imports <b>no</b> {@code yuheng-admin} type; column names, types and closed enums must match the managed
 * {@code V14} DDL column for column, and any drift has to surface at startup or in integration tests instead of being
 * papered over at runtime.
 *
 * 用法 / Usage: 只在持久边界由 {@code MpLlmConfigurationRepository} 与 {@code LlmChannelDAO.xml} 的具名语句使用；
 * {@code deployment}/{@code protocol} 以 {@code @EnumValue} 的 wire 文本落库而非 ordinal，业务 {@code revision} 与不透明
 * {@code channelKey} 独立于 MP 的 {@code version}/{@code id}。本进程账号只有 SELECT 授权，任何写入、DDL 与
 * {@code secret_ref} 解析都归控制面，故本类不对 Service 暴露写语义，也不得被当作可写实体使用。
 * / Use it only at the persistence boundary through {@code MpLlmConfigurationRepository} and the named statements of
 * {@code LlmChannelDAO.xml}; {@code deployment}/{@code protocol} persist as their {@code @EnumValue} wire text rather than
 * ordinals, and the business {@code revision} plus opaque {@code channelKey} stay distinct from the MP {@code version} and
 * {@code id}. This process' database role holds only SELECT grants, so writes, DDL and {@code secret_ref} resolution
 * belong to the control plane: the class exposes no write semantics to a service and must never be treated as writable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_llm_channel", autoResultMap = true)
public class LlmChannelPO extends EgonModel<LlmChannelPO> {

    /** 中文说明：不透明渠道 key（{@code ^[a-z0-9][a-z0-9-]{0,63}$}），模型 {@code routes[].channelKey} 逻辑引用它，无物理外键。 English summary: the opaque channel key logically referenced by {@code routes[].channelKey} with no physical foreign key. */
    @TableField("channel_key")
    private String channelKey;

    /** 中文说明：展示名称，仅管理面可读，引擎不把它写入任何响应。 English summary: the display name, readable only by the management plane and never written into an engine response. */
    @TableField("name")
    private String name;

    /** 中文说明：部署形态，封闭集合 LOCAL/CLOUD；embedding 只允许 LOCAL。 English summary: the deployment form, the closed LOCAL/CLOUD set; embedding only allows LOCAL. */
    @TableField("deployment")
    private LlmDeploymentEnum deployment;

    /** 中文说明：上游协议，封闭四种协议；必须与模型 alias 声明的协议及 route 能力一致。 English summary: the upstream protocol, one of the four closed ones; it must agree with the alias-declared protocols and the route capabilities. */
    @TableField("protocol")
    private LlmProtocolEnum protocol;

    /** 中文说明：规范化 HTTPS base URL（无 user-info/query/fragment）；只存在于服务端配置，绝不出现在错误与日志里。 English summary: the normalized HTTPS base URL without user-info, query or fragment; it stays in server-side configuration and never reaches errors or logs. */
    @TableField("base_url")
    private String baseUrl;

    /** 中文说明：{@code secret_ref} 是本地密钥引用而非凭据值，由启动期文件拥有者/模式校验解析一次。 English summary: {@code secret_ref} is a local secret reference rather than a credential value, resolved once under start-up ownership and mode checks. */
    @TableField("secret_ref")
    private String secretRef;

    /** 中文说明：渠道启停位；停用渠道不进入候选集，也不被当作“可放宽”的兜底。 English summary: the enablement flag; a disabled channel never enters the candidate set and is never a fallback to widen toward. */
    @TableField("enabled")
    private Boolean enabled;

    /** 中文说明：连接超时毫秒，由管理面配置校验给出边界，引擎不自行放宽。 English summary: the connect timeout in milliseconds, bounded by management-plane validation and never widened by the engine. */
    @TableField("connect_timeout_ms")
    private Integer connectTimeoutMs;

    /** 中文说明：首包/头部超时毫秒。 English summary: the header or first-packet timeout in milliseconds. */
    @TableField("header_timeout_ms")
    private Integer headerTimeoutMs;

    /** 中文说明：空闲超时毫秒，流式响应两次数据帧之间的最大静默。 English summary: the idle timeout in milliseconds, the maximum silence between two streamed frames. */
    @TableField("idle_timeout_ms")
    private Integer idleTimeoutMs;

    /** 中文说明：整次请求总超时毫秒，与请求级 deadline 取较小者。 English summary: the total per-request timeout in milliseconds, capped by the request-wide deadline. */
    @TableField("total_timeout_ms")
    private Integer totalTimeoutMs;

    /** 中文说明：该渠道在引擎内的并发上限，配合进程内许可与公平性调度。 English summary: the channel's concurrency ceiling inside the engine, driving in-process permits and fairness. */
    @TableField("max_concurrent")
    private Integer maxConcurrent;

    /** 中文说明：业务修订号，与管理面 CAS 同步；与 MP 技术 {@code version} 互不替代。 English summary: the business revision synchronised with the control-plane CAS, never interchangeable with the technical MP {@code version}. */
    @TableField("revision")
    private Long revision;
}
