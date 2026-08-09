/**
 * DDC Redisson 客户端创建、Key 规则和通用 Topic 订阅资源句柄。
 * 本包不判断配置或注册业务事件；共享常量只能表达 Redis 命名协议，监听器通过本包访问基础设施。
 *
 * <p>Redisson client creation, key conventions, and generic Topic subscription resources for DDC.
 * Business event interpretation is excluded; shared constants may express only Redis naming
 * protocol, and listeners access infrastructure through this package.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.redis;

import org.springframework.lang.NonNullApi;
