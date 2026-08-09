/**
 * Spring Boot ConfigData 阶段使用的 DDC RPC 装载能力。
 * 这里不得依赖普通运行期 Bean 生命周期完成首轮远程配置加载。
 * / DDC RPC loading support for the Spring Boot ConfigData phase. Initial
 * remote configuration loading must not depend on the ordinary runtime bean
 * lifecycle.
 */
@org.springframework.lang.NonNullApi
package top.egon.cola.component.rpc.ddc.configdata;
