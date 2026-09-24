package top.egon.cola.component.yuheng.admin.routing.repository;


import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayDraftBO;

import java.util.Optional;

/**
 * 中文说明：{@code GatewayDraftMetadataRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关草稿头元数据（草稿头业务载体的查找与保存）的职责与边界；契约只暴露业务对象（BO），不继承 JpaRepository 之类的泛型 CRUD，也不出现在任何签名中的持久化对象（PO）或 DAO。
 * English summary: {@code GatewayDraftMetadataRepository} is an interface contract in the current Gateway module; it owns the gateway draft head metadata responsibility of finding and saving the draft head business carrier, exposing business objects only, inheriting no generic CRUD such as JpaRepository, and leaking no {@code *RecordPO} or DAO in any signature.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件注入该端口，由 MyBatis-Plus 实现（{@code MpGatewayDraftMetadataRepository}）承接事务与守卫持久化协作；业务 revision 的期望校验与递增属于持久边界职责，调用方只读写业务载体。/ Inject the port through the Spring container or an enclosing component; the MyBatis-Plus implementation owns transaction and guarded persistence collaboration, and the business revision expectation check plus increment stay inside the persistence boundary.
 */
@Validated
public interface GatewayDraftMetadataRepository {

    /**
     * 中文说明：执行 find 操作；该方法是 {@code GatewayDraftMetadataRepository} 的调用入口，按网关组标识读取当前活跃的草稿头业务载体，不存在时返回空，不泄漏任何持久化对象。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code GatewayDraftMetadataRepository}, reading the current active draft head business carrier by gateway group identifier, returning empty when absent and leaking no persistence object.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftMetadataRepository.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回草稿头业务载体或空；returns the draft head business carrier, or empty when no active draft head exists for the group.
     */
    Optional<GatewayDraftBO> find(String gatewayGroupId);

    /**
     * 中文说明：执行 save 操作；该方法是 {@code GatewayDraftMetadataRepository} 的调用入口，插入或替换草稿头业务载体并返回权威业务结果；业务修订号的期望比较、原修订 CAS 与递增由持久边界完成，0 行影响不得报告成功。
     * English summary: Executes the save operation; this method is the invocation entry point on {@code GatewayDraftMetadataRepository}, inserting or replacing the draft head business carrier and returning the authoritative business result; the revision expectation check, original-revision CAS and increment happen at the persistence boundary, and a zero-row write is never reported as success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftMetadataRepository.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿头业务载体；parameter draft head business carrier.
     * @return 返回持久化后的权威草稿头业务载体；returns the persisted, authoritative draft head business carrier.
     */
    GatewayDraftBO save(GatewayDraftBO draft);
}
