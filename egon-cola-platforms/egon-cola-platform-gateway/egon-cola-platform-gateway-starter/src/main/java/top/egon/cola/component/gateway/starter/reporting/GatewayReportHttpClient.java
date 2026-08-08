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
 */
public final class GatewayReportHttpClient {

    /** Relative Gateway Admin path for definition report operations. */
    public static final String REPORT_PATH =
            "/api/v1/gateway/openapi/interface-definitions/reports";

    /** Reporting and request-signing configuration. */
    private final GatewayReportingProperties properties;

    /** HTTP client configured for the Gateway Admin base URL. */
    private final RestClient client;

    /** Signer for DDC-compatible authenticated Admin requests. */
    private final DdcRequestSigner signer = new DdcRequestSigner();

    /** Clock used to produce request-signing timestamps. */
    private final Clock clock;

    /**
     * Creates a reporting client using configured HTTP timeouts and the UTC
     * system clock.
     *
     * @param properties reporting and request-signing configuration
     */
    public GatewayReportHttpClient(
            GatewayReportingProperties properties) {
        this(properties, restClient(properties), Clock.systemUTC());
    }

    /**
     * Creates a reporting client with injectable transport and time source.
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
     */
    public static final class GatewayReportTransportException
            extends RuntimeException {

        /** Whether retrying the failed request is permitted. */
        private final boolean retryable;

        /**
         * Creates a retry-aware reporting transport exception.
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
         *
         * @return {@code true} when retry is permitted
         */
        public boolean retryable() {
            return retryable;
        }
    }
}
