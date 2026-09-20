package top.egon.cola.component.dtp.domain.model.valobj;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * @author 有罗敷的马同学
 * @description 注册中心枚举值对象
 * @Date 上午8:55 2025/4/13
 **/
public enum RegistryEnumVO implements EgonEnum {

    THREAD_POOL_CONFIG_LIST_KEY(0, "THREAD_POOL_CONFIG_LIST_KEY", "池化配置列表"),
    THREAD_POOL_CONFIG_PARAMETER_LIST_KEY(1, "THREAD_POOL_CONFIG_PARAMETER_LIST_KEY", "池化配置参数"),
    DYNAMIC_THREAD_POOL_REDIS_TOPIC(2, "DYNAMIC_THREAD_POOL_REDIS_TOPIC", "动态线程池监听主题配置");

    private final int code;

    private final String key;

    private final String desc;

    RegistryEnumVO(int code, String key, String desc) {
        this.code = code;
        this.key = key;
        this.desc = desc;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return desc;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }


}
