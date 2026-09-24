package top.egon.cola.component.yuheng.llm.proxy.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code LlmConfigurationRepository} 是 LLM 引擎读取受管配置的唯一业务端口，只提供两类<b>只读</b>类型化读：
 * 一次请求的模型/渠道一致快照，以及有界的模型目录。它刻意<b>不</b>继承任何泛型 CRUD 端口，也不暴露 PO、DAO 或
 * MyBatis 类型，因此 service 与 controller 无法把写语义、租户列或技术 {@code version} 带进业务层；启停、subject
 * 授权、协议/能力与本地 embedding 的过滤顺序属于 {@code LlmRouteSelectionStrategy} 与调用方 Service，本端口只如实
 * 返回当前租户的活跃配置行，绝不伪造渠道、不隐藏悬空 route、也不把读失败变成空成功。租户一律来自部署绑定的受信
 * 身份（MDC），方法签名不接受租户参数，因此越权请求连“选哪个租户”的机会都没有。
 * English summary: {@code LlmConfigurationRepository} is the only business port through which the LLM engine reads
 * managed configuration, offering just two <b>read-only</b> typed reads: the coherent model/channel snapshot for one
 * request and the bounded model catalog. It deliberately extends <b>no</b> generic CRUD port and leaks no PO, DAO or
 * MyBatis type, so a service or controller can never drag write semantics, the tenant column or the technical
 * {@code version} into the business layer. Enablement, subject authorization, protocol/capability and local-embedding
 * filtering belong to {@code LlmRouteSelectionStrategy} and the calling service, while this port returns the active
 * rows of the current tenant as they are: fabricating no channel, hiding no dangling route and turning no read failure
 * into an empty success. Tenancy always comes from the deployment-bound trusted identity (the MDC) and never from a
 * method parameter, so a privileged caller does not even get a chance to choose a tenant.
 *
 * 用法 / Usage: 由 {@code MpLlmConfigurationRepository} 以 MyBatis-Plus 具名只读语句实现，并在受守卫的部署身份上下文
 * 内、于短只读事务中完成读取；事务必须在返回前结束，因为出站 HTTP 绝不能持有数据库事务或连接。缺 alias 返回
 * {@link Optional#empty()} 由调用方映射为 404，配置不合法（如 route 数组不可解析）由实现失败关闭。
 * / Implemented by {@code MpLlmConfigurationRepository} through named MyBatis-Plus read statements inside the guarded
 * deployment identity and a short read-only transaction that must close before returning, because egress HTTP may never
 * hold a database transaction or connection. A missing alias yields {@link Optional#empty()} for the caller to map to
 * 404, while unparsable configuration fails closed in the implementation.
 */
@Validated
public interface LlmConfigurationRepository {

    /**
     * 中文说明：读取一个模型 alias 的只读一致快照：模型侧业务事实，加上其 {@code routes[]} 显式声明并按渠道 key
     * 批量解析出的渠道事实（顺序保持持久层声明顺序，route 数与 key 数分别受 16/64 上限约束）。
     * English summary: Reads the read-only coherent snapshot of one model alias: the model-side business facts plus the
     * channel facts resolved by the channel keys its {@code routes[]} explicitly declare, keeping the persisted
     * declaration order under the separate 16-route and 64-key ceilings.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmConfigurationRepository.findSnapshot(modelKey)}；在请求进入时调用一次，
     * 整个请求（含至多两次尝试）复用同一份快照，不在重试时重读配置。
     * / Call it once as the request arrives and reuse the same snapshot for the whole request, including its at most two
     * attempts, rather than re-reading configuration on a retry.
     * @param modelKey 参数 已校验的客户端 alias，非空、至多 64 且符合受管 key 形态；parameter the validated client alias, non-blank, at most 64 and in the managed key shape.
     * @return 返回 该 alias 的只读快照，alias 不存在或已软删时为 {@link Optional#empty()}；returns the snapshot, or {@link Optional#empty()} when the alias is absent or logically deleted.
     */
    Optional<LlmModelSnapshotBO> findSnapshot(@NotBlank
                                              @Size(max = 64)
                                              @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
                                              String modelKey);

    /**
     * 中文说明：读取当前租户的活跃模型目录，按稳定 {@code id} 升序且有界（配置表规模由部署约束在各 1000 行以内），
     * 不做上游动态发现；返回的快照只保证模型侧字段，其 route 的渠道事实未解析即为 {@code null}，因此目录读不能被
     * 误用为路由依据。
     * English summary: Reads the current tenant's active model catalog ordered by the stable {@code id} ascending and
     * bounded (the configuration tables are deployment-capped at 1000 rows each) with no upstream dynamic discovery. A
     * returned snapshot guarantees only the model-side fields, and a route whose channel facts are unresolved stays
     * {@code null}, so the catalog read may never be mistaken for a routing decision.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmConfigurationRepository.findCatalog()}；由目录入口在其之上继续施加
     * enabled 与 subject 授权过滤后投影为原生响应。
     * / The catalog endpoint keeps applying the enabled and subject-authorization filters above this read before
     * projecting the native response.
     * @return 返回 有界、按 id 升序的只读快照列表，没有配置时为空列表而非 {@code null}；returns the bounded, id-ascending snapshots, an empty list rather than {@code null} when nothing is configured.
     */
    List<LlmModelSnapshotBO> findCatalog();
}
