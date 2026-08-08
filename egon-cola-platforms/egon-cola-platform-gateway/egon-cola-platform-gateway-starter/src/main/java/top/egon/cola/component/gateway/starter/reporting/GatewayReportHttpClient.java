package top.egon.cola.component.gateway.starter.reporting;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.model.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.model.security.DdcRequestSigner;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;

/**
 * Sends signed Gateway definition reports to Gateway Admin and retrieves their
 * acknowledgement receipts.
 *
 * <p>中文：向 Gateway Admin 发送带签名的接口定义报告，并查询对应的
 * 确认回执。
 */
public final class GatewayReportHttpClient {

    /**
     * Relative Gateway Admin path for definition report operations.
     * 接口定义报告操作的 Gateway Admin 相对路径。
     */
    public static final String REPORT_PATH =
            "/api/v1/gateway/openapi/interface-definitions/reports";

    /** Reporting and request-signing configuration. 上报及请求签名配置。 */
    private final GatewayReportingProperties properties;

    /**
     * HTTP client configured for the Gateway Admin base URL.
     * 按 Gateway Admin 基础 URL 配置的 HTTP 客户端。
     */
    private final RestClient client;

    /**
     * Signer for DDC-compatible authenticated Admin requests.
     * 兼容 DDC 认证请求的签名器。
     */
    private final DdcRequestSigner signer = new DdcRequestSigner();

    /** Clock used to produce request-signing timestamps. 生成请求签名时间戳的时钟。 */
    private final Clock clock;

    /**
     * Creates a reporting client using configured HTTP timeouts and the UTC
     * system clock.
     * 中文：使用配置的 HTTP 超时和 UTC 系统时钟创建上报客户端。
     *
     * @param properties reporting and request-signing configuration
     */
    public GatewayReportHttpClient(
            GatewayReportingProperties properties) {
        this(properties, restClient(properties), Clock.systemUTC());
    }

    /**
     * Creates a reporting client with injectable transport and time source.
     * 中文：使用可注入的传输客户端和时间源创建上报客户端，便于测试。
     *
     * @param properties reporting and request-signing configuration
     * @param client HTTP transport
     * @param clock request-signing clock
     */
    GatewayReportHttpClient(
            GatewayReportingProperties properties,
            RestClient client,
            Clock clock) {
        this.properties = properties;
        this.client = client;
        this.clock = clock;
    }

    /**
     * Submits a signed definition report and returns the Admin receipt.
     * 中文：提交带签名的接口定义报告并返回 Admin 回执。
     *
     * @param report serialized report and identity to submit
     * @return acknowledgement receipt, possibly {@code null} for an empty body
     * @throws GatewayReportTransportException if the request fails
     */
    public GatewayInterfaceDefinitionReportResult submit(
            GatewayDefinitionReportFactory.BuiltReport report) {
        long timestamp = clock.millis();
        String nonce = UuidV7.simpleString();
        DdcCanonicalRequest canonical = new DdcCanonicalRequest(
                "POST",
                REPORT_PATH,
                Map.of(),
                timestamp,
                nonce,
                report.payload()
        );
        try {
            return client.post()
                    .uri(REPORT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                            DdcRequestSigner.ACCESS_KEY_HEADER,
                            properties.getAccessKey()
                    )
                    .header(
                            DdcRequestSigner.TIMESTAMP_HEADER,
                            Long.toString(timestamp)
                    )
                    .header(DdcRequestSigner.NONCE_HEADER, nonce)
                    .header(
                            DdcRequestSigner.CONTENT_SHA256_HEADER,
                            canonical.contentSha256()
                    )
                    .header(
                            DdcRequestSigner.SIGNATURE_HEADER,
                            signer.sign(
                                    canonical,
                                    properties.getSecretKey()
                            )
                    )
                    .header("X-Gateway-Contract-Version", "v2")
                    .header(
                            "X-Gateway-Application-Code",
                            properties.getApplicationCode()
                    )
                    .header(
                            "X-Gateway-Report-Id",
                            report.report().reportId()
                    )
                    .body(report.payload())
                    .retrieve()
                    .body(GatewayInterfaceDefinitionReportResult.class);
        } catch (RestClientResponseException failure) {
            boolean retryable = failure.getStatusCode().value() == 429
                    || failure.getStatusCode().is5xxServerError();
            throw new GatewayReportTransportException(
                    "gateway report failed with HTTP "
                            + failure.getStatusCode().value(),
                    retryable,
                    failure
            );
        } catch (RuntimeException failure) {
            throw new GatewayReportTransportException(
                    "gateway report transport failed",
                    true,
                    failure
            );
        }
    }

