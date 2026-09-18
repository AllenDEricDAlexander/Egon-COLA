package top.egon.cola.component.common.core.cache;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 缓存能力端口 SPI：由两级缓存 starter 提供实现，供仓储等下游组件以零缓存依赖方式集成。
 *
 * <p>键形状约定为 {@code tenantId:id}；glob 模式仅允许 {@code tenantId:*} 形态，
 * 具体校验由实现方守卫完成，端口层不抛受检异常、不做参数校验。
 *
 * <p>实现要求与语义细节以两级缓存主 Spec §10 为准。
 */
public interface EgonColaCachePort {

    /**
     * 读穿缓存取值：命中则返回缓存值，未命中调用 {@code loader} 回源并回填。
     *
     * @param cacheName 缓存区域名
     * @param key       完整缓存键（{@code tenantId:id}）
     * @param loader    回源加载器，仅在缓存缺失时调用
     * @return 缓存值或回源值，可能为 null
     */
    Object get(String cacheName, String key, Supplier<Object> loader);

    /**
     * 批量读穿取值：逐键命中/回源，返回列表与入参 {@code keys} 等长，
     * 某 id 无值时对应位置为 null。
     *
     * @param cacheName 缓存区域名
     * @param keys      完整缓存键列表（均为 {@code tenantId:id}）
     * @param loader    以单个 key 为入参的回源加载器
     * @return 与 keys 等长的值列表，缺失位置为 null
     */
    List<Object> getAll(String cacheName, List<String> keys, Function<String, Object> loader);

    /**
     * 注册事务提交后的失效动作：若当前存在激活事务，失效在 afterCommit 阶段执行
     * （精确键与 glob 模式一并广播失效）；事务回滚则丢弃不执行；
     * 无激活事务时立即执行。
     *
     * @param cacheName    缓存区域名
     * @param exactKeys    待失效精确键集合，可为空
     * @param globPatterns 待失效 glob 模式集合（仅 {@code tenantId:*} 形态），可为空
     */
    void registerEvictionAfterCommit(String cacheName, Collection<String> exactKeys,
                                     Collection<String> globPatterns);
}
