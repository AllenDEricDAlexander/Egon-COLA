package top.egon.cola.component.yuheng.llm.proxy.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.llm.proxy.domain.po.LlmModelPO;

import java.util.List;

/**
 * 中文说明：{@code LlmModelDAO} 是数据面进程自有的 {@code gateway_llm_model} 映射器，只继承 {@link EgonColaMapper}
 * 已声明的受守卫语句（{@code selectActiveById}/{@code selectActiveByIds}/{@code deleteVersionedById}），不重复声明
 * 泛型 {@code T}，也不新增任何写语句：模型 alias 的唯一写入者是控制面，本进程账号只有 SELECT 授权。这里只补
 * Spec §11.2 引擎访问路径所需的具名类型化查询：按 alias 点查与有界目录读。
 * English summary: {@code LlmModelDAO} is the data-plane process' own mapper for {@code gateway_llm_model}. It only
 * inherits the guarded statements already declared by {@link EgonColaMapper} ({@code selectActiveById},
 * {@code selectActiveByIds}, {@code deleteVersionedById}), never redeclares the generic {@code T} and adds no statement
 * that writes: the control plane is the sole writer of model aliases while this process' role holds SELECT grants only.
 * It declares just the additional typed named queries the engine needs for the Spec §11.2 access paths, namely the
 * point read by alias and the bounded catalog read.
 *
 * 用法 / Usage: 由 {@code @MapperScan} 注册，仅经 {@code MpLlmConfigurationRepository} 的受守卫只读边界访问，业务代码
 * 不得直接注入本接口；语句集合见 {@code mybatis/mapper/llm/LlmModelDAO.xml}。
 * / Registered through {@code @MapperScan} and reached only through the guarded read-only boundary of
 * {@code MpLlmConfigurationRepository}; business code must not inject this interface directly. Its statements live in
 * {@code mybatis/mapper/llm/LlmModelDAO.xml}.
 */
public interface LlmModelDAO extends EgonColaMapper<LlmModelPO> {

    /**
     * 中文说明：按客户端 alias（{@code model_key}）点查活跃行，对应 {@code pk_gateway_llm_model} 的等值访问路径，
     * detail 结果 0..1；查不到必须由调用方按 404 语义处理，不得伪造快照。
     * English summary: Point-reads the active row by the client alias ({@code model_key}) along the equality path of
     * {@code pk_gateway_llm_model}, yielding 0..1 detail rows; an absent row must be handled as 404 by the caller and may
     * never be replaced by a fabricated snapshot.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmModelDAO.selectActiveByModelKey(modelKey)}。
     * @param modelKey 参数 已校验的非空 alias；parameter the validated non-blank alias.
     * @return 返回 活跃模型行，缺失为 {@code null}；returns the active model row, or {@code null} when absent.
     */
    LlmModelPO selectActiveByModelKey(@Param("modelKey") String modelKey);

    /**
     * 中文说明：有界读取当前租户的活跃模型目录，按稳定 {@code id} 升序返回，供 {@code GET /v1/models} 使用；
     * 上限必须由调用方传入且为正数，配置表规模由部署侧约束在各 1000 行以内，因此不存在无界全表读。
     * English summary: Reads the current tenant's active model catalog bounded and ordered by the stable {@code id}
     * ascending for {@code GET /v1/models}; the ceiling must be a positive number supplied by the caller, and since the
     * configuration tables are deployment-bounded to 1000 rows each an unbounded scan never exists.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmModelDAO.selectActiveCatalog(limit)}。
     * @param limit 参数 正数行上限；parameter the positive row ceiling.
     * @return 返回 至多 {@code limit} 条活跃模型行，按 id 升序；returns at most {@code limit} active rows ordered by id ascending.
     */
    List<LlmModelPO> selectActiveCatalog(@Param("limit") int limit);
}