    /**
     * Retrieves an existing report acknowledgement by report identifier.
     * 中文：按报告标识查询已有的上报确认回执。
     *
     * @param reportId report identifier accepted by Gateway Admin
     * @return acknowledgement receipt, or empty when Admin returns HTTP 404 or
     *         an empty body
     * @throws IllegalArgumentException if {@code reportId} is invalid
     * @throws GatewayReportTransportException if the request fails
     */
    public Optional<GatewayInterfaceDefinitionReportResult> find(
            String reportId) {
        if (reportId == null
                || !reportId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException(
                    "gateway reportId is invalid"
            );
        }
        String path = REPORT_PATH + "/" + reportId;
        long timestamp = clock.millis();
        String nonce = UuidV7.simpleString();
        DdcCanonicalRequest canonical = new DdcCanonicalRequest(
                "GET",
                path,
                Map.of(),
                timestamp,
                nonce,
                new byte[0]
        );
        try {
            return Optional.ofNullable(client.get()
                    .uri(path)
                    .header(
                            DdcRequestSigner.ACCESS_KEY_HEADER,
                            properties.getAccessKey()
                    )
                    .header(
                            DdcRequestSigner.TIMESTAMP_HEADER,
                            Long.toString(timestamp)
                    )
                    .header(DdcRequestSigner.NONCE_HEADER, nonce)
                    .header(
                            DdcRequestSigner.CONTENT_SHA256_HEADER,
                            canonical.contentSha256()
                    )
                    .header(
                            DdcRequestSigner.SIGNATURE_HEADER,
                            signer.sign(
                                    canonical,
                                    properties.getSecretKey()
                            )
                    )
                    .header(
                            "X-Gateway-Application-Code",
                            properties.getApplicationCode()
                    )
                    .retrieve()
                    .body(GatewayInterfaceDefinitionReportResult.class));
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw transportFailure(failure);
        } catch (RuntimeException failure) {
            throw new GatewayReportTransportException(
                    "gateway report receipt transport failed",
                    true,
                    failure
            );
        }
    }

    /**
     * Converts an Admin HTTP failure into a retry-aware transport exception.
     * 中文：将 Admin HTTP 失败转换为携带可重试信息的传输异常。
     *
     * @param failure response exception returned by the HTTP client
     * @return reporting transport exception
     */
    private GatewayReportTransportException transportFailure(
            RestClientResponseException failure) {
        boolean retryable = failure.getStatusCode().value() == 429
                || failure.getStatusCode().is5xxServerError();
        return new GatewayReportTransportException(
                "gateway report failed with HTTP "
                        + failure.getStatusCode().value(),
                retryable,
                failure
        );
    }

    /**
     * Builds the default non-redirecting HTTP client from reporting settings.
     * 中文：根据上报配置创建默认的不跟随重定向 HTTP 客户端。
     *
     * @param properties reporting transport configuration
     * @return configured REST client
     */
    private static RestClient restClient(
            GatewayReportingProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder()
                .baseUrl(properties.getAdminBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Reports a transport or Admin response failure together with whether the
     * coordinator may retry it.
     * 中文：封装传输或 Admin 响应失败，并标记协调器是否可以重试。
     */
    public static final class GatewayReportTransportException
            extends RuntimeException {

        /** Whether retrying the failed request is permitted. 是否允许重试失败请求。 */
        private final boolean retryable;

        /**
         * Creates a retry-aware reporting transport exception.
         * 中文：创建携带重试标记的上报传输异常。
         *
         * @param message failure description
         * @param retryable whether the request may be retried
         * @param cause underlying failure, or {@code null} when unavailable
         */
        GatewayReportTransportException(
                String message,
                boolean retryable,
                Throwable cause) {
            super(message, cause);
            this.retryable = retryable;
        }

        /**
         * Returns whether the coordinator may retry the failed request.
         * 中文：返回协调器是否可以重试失败请求。
         *
         * @return {@code true} when retry is permitted
         */
        public boolean retryable() {
            return retryable;
        }
    }
}
