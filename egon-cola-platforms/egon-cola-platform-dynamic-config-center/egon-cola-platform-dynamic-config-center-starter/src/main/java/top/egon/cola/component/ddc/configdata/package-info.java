/**
 * DDC 的 Spring Boot ConfigData 定位、资源描述、远程获取和属性源加载 SPI。
 * 本包只负责启动期 `ddc:` 配置导入，依赖客户端与格式能力；运行期刷新和自动装配不属于本包。
 *
 * <p>Spring Boot ConfigData SPI for locating, describing, fetching, and loading DDC property
 * sources. It handles startup-time {@code ddc:} imports and depends on client and format support;
 * runtime refresh and automatic configuration are excluded.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.configdata;

import org.springframework.lang.NonNullApi;
