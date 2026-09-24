package top.egon.cola.component.yuheng.admin.llm.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.llm.converter.LlmChannelPersistenceConverter;
import top.egon.cola.component.yuheng.admin.llm.converter.LlmModelPersistenceConverter;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmChannelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmModelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmRouteBindingDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.po.LlmChannelPO;
import top.egon.cola.component.yuheng.admin.llm.domain.po.LlmModelPO;
import top.egon.cola.component.yuheng.admin.llm.repository.LlmConfigurationRepository;
import top.egon.cola.component.yuheng.admin.llm.repository.mp.LlmChannelPersistenceRepository;
import top.egon.cola.component.yuheng.admin.llm.repository.mp.LlmModelPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code MpLlmConfigurationRepository} 是 {@code gateway_llm_channel} 与 {@code gateway_llm_model} 两张表的
 * MyBatis-Plus 门面存储，取代原 SQL 的手写访问：读取保留原访问路径的 {@code channel_key}/{@code model_key} 谓词、
 * {@code create_time DESC, id DESC} 次序与独立的匹配总数，写入保留原
 * {@code UPDATE ... WHERE channel_key = :key AND revision = :expected} 的业务版本语义；
 * 列与类型映射（含三个 jsonb 列的结构化集合）只经 MapStruct 持久转换器完成，公开端口不泄漏任何行模型。
 * English summary: {@code MpLlmConfigurationRepository} is the MyBatis-Plus facade store over the
 * {@code gateway_llm_channel} and {@code gateway_llm_model} tables that replaces the legacy hand-written SQL access: the
 * reads keep the original {@code channel_key}/{@code model_key} predicates, the {@code create_time DESC, id DESC} order and
 * the separate matched count, while the writes keep the legacy
 * {@code UPDATE ... WHERE channel_key = :key AND revision = :expected} business-version semantics; column and type mapping
 * (the structured jsonb collections included) happens only in the MapStruct persistence converters and the public port never
 * leaks a row model.
 *
 * 用法 / Usage: 通过业务端口 {@code LlmConfigurationRepository} 以 bean 名 {@code llmConfigurationRepository} 注入，
 * 写入组合在调用方 {@code gatewayTransactionManager} 的同一事务内；所有语句都走该表的受守卫
 * {@code EgonColaRepository} 边界，因此天然带同租户过滤、仅活跃行（{@code deleted_at IS NULL}）与乐观锁 CAS。
 * 影响 0 行、业务 {@code revision} 不匹配或唯一键竞争一律按
 * {@code GatewayAdminRevisionConflictException} 如实抛出，绝不伪造成功。/ Use it through the
 * {@code LlmConfigurationRepository} port under the bean name {@code llmConfigurationRepository}, with writes composed
 * inside the caller's {@code gatewayTransactionManager} transaction. Every statement goes through these tables' guarded
 * {@code EgonColaRepository} boundary, so tenant filtering, active-only rows ({@code deleted_at IS NULL}) and optimistic
 * locking are structural; a zero-row effect, a business {@code revision} mismatch or a unique-key race always surfaces as a
 * {@code GatewayAdminRevisionConflictException} instead of a fake success.
 */
@Slf4j
@Repository("llmConfigurationRepository")
@RequiredArgsConstructor
@Validated
public class MpLlmConfigurationRepository implements LlmConfigurationRepository {

    /** 中文说明：创建意图的乐观版本哨兵值，与命令载体的 {@code expectedRevision = 0} 同义。 English summary: the optimistic revision sentinel that means create intent, matching the command's {@code expectedRevision = 0}. */
    private static final long CREATE_REVISION = 0L;

    /** 中文说明：新建行的权威业务 revision，创建即 1 因为投影合同要求正整数 revision。 English summary: the authoritative revision of a fresh row; one, because the projection contract requires a positive revision. */
    private static final long FIRST_REVISION = 1L;

    /** 中文说明：分页与反查扫描的每页行数，受守卫边界的 maxPageSize 上界之内。 English summary: the page size used by paging and by the reverse-reference scan, inside the guarded boundary's max page size. */
    private static final int SCAN_PAGE_SIZE = 100;

    /** 中文说明：按路由反查模型时的扫描硬上界；超过即失败关闭，不把未扫全当作无反向引用。 English summary: the hard scan bound of the routed-channel reverse lookup; exceeding it fails closed instead of reading an unfinished scan as “no reference”. */
    private static final int SCAN_MAX_ROWS = 1_000;

