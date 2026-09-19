package top.egon.cola.component.common.cache.model;

/**
 * 空值缓存哨兵（防穿透）：唯一进入 L2 值域的非业务类型；
 * 读取命中哨兵由缓存层翻译为对外语义 null。无字段 record 天然按值相等。
 */
public record EgonColaCacheNullValueBO() {

    public static final EgonColaCacheNullValueBO INSTANCE = new EgonColaCacheNullValueBO();
}
