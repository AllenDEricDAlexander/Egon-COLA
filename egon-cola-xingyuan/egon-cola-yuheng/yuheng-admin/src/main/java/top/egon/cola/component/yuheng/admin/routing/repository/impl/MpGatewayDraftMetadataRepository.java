package top.egon.cola.component.yuheng.admin.routing.repository.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.routing.converter.GatewayDraftPersistenceConverter;
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayDraftBO;
import top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayDraftRecordPO;
import top.egon.cola.component.yuheng.admin.routing.repository.GatewayDraftMetadataRepository;
import top.egon.cola.component.yuheng.admin.routing.repository.mp.GatewayDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：{@code MpGatewayDraftMetadataRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责网关草稿头元数据存储（MyBatis-Plus 门面）的职责与边界：以业务端口 {@code GatewayDraftMetadataRepository} 暴露草稿头的查找与保存，读写一律经 {@code gateway_draft} 表的受守卫 {@code EgonColaRepository} 持久化边界（租户过滤、活跃读取、乐观锁 CAS），列与时间映射只经 MapStruct 转换器完成；替换写入在同一事务内先加载当前活跃行，按原业务修订号做 CAS 并在持久边界递增业务修订，0 行影响按修订冲突如实抛出。
 * English summary: {@code MpGatewayDraftMetadataRepository} is a MyBatis-Plus facade store in the current Gateway module owning the gateway draft head metadata responsibility: it serves the {@code GatewayDraftMetadataRepository} business port through the guarded {@code EgonColaRepository} boundary of {@code gateway_draft} (tenant filtering, active-only reads, optimistic-lock CAS) with column and time mapping confined to the MapStruct converter; a replace loads the current active row in the same transaction, performs the original-revision CAS, increments the business revision at the persistence boundary, and a zero-row write surfaces truthfully as a revision conflict.
 *
 * 用法 / Usage: 通过业务端口由 Spring 容器注入；事务归属保持在调用方（{@code gatewayTransactionManager}），本实现只组合守卫持久化协作，不外泄 RecordPO，也不取代后续 Step 归属的 {@code GatewayDraftJpaRepository} MP 适配器。/ Use it through the business port via the Spring container; transaction ownership stays with the caller ({@code gatewayTransactionManager}); this implementation only composes guarded persistence collaboration, never leaks a RecordPO, and does not replace the later-Step MP adapter owning {@code GatewayDraftJpaRepository}.
 */
@Slf4j
@Repository("mpGatewayDraftMetadataRepository")
@RequiredArgsConstructor
@Validated
public class MpGatewayDraftMetadataRepository implements GatewayDraftMetadataRepository {

    @Qualifier("gatewayDraftPersistenceRepository")
    private final GatewayDraftPersistenceRepository draftPersistenceRepository;

    @Qualifier("gatewayDraftPersistenceConverter")
    private final GatewayDraftPersistenceConverter draftPersistenceConverter;

    /**
     * 中文说明：执行 find 操作；按网关组读取当前活跃的草稿头业务载体（活跃唯一性由租户内部分唯一索引保证），组键无法命中时如实返回空，再经转换器投影为业务载体。
     * English summary: Executes the find operation; reads the current active draft head business carrier by gateway group (one active row per tenant and group is guaranteed by the partial unique index), returns empty truthfully for an unmatchable group key, and projects through the converter into the business carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDraftMetadataRepository.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayDraftBO> find(String gatewayGroupId) {
        Long gatewayGroupColumn = gatewayGroupColumn(gatewayGroupId);
        if (gatewayGroupColumn == null) {
            return Optional.empty();
        }
        return draftPersistenceRepository.getOneOpt(
                Wrappers.<GatewayDraftRecordPO>lambdaQuery()
                        .eq(GatewayDraftRecordPO::getGatewayGroupId, gatewayGroupColumn)
        ).map(draftPersistenceConverter::toTarget);
    }

    /**
     * 中文说明：执行 save 操作；等价于草稿头的插入或替换：先按组键读取当前活跃行，缺失则受守卫插入（技术 id/租户/审计/version 由边界补齐，业务修订保持载体携带值），存在则先比对载体业务修订与库内业务修订（不一致即冲突），再沿用原行技术 id/version 做受守卫的乐观锁 CAS 更新，并在新值中递增业务修订；任何 0 行写入或修订不一致都映射为 {@code GatewayAdminRevisionConflictException}，绝不伪造成功。
     * English summary: Executes the save operation; equivalent to an insert-or-replace of the draft head: it loads the current active row by group key, performs a guarded insert when absent (the boundary fills the technical id/tenant/audit/version while the business revision keeps the carrier value), and otherwise compares the carrier revision with the stored one first (a mismatch is a conflict), then reuses the stored technical id/version so the guarded optimistic-lock CAS owns concurrency and increments the business revision in the written value; any zero-row write or revision mismatch maps to {@code GatewayAdminRevisionConflictException} and is never faked as success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDraftMetadataRepository.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @return 返回 save 的处理结果；returns the result of the operation.
     */
    @Override
    public GatewayDraftBO save(GatewayDraftBO draft) {
        GatewayDraftRecordPO candidate = draftPersistenceConverter.toPersistence(draft);
        Optional<GatewayDraftRecordPO> current = draftPersistenceRepository.getOneOpt(
                Wrappers.<GatewayDraftRecordPO>lambdaQuery()
                        .eq(GatewayDraftRecordPO::getGatewayGroupId, candidate.getGatewayGroupId())
        );
        if (current.isEmpty()) {
            if (!draftPersistenceRepository.save(candidate)) {
                throw new GatewayAdminRevisionConflictException(draft.getRevision());
            }
            return draftPersistenceConverter.toTarget(candidate);
        }
        GatewayDraftRecordPO persisted = current.get();
        if (!Objects.equals(persisted.getRevision(), candidate.getRevision())) {
            throw new GatewayAdminRevisionConflictException(persisted.getRevision());
        }
        candidate.setId(persisted.getId());
        candidate.setVersion(persisted.getVersion());
        candidate.setRevision(persisted.getRevision() + 1);
        boolean written = draftPersistenceRepository.updateById(candidate);
        if (!written) {
            throw new GatewayAdminRevisionConflictException(persisted.getRevision());
        }
        return draftPersistenceConverter.toTarget(candidate);
    }

    /**
     * 中文说明：执行 组键投影 操作；该私有辅助方法把端口携带的十进制不透明组标识字符串转换为新 {@code bigint} 列的查询谓词值；null、空白或非十进制输入在旧 VARCHAR 主键模型下永远不可能命中任何草稿头，因此如实返回 null 由调用方映射为“无匹配行”的空读取。
     * English summary: Executes the group-key projection; this private helper converts the decimal opaque group identifier carried by the port into the predicate value of the new {@code bigint} column. A null, blank or non-decimal input could never match any draft head under the legacy VARCHAR primary-key model, so it truthfully yields null and the caller maps that to an empty read.
     *
     * 用法 / Usage: 仅在本类内部为守卫查询准备谓词值时调用；/ Used only inside this class to prepare predicate values for guarded queries; the save path takes the group key already converted by the MapStruct converter.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 组键投影 的处理结果；returns the result of the operation.
     */
    private static Long gatewayGroupColumn(String gatewayGroupId) {
        if (gatewayGroupId == null || gatewayGroupId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(gatewayGroupId.trim());
        } catch (NumberFormatException invalidOpaqueId) {
            return null;
        }
    }
}
