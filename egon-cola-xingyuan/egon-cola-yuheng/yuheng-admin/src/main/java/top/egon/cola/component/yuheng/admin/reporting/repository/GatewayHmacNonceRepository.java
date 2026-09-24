package top.egon.cola.component.yuheng.admin.reporting.repository;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

/**
 * 中文说明：{@code GatewayHmacNonceRepository} 是上报 HMAC 防重放的一次性随机数（nonce）存储端口：只声明调用方真正使用的
 * 「登记 nonce」与「清理过期 nonce」两个契约，签名里不出现 MyBatis-Plus 行模型、JPA 派生查询名或裸 SQL 概念，
 * 具体的租户过滤、活跃读取与写入守卫由 {@code repository/impl} 下的门面实现经各表的受守卫持久化边界承担。
 * English summary: {@code GatewayHmacNonceRepository} is the gateway report HMAC replay-protection nonce store port: it declares
 * only the two contracts callers actually use — claiming a nonce and sweeping expired ones — and keeps MyBatis-Plus row models,
 * Spring Data derived-query names and raw-SQL concepts out of its signatures, while the facade implementation under
 * {@code repository/impl} delegates tenant filtering, active reads and write guards to the guarded persistence boundary.
 *
 * 用法 / Usage: 由 {@code GatewayReportHmacFilter} 与 {@code GatewayHmacNonceReaper} 通过本端口注入，禁止依赖具体实现类型；
 * 校验注解只界定入参形状，不改变既有幂等/重放语义。/ Inject it through this port in {@code GatewayReportHmacFilter} and
 * {@code GatewayHmacNonceReaper} and never depend on the implementation type; the constraints only shape arguments and change no
 * idempotency or replay semantics.
 */
@Validated
public interface GatewayHmacNonceRepository {

    /**
     * 中文说明：执行 claim 操作；在 (accessKey, nonce) 上登记一条有效期至 {@code expiresAt} 的一次性随机数，
     * 首次登记返回 {@code true}，命中既有唯一键（重放）返回 {@code false}，与原 {@code INSERT} + 唯一键冲突语义一致，
     * 不抛出额外异常类型。
     * English summary: Executes the claim operation; registers one nonce bound to {@code (accessKey, nonce)} that stays effective until
     * {@code expiresAt}. The first registration returns {@code true} while a unique-key hit (a replay) returns {@code false}, exactly as
     * the original {@code INSERT} plus unique-key conflict behaved, and no extra exception type is introduced.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHmacNonceRepository.claim(accessKey, nonce, expiresAt, now)}。
     * 入参 {@code now} 保留为调用方的参考时间：技术创建时间已由持久边界的审计填充统一写入，因此该值不再落到任何列。
     * @param accessKey 参数 访问键；parameter access key。
     * @param nonce 参数 nonce；parameter nonce。
     * @param expiresAt 参数 过期时间，写入业务列 {@code expires_at}；parameter expiry written to the business column {@code expires_at}。
     * @param now 参数 调用方参考时间；parameter caller's reference instant.
     * @return 返回首次登记是否为真（重放为假）；returns whether the nonce was claimed freshly, false on a replay.
     */
    boolean claim(
            @NotBlank String accessKey,
            @NotBlank String nonce,
            @NotNull Instant expiresAt,
            @NotNull Instant now);

    /**
     * 中文说明：执行 deleteExpired 操作；清理 {@code expires_at} 严格早于 {@code now} 的 nonce，返回真正被清理的行数，
     * 与原 {@code DELETE ... WHERE expires_at < ?} 的受影响行数一致：没有命中就是 0，绝不伪造成功。
     * English summary: Executes the deleteExpired operation; sweeps every nonce whose {@code expires_at} is strictly earlier than
     * {@code now} and returns the number of rows actually swept, matching the affected-row count of the original
     * {@code DELETE ... WHERE expires_at < ?}: zero when nothing qualifies and never a fabricated success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHmacNonceRepository.deleteExpired(now)}。
     * @param now 参数 清理基准时间；parameter sweep cut-off instant。
     * @return 返回被清理行数；returns the number of swept rows.
     */
    int deleteExpired(@NotNull Instant now);
}
