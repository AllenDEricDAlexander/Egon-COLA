package top.egon.cola.platform.idp.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.security.CredentialExtractionResult;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 从 Gateway 请求中提取唯一的 Authorization Bearer 凭据。
 * 本提取器从不读取查询参数中的凭据，并在无凭据、成功或失败三种结果中始终声明应清理的保留头，
 * 防止外部请求伪造下游可信身份。
 *
 * <p>Extracts exactly one Authorization Bearer credential from a Gateway request. It never reads
 * credentials from query data and always declares the reserved fields to remove for missing,
 * successful, and invalid credential results, preventing callers from spoofing downstream trusted
 * identity.</p>
 */
public final class IdpBearerCredentialExtractor
        implements GatewayCredentialExtractor {

    /**
     * Gateway 配置引用本提取器时使用的稳定标识。
     *
     * <p>Stable identifier used by Gateway configuration to select this extractor.</p>
     */
    public static final String EXTRACTOR_ID = "idp-bearer";

    /**
     * 接受的 Bearer 令牌最大字符数。
     *
     * <p>Maximum accepted Bearer token length.</p>
     */
    private static final int MAX_TOKEN_LENGTH = 8192;

    /**
     * 提供请求字段清理集合的保留头规则。
     *
     * <p>Reserved-header rules supplying the request fields to remove.</p>
     */
    private final IdpReservedHeaderSanitizer sanitizer;

    /**
     * 创建 IdP Bearer 凭据提取器。
     *
     * <p>Creates the IdP Bearer credential extractor.</p>
     *
     * @param sanitizer 保留身份头清理器；reserved identity-header sanitizer
     */
    public IdpBearerCredentialExtractor(IdpReservedHeaderSanitizer sanitizer) {
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
    }

    /**
     * 返回提取器稳定标识。
     *
     * <p>Returns the stable extractor identifier.</p>
     *
     * @return {@value #EXTRACTOR_ID}
     */
    @Override
    public String extractorId() {
        return EXTRACTOR_ID;
    }

    /**
     * 返回本提取器产生的凭据类型。
     *
     * <p>Returns the credential type produced by this extractor.</p>
     *
     * @return {@code bearer}
     */
    @Override
    public String credentialType() {
        return "bearer";
    }

    /**
     * 提取并严格校验唯一的 Bearer 认证头。
     * 没有认证头表示匿名输入而非格式错误；重复头、非法方案、空白/控制字符、逗号或超长令牌均被拒绝。
     *
     * <p>Extracts and strictly validates the single Bearer authorization header. No header denotes
     * anonymous input rather than malformed input; duplicate headers, an invalid scheme,
     * whitespace or control characters, commas, and oversized tokens are rejected.</p>
     *
     * @param exchange 当前 Gateway 交换对象；current Gateway exchange
     * @param policy 当前安全策略；current security policy
     * @return 异步凭据提取结果；asynchronous credential-extraction result
     */
    @Override
    public Publisher<CredentialExtractionResult> extract(
            GatewayExchange exchange,
            GatewaySecurityPolicy policy
    ) {
        Objects.requireNonNull(exchange, "exchange");
        List<String> values = new ArrayList<>();
        exchange.request().headers().names().stream()
                .filter(name -> "authorization".equalsIgnoreCase(name))
                .forEach(name -> values.addAll(
                        exchange.request().headers().values(name)));
        if (values.isEmpty()) {
            return Mono.just(new CredentialExtractionResult(
                    List.of(), sanitizer.fieldsToRemove(), null));
        }
        if (values.size() != 1) {
            return Mono.just(invalid());
        }
        String value = values.getFirst();
        if (value == null || value.length() < "Bearer ".length()
                || !value.regionMatches(
                        true, 0, "Bearer ", 0, "Bearer ".length())) {
            return Mono.just(invalid());
        }
        String token = value.substring("Bearer ".length());
        if (token.isEmpty() || token.length() > MAX_TOKEN_LENGTH
                || token.chars().anyMatch(character ->
                Character.isWhitespace(character)
                        || Character.isISOControl(character)
                        || character == ',')) {
            return Mono.just(invalid());
        }
        return Mono.just(new CredentialExtractionResult(
                List.of(new GatewayCredential("bearer", token, Map.of())),
                sanitizer.fieldsToRemove(),
                null));
    }

    /**
     * 创建统一的非法凭据结果，同时保留请求头清理要求。
     *
     * <p>Creates the uniform invalid-credential result while preserving request-header removal
     * requirements.</p>
     *
     * @return 非法凭据结果；invalid-credential result
     */
    private CredentialExtractionResult invalid() {
        return new CredentialExtractionResult(
                List.of(),
                sanitizer.fieldsToRemove(),
                "GATEWAY_CREDENTIAL_INVALID");
    }
}
