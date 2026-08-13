package top.egon.cola.platform.rbac3.starter.manifest;

import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;

/**
 * 类型 `Rbac3ManifestContributor` 位于当前包内，是接口，用于承载 `Rbac3 Manifest Contributor` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3ManifestContributor` is an interface in its package and carries the responsibility, state, or contract for `Rbac3 Manifest Contributor`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3ManifestContributor` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3ManifestContributor` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@FunctionalInterface
public interface Rbac3ManifestContributor {
    /**
     * 方法 `contribute` 按照 `Rbac3ManifestContributor` 的职责处理输入，完成 `contribute` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `contribute` processes its inputs according to `Rbac3ManifestContributor`'s responsibility, performs the `contribute` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `contribute` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `contribute`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    ResourceManifest contribute();
}