    @Qualifier("llmChannelPersistenceRepository")
    private final LlmChannelPersistenceRepository channelPersistenceRepository;

    @Qualifier("llmModelPersistenceRepository")
    private final LlmModelPersistenceRepository modelPersistenceRepository;

    @Qualifier("llmChannelPersistenceConverter")
    private final LlmChannelPersistenceConverter channelPersistenceConverter;

    @Qualifier("llmModelPersistenceConverter")
    private final LlmModelPersistenceConverter modelPersistenceConverter;

    /**
     * 中文说明：执行 countChannels 操作；走受守卫的活跃计数（租户与 {@code deleted_at IS NULL} 由边界追加），
     * 与当页读取同谓词，因此分页响应的 total 与实际可见行一致。
     * English summary: Executes the countChannels operation; it uses the guarded active count (the boundary contributes the
     * tenant and the {@code deleted_at IS NULL} filter) under the same predicate as the page read, so the reported total
     * agrees with the rows a caller can actually see.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.countChannels()}。
     * @return 返回 活跃渠道总数；returns the number of active channels.
     */
    @Override
    public long countChannels() {
        return channelPersistenceRepository.count();
    }

    /**
     * 中文说明：执行 findChannelPage 操作；以受守卫的当页查询读取活跃渠道，次序固定 {@code create_time} 倒序并以
     * {@code id} 倒序稳定并列；关闭 MP 的自动 count，因为总数由 {@link #countChannels()} 显式配对给出。
     * English summary: Executes the findChannelPage operation; the guarded page query reads active channels ordered by
     * {@code create_time} descending with a descending {@code id} tie-break, and MyBatis-Plus' automatic count stays off
     * because {@link #countChannels()} supplies the total explicitly.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findChannelPage(page, size)}；空页返回 {@code []}。
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 当页渠道业务载体；returns the channel carriers of that page.
     */
    @Override
    public List<LlmChannelBO> findChannelPage(
            int page,
            int size) {
        return channelPersistenceConverter.toBusinessList(
                channelPersistenceRepository.list(
                        new Page<LlmChannelPO>(page, size, false),
                        orderedChannels()
                )
        );
    }

