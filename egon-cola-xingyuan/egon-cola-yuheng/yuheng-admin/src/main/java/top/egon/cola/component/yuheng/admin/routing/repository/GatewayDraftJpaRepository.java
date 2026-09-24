package top.egon.cola.component.yuheng.admin.routing.repository;


import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayDraftBO;

/**
 * 中文说明：{@code GatewayDraftJpaRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关草稿Repository相关的职责与边界。
 * English summary: {@code GatewayDraftJpaRepository} is an interface contract in the current Gateway module; it owns the gateway draft repository-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Validated
public interface GatewayDraftJpaRepository {
    /**
     * 中文说明：按主键读取草稿头业务载体，找不到时返回空；草稿头不再由 Spring Data 实体管理。
     * English summary: Reads the draft head business carrier by primary key and returns empty when absent; the draft head is no longer a Spring Data managed entity.
     *
     * 用法 / Usage: {@code GatewayDraftJpaRepository.findById(...)} 由发布与草稿流程在读取草稿头时调用。/ Called by the release and draft flows when loading the draft head.
     * @param id 参数 草稿头主键；parameter draft head identifier.
     * @return 返回草稿头业务载体或空；returns the draft head carrier or empty.
     */
    Optional<GatewayDraftBO> findById(String id);

    /**
     * 中文说明：写入或更新草稿头业务载体并返回持久化结果；MP 守卫写入即时生效，修订号由持久边界递增。
     * English summary: Persists the draft head business carrier and returns the stored carrier; the guarded MP write is immediate and the persistence boundary increments the revision.
     *
     * 用法 / Usage: {@code GatewayDraftJpaRepository.save(...)} 由草稿与发布流程写入草稿头时调用。/ Called by the draft and release flows when writing the draft head.
     * @param draft 参数 草稿头业务载体；parameter draft head carrier.
     * @return 返回持久化后的草稿头载体；returns the persisted draft head carrier.
     */
    GatewayDraftBO save(GatewayDraftBO draft);

    /**
     * 中文说明：保留既有调用方的写序语义；MP 路径写入即时落库，因此该方法只声明顺序边界，不做实体脏检查。
     * English summary: Preserves the write-ordering expectation of existing callers; MP writes already land in the database, so this method declares the ordering boundary only and performs no entity dirty checking.
     *
     * 用法 / Usage: {@code GatewayDraftJpaRepository.flush()} 在随后必须读到刚写入草稿头的路径上调用。/ Called where the draft head must be readable immediately afterwards.
     */
    void flush();
}
