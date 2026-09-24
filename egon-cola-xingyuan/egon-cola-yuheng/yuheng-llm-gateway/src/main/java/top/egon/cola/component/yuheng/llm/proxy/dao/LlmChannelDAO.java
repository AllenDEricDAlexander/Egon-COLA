package top.egon.cola.component.yuheng.llm.proxy.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.llm.proxy.domain.po.LlmChannelPO;

import java.util.Collection;
import java.util.List;

/**
 * 中文说明：{@code LlmChannelDAO} 是数据面进程自有的 {@code gateway_llm_channel} 映射器，只继承
 * {@link EgonColaMapper} 已声明的受守卫语句（{@code selectActiveById}/{@code selectActiveByIds}/
 * {@code deleteVersionedById}），不重复声明泛型 {@code T}，也不新增任何写语句：本进程账号只有该表的 SELECT 授权，
 * 渠道写入与 DDL 全部归控制面。这里只补 Spec §11.2 引擎访问路径所需的具名类型化查询，即按 {@code routes[]} 声明的
 * 渠道 key 批量点查（batch ≤ 64）。
 * English summary: {@code LlmChannelDAO} is the data-plane process' own mapper for {@code gateway_llm_channel}. It only
 * inherits the guarded statements already declared by {@link EgonColaMapper} ({@code selectActiveById},
 * {@code selectActiveByIds}, {@code deleteVersionedById}), never redeclares the generic {@code T} and adds no statement
 * that writes: this process' database role holds SELECT grants on the table alone, while channel writes and DDL belong
 * to the control plane. It declares just the additional typed named query the engine needs for the Spec §11.2 access
 * path, namely the bounded batch lookup by the channel keys {@code routes[]} declares (batch ≤ 64).
 *
 * 用法 / Usage: 由 {@code @MapperScan} 注册，仅经 {@code MpLlmConfigurationRepository} 的受守卫只读边界访问，业务代码
 * 不得直接注入本接口；语句集合见 {@code mybatis/mapper/llm/LlmChannelDAO.xml}。
 * / Registered through {@code @MapperScan} and reached only through the guarded read-only boundary of
 * {@code MpLlmConfigurationRepository}; business code must not inject this interface directly. Its statements live in
 * {@code mybatis/mapper/llm/LlmChannelDAO.xml}.
 */
public interface LlmChannelDAO extends EgonColaMapper<LlmChannelPO> {

    /**
     * 中文说明：按渠道 key 集合批量读取活跃行，对应 {@code pk_gateway_llm_channel} 的等值 IN 访问路径；集合必须由
     * 调用方限定为非空且有界（≤64），空集合不得退化为无界全表扫描。
     * English summary: Batch-reads active rows by channel keys along the equality-IN path of
     * {@code pk_gateway_llm_channel}; the caller must keep the collection non-empty and bounded (≤ 64) so that an empty
     * one can never degrade into an unbounded scan.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmChannelDAO.selectActiveByChannelKeys(channelKeys)}。
     * @param channelKeys 参数 非空且有界的渠道 key 集合；parameter the non-empty, bounded channel key collection.
     * @return 返回 命中的活跃渠道行，未命中的 key 不出现在结果中；returns the matched active rows, with unmatched keys simply absent.
     */
    List<LlmChannelPO> selectActiveByChannelKeys(@Param("channelKeys") Collection<String> channelKeys);
}
