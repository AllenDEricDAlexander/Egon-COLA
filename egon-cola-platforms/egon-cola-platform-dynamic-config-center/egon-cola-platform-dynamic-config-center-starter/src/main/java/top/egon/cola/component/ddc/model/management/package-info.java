/**
 * DDC 管理查询、写入、发布任务与运维视图模型，构成 Starter 和 Admin 的共享管理契约。
 * 本包不依赖 JPA、Redisson 或 Admin 实体；{@code api.client} 使用这些模型定义端口。
 *
 * <p>Management query, write, publication-task, and operational-view models shared by Starter and
 * Admin. JPA, Redisson, and Admin entities are forbidden; {@code api.client} uses these models in
 * management ports.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.model.management;

import org.springframework.lang.NonNullApi;
