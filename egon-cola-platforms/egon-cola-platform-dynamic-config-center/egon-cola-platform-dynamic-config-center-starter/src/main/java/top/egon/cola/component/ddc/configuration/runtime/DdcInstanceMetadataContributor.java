package top.egon.cola.component.ddc.configuration.runtime;

import java.util.Map;

/**
 * 向 DDC 实例注册请求贡献自定义元数据的扩展接口。
 * Extension point that contributes custom metadata to DDC instance registration requests.
 */
@FunctionalInterface
public interface DdcInstanceMetadataContributor {

    /**
     * 返回需要合并到实例注册信息中的元数据。
     * Returns metadata to merge into the instance registration information.
     *
     * @return 元数据映射; metadata map
     */
    Map<String, String> metadata();
}