    /**
     * 中文说明：执行 findChannel 操作；按 {@code channel_key} 在受守卫的活跃集合内取行，
     * 业务唯一键（租户内 {@code channel_key} 加 {@code deleted_at}）保证至多一行，取首行只作为并列防护而不放宽语义。
     * English summary: Executes the findChannel operation; it reads by {@code channel_key} inside the guarded active set, the
     * business unique key ({@code channel_key} plus {@code deleted_at} within the tenant) bounding that to at most one row,
     * and taking the first row is only a tie-break guard rather than a loosening of the semantics.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findChannel(channelKey)}。
     * @param channelKey 参数 渠道稳定 key；parameter channel stable key.
     * @return 返回 渠道业务载体；returns the channel carrier when present.
     */
    @Override
    public Optional<LlmChannelBO> findChannel(String channelKey) {
        return firstChannel(channelKey).map(channelPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 findChannelsByKeys 操作；把路由 key 集合去重后一次 key-IN 读取活跃渠道，
     * 重复 key 不产生第二次数据库访问，未知 key 由调用方如实视为缺失渠道。
     * English summary: Executes the findChannelsByKeys operation; the routed keys are deduplicated and read in one key-IN batch
     * of active channels, a repeated key costing no second database access and an unknown key staying honestly absent.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findChannelsByKeys(channelKeys)}；
     * 空集合与超过 64 个 key 由端口注解校验先拒绝。
     * @param channelKeys 参数 渠道稳定 key 集合；parameter channel stable keys.
     * @return 返回 命中的渠道业务载体列表；returns the matched channel carriers.
     */
    @Override
    public List<LlmChannelBO> findChannelsByKeys(Collection<String> channelKeys) {
        Set<String> distinct = new LinkedHashSet<>(channelKeys);
        if (distinct.isEmpty()) {
            return List.of();
        }
        return channelPersistenceConverter.toBusinessList(
                channelPersistenceRepository.list(
                        Wrappers.<LlmChannelPO>lambdaQuery()
                                .in(LlmChannelPO::getChannelKey, distinct)
                )
        );
    }

    /**
     * 中文说明：执行 saveChannel 操作；先按 {@code channel_key} 读取活跃行——缺失即创建意图，要求载体 revision 为
     * 0 哨兵值并以权威 {@code revision = 1} 走受守卫插入（技术列由边界补齐，唯一键竞争按冲突抛出而不回读，
     * 因为该事务已被约束违例中止）；存在则要求库中 revision 与载体期望值一致，随后沿用原行技术
     * {@code id}/{@code version} 以 {@code revision + 1} 做乐观锁 CAS 整行覆盖业务列。
     * English summary: Executes the saveChannel operation; it loads the active row by {@code channel_key} first — absence is the
     * create intent, requiring the zero sentinel on the carrier and performing a guarded insert at the authoritative
     * {@code revision = 1} (the boundary fills the technical columns and a unique-key race raises a conflict without a
     * reload, since the transaction is already aborted by the constraint violation); presence requires the stored revision to
     * equal the carrier's expectation and then performs an optimistic-lock CAS with {@code revision + 1} that overwrite-replaces
     * the business columns while reusing the original row's technical {@code id}/{@code version}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.saveChannel(channel)}；成功时把权威 revision 与
     * 审计时刻回写进入参载体并返回同一实例，调用方须在 {@code gatewayTransactionManager} 事务内携带可信租户上下文调用。
     * @param channel 参数 渠道完整保存载体；parameter the full channel save carrier.
     * @return 返回 已提交的渠道业务载体；returns the committed channel carrier.
     */
    @Override
    public LlmChannelBO saveChannel(LlmChannelBO channel) {
        Optional<LlmChannelPO> current = firstChannel(channel.getChannelKey());
        if (current.isEmpty()) {
            return insertChannel(channel);
        }
        return replaceChannel(channel, current.get());
    }

    /**
     * 中文说明：执行 findModelsByChannelKey 操作；由于 {@code routes} 是 jsonb 列且受守卫 DAO/XML 只提供按主键与
     * key-IN 的活跃读取，这里按 {@code create_time} 倒序有界分页扫描活跃模型行，逐行解码为业务载体后比对
     * route 的渠道 key；扫描超过硬上界按内部错误失败关闭，避免把未扫全当成“没有反向引用”。
     * English summary: Executes the findModelsByChannelKey operation; because {@code routes} is a jsonb column and the guarded
     * DAO/XML offers only active reads by primary key and by key-IN, active model rows are scanned in bounded
     * {@code create_time} descending pages, each row being decoded onto its carrier before the routed channel key is compared.
     * Exceeding the hard scan bound fails closed as an internal error instead of treating an unfinished scan as “no reverse
     * reference”.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findModelsByChannelKey(channelKey)}；
     * 无引用时返回空列表；这条访问路径的成本受 1000 行扫描上界约束。
     * @param channelKey 参数 渠道稳定 key；parameter channel stable key.
     * @return 返回 引用该渠道的模型业务载体列表；returns the model carriers routing at this channel.
     */
    @Override
    public List<LlmModelBO> findModelsByChannelKey(String channelKey) {
        List<LlmModelBO> matched = new ArrayList<>();
        for (int page = 1; ; page = page + 1) {
            List<LlmModelPO> rows = modelPersistenceRepository.list(
                    new Page<LlmModelPO>(page, SCAN_PAGE_SIZE, false),
                    orderedModels()
            );
            for (LlmModelPO row : rows) {
                LlmModelBO carrier = modelPersistenceConverter.toBusiness(row);
                if (routesChannel(carrier, channelKey)) {
                    matched.add(carrier);
                }
            }
            if (rows.size() < SCAN_PAGE_SIZE) {
                return matched;
            }
            if (page * SCAN_PAGE_SIZE >= SCAN_MAX_ROWS) {
                log.error("YUHENG_ADMIN_LLM_MODEL_SCAN_LIMIT channelKey={} bound={}", channelKey, SCAN_MAX_ROWS);
                throw new IllegalStateException("YUHENG_ADMIN_LLM_MODEL_SCAN_LIMIT");
            }
        }
    }

    /**
     * 中文说明：执行 countModels 操作；与渠道计数同口径的受守卫活跃计数，供分页响应携带真实总数。
     * English summary: Executes the countModels operation; the guarded active count under the same predicate as the channel
     * count so the page response carries a real total.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.countModels()}。
     * @return 返回 活跃模型总数；returns the number of active models.
     */
    @Override
    public long countModels() {
        return modelPersistenceRepository.count();
    }

    /**
     * 中文说明：执行 findModelPage 操作；受守卫的当页查询读取活跃模型行，三个 jsonb 列由持久转换器解码为
     * 结构化集合，次序与渠道分页一致且总数由 {@link #countModels()} 配对给出。
     * English summary: Executes the findModelPage operation; the guarded page query reads active model rows, the three jsonb
     * columns decoding into structured collections in the persistence converter, under the channel page ordering with the
     * total supplied by {@link #countModels()}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findModelPage(page, size)}；空页返回 {@code []}。
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 当页模型业务载体；returns the model carriers of that page.
     */
    @Override
    public List<LlmModelBO> findModelPage(
            int page,
            int size) {
        return modelPersistenceConverter.toBusinessList(
                modelPersistenceRepository.list(
                        new Page<LlmModelPO>(page, size, false),
                        orderedModels()
                )
        );
    }

    /**
     * 中文说明：执行 findModel 操作；按 {@code model_key} 在受守卫的活跃集合内取行，租户内业务唯一键保证至多一行。
     * English summary: Executes the findModel operation; it reads by {@code model_key} inside the guarded active set, the
     * tenant-scoped business unique key bounding that to at most one row.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findModel(modelKey)}。
     * @param modelKey 参数 模型稳定 key；parameter model stable key.
     * @return 返回 模型业务载体；returns the model carrier when present.
     */
    @Override
    public Optional<LlmModelBO> findModel(String modelKey) {
        return firstModel(modelKey).map(modelPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 saveModel 操作；与渠道保存共用创建/替换的 CAS 语义，作用在 {@code model_key} 上，
     * {@code protocols}/{@code allowedSubjects}/{@code routes} 三个 jsonb 列随业务整行替换一并写回；
     * 嵌入空间不变式由业务入口复核，本方法不静默放宽。
     * English summary: Executes the saveModel operation; it shares the create-versus-replace CAS semantics of the channel save
     * on {@code model_key} and writes the three jsonb columns {@code protocols}/{@code allowedSubjects}/{@code routes} back as
     * part of the business row replacement. The embedding-space invariant stays with the owning service and is never
     * silently relaxed here.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.saveModel(model)}；成功时把权威 revision 与
     * 审计时刻回写进入参载体并返回同一实例。
     * @param model 参数 模型完整保存载体；parameter the full model save carrier.
     * @return 返回 已提交的模型业务载体；returns the committed model carrier.
     */
    @Override
    public LlmModelBO saveModel(LlmModelBO model) {
        Optional<LlmModelPO> current = firstModel(model.getModelKey());
        if (current.isEmpty()) {
            return insertModel(model);
        }
        return replaceModel(model, current.get());
    }

    /**
     * 中文说明：以 0 哨兵期望创建渠道行；期望值非 0 说明前提已被破坏（该行应在入口已存在）而按冲突抛出，
     * 插入 0 行同样按冲突抛出；唯一键竞争事务已中止，因此携带期望哨兵而非回读既有 revision。
     * English summary: Creates the channel row under the zero sentinel expectation; a different expectation means the entry
     * point's precondition has been broken and raises a conflict, as does a zero-row insert. A unique-key race arrives with
     * the transaction already aborted, so the expectation sentinel is reported instead of a reload.
     * @param channel 参数 渠道载体；parameter the channel carrier.
     * @return 返回 已提交的渠道载体；returns the committed channel carrier.
     */
    private LlmChannelBO insertChannel(LlmChannelBO channel) {
        if (channel.getRevision() != CREATE_REVISION) {
            throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
        }
        LlmChannelPO candidate = channelPersistenceConverter.newRow(channel);
        candidate.setId(null);
        candidate.setRevision(FIRST_REVISION);
        try {
            if (!channelPersistenceRepository.save(candidate)) {
                throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
            }
        } catch (DuplicateKeyException raced) {
            log.warn("YUHENG_ADMIN_LLM_CHANNEL_CREATE_RACED channelKey={}", channel.getChannelKey(), raced);
            throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
        }
        return authoritative(channel, candidate);
    }

    /**
     * 中文说明：按库中现值 CAS 替换渠道行；期望 revision 不符即携带现值抛出 409，随后沿用原行技术定位与
     * 乐观锁版本以 {@code revision + 1} 更新，0 行影响按冲突抛出。
     * English summary: Replaces the channel row under the stored revision; a mismatched expectation raises 409 carrying the
     * stored value, and the update then reuses the original row's technical locator and lock version to advance
     * {@code revision + 1}, a zero-row effect being a conflict.
     * @param channel 参数 渠道载体；parameter the channel carrier.
     * @param persisted 参数 已加载活跃行；parameter the loaded active row.
     * @return 返回 已提交的渠道载体；returns the committed channel carrier.
     */
    private LlmChannelBO replaceChannel(
            LlmChannelBO channel,
            LlmChannelPO persisted) {
        long stored = revisionOf(persisted.getRevision());
        if (channel.getRevision() != stored) {
            throw new GatewayAdminRevisionConflictException(stored);
        }
        channelPersistenceConverter.applyBusiness(channel, persisted);
        persisted.setRevision(stored + 1);
        if (!channelPersistenceRepository.updateById(persisted)) {
            throw new GatewayAdminRevisionConflictException(stored);
        }
        return authoritative(channel, persisted);
    }

    /**
     * 中文说明：以 0 哨兵期望创建模型行，语义与渠道创建一致。
     * English summary: Creates the model row under the zero sentinel expectation, with the channel create semantics.
     * @param model 参数 模型载体；parameter the model carrier.
     * @return 返回 已提交的模型载体；returns the committed model carrier.
     */
    private LlmModelBO insertModel(LlmModelBO model) {
        if (model.getRevision() != CREATE_REVISION) {
            throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
        }
        LlmModelPO candidate = modelPersistenceConverter.newRow(model);
        candidate.setId(null);
        candidate.setRevision(FIRST_REVISION);
        try {
            if (!modelPersistenceRepository.save(candidate)) {
                throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
            }
        } catch (DuplicateKeyException raced) {
            log.warn("YUHENG_ADMIN_LLM_MODEL_CREATE_RACED modelKey={}", model.getModelKey(), raced);
            throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
        }
        return authoritative(model, candidate);
    }

    /**
     * 中文说明：按库中现值 CAS 替换模型行，语义与渠道替换一致。
     * English summary: Replaces the model row under the stored revision with the channel replace semantics.
     * @param model 参数 模型载体；parameter the model carrier.
     * @param persisted 参数 已加载活跃行；parameter the loaded active row.
     * @return 返回 已提交的模型载体；returns the committed model carrier.
     */
    private LlmModelBO replaceModel(
            LlmModelBO model,
            LlmModelPO persisted) {
        long stored = revisionOf(persisted.getRevision());
        if (model.getRevision() != stored) {
            throw new GatewayAdminRevisionConflictException(stored);
        }
        modelPersistenceConverter.applyBusiness(model, persisted);
        persisted.setRevision(stored + 1);
        if (!modelPersistenceRepository.updateById(persisted)) {
            throw new GatewayAdminRevisionConflictException(stored);
        }
        return authoritative(model, persisted);
    }

    /**
     * 中文说明：按 {@code channel_key} 取受守卫活跃行首行，重复键不抛异常而按“已成立的那一行”处理。
     * English summary: Reads the first guarded active row by {@code channel_key}, a duplicate key being handled as the
     * established row instead of an exception.
     * @param channelKey 参数 渠道稳定 key；parameter channel stable key.
     * @return 返回 活跃行；returns the active row when present.
     */
    private Optional<LlmChannelPO> firstChannel(String channelKey) {
        return channelPersistenceRepository.list(
                Wrappers.<LlmChannelPO>lambdaQuery()
                        .eq(LlmChannelPO::getChannelKey, channelKey)
        ).stream().findFirst();
    }

    /**
     * 中文说明：按 {@code model_key} 取受守卫活跃行首行。
     * English summary: Reads the first guarded active row by {@code model_key}.
     * @param modelKey 参数 模型稳定 key；parameter model stable key.
     * @return 返回 活跃行；returns the active row when present.
     */
    private Optional<LlmModelPO> firstModel(String modelKey) {
        return modelPersistenceRepository.list(
                Wrappers.<LlmModelPO>lambdaQuery()
                        .eq(LlmModelPO::getModelKey, modelKey)
        ).stream().findFirst();
    }

    /**
     * 中文说明：判断模型载体的路由绑定是否指向该渠道 key；routes 缺失或为空按无引用处理。
     * English summary: Decides whether the carrier's route bindings address this channel key, absent or empty routes meaning
     * no reference.
     * @param carrier 参数 模型载体；parameter the model carrier.
     * @param channelKey 参数 渠道稳定 key；parameter channel stable key.
     * @return 返回 是否被引用；returns whether the channel is referenced.
     */
    private static boolean routesChannel(
            LlmModelBO carrier,
            String channelKey) {
        List<LlmRouteBindingDTO> routes = carrier.getRoutes();
        if (routes == null) {
            return false;
        }
        return routes.stream().anyMatch(route -> channelKey.equals(route.getChannelKey()));
    }

    /**
     * 中文说明：渠道分页的稳定次序谓词：{@code create_time} 倒序并以 {@code id} 倒序打破并列；
     * 只构造受守卫的 lambda 查询，租户与活跃谓词由边界追加。
     * English summary: The stable channel page order predicate: {@code create_time} descending with a descending {@code id}
     * tie-break; it builds only the guarded lambda query while the boundary contributes the tenant and active-row filters.
     * @return 返回 渠道活跃行排序查询；returns the ordered query over active channel rows.
     */
    private static LambdaQueryWrapper<LlmChannelPO> orderedChannels() {
        return Wrappers.<LlmChannelPO>lambdaQuery()
                .orderByDesc(LlmChannelPO::getCreateTime)
                .orderByDesc(LlmChannelPO::getId);
    }

    /**
     * 中文说明：模型分页与按路由反查扫描共用的稳定次序谓词，与渠道一致。
     * English summary: The stable ordering predicate shared by the model page and by the routed reverse scan, matching the
     * channel one.
     * @return 返回 模型活跃行排序查询；returns the ordered query over active model rows.
     */
    private static LambdaQueryWrapper<LlmModelPO> orderedModels() {
        return Wrappers.<LlmModelPO>lambdaQuery()
                .orderByDesc(LlmModelPO::getCreateTime)
                .orderByDesc(LlmModelPO::getId);
    }

    /**
     * 中文说明：把仓储侧权威的技术与业务值回写进入参载体并返回同一对象，取代旧实现的“写后不自知”；
     * 只回写不透明主键、权威 {@code revision} 与审计时刻，业务列以载体为准。
     * English summary: Copies the repository-authoritative technical and business values back into the given carrier and returns
     * that same object, replacing the legacy write-without-feedback behaviour; only the opaque identifier, the authoritative
     * {@code revision} and the audit instants are synced back while the business columns stay as the carrier holds them.
     * @param carrier 参数 入参载体；parameter the incoming carrier.
     * @param row 参数 仓储侧行；parameter the repository-side row.
     * @return 返回 权威载体；returns the authoritative carrier.
     */
    private static LlmChannelBO authoritative(
            LlmChannelBO carrier,
            LlmChannelPO row) {
        carrier.setId(row.getId() == null ? null : Long.toString(row.getId()));
        carrier.setRevision(revisionOf(row.getRevision()));
        carrier.setCreatedAt(row.getCreateTime());
        carrier.setUpdatedAt(row.getUpdateTime());
        log.debug("gateway_llm_channel {} resolved to authoritative revision {}", row.getChannelKey(), row.getRevision());
        return carrier;
    }

    /**
     * 中文说明：模型侧的权威值回写，语义与渠道侧一致。
     * English summary: The model-side authoritative write-back, with the channel-side semantics.
     * @param carrier 参数 入参载体；parameter the incoming carrier.
     * @param row 参数 仓储侧行；parameter the repository-side row.
     * @return 返回 权威载体；returns the authoritative carrier.
     */
    private static LlmModelBO authoritative(
            LlmModelBO carrier,
            LlmModelPO row) {
        carrier.setId(row.getId() == null ? null : Long.toString(row.getId()));
        carrier.setRevision(revisionOf(row.getRevision()));
        carrier.setCreatedAt(row.getCreateTime());
        carrier.setUpdatedAt(row.getUpdateTime());
        log.debug("gateway_llm_model {} resolved to authoritative revision {}", row.getModelKey(), row.getRevision());
        return carrier;
    }

    /**
     * 中文说明：把可空 bigint 业务 revision 读为 long，列缺失即 0（新表尚无历史行，缺失只可能来自未盖章的行）。
     * English summary: Reads the nullable bigint business revision as a long, an absent column being zero (no legacy rows exist,
     * so an unset value can only come from an unstamped row).
     * @param revision 参数 列值；parameter column value.
     * @return 返回 业务 revision；returns the business revision.
     */
    private static long revisionOf(Long revision) {
        return revision == null ? CREATE_REVISION : revision;
    }
}
