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

public final class GatewayReportHttpClient {

    public static final String REPORT_PATH =
            "/api/v1/gateway/openapi/interface-definitions/reports";

    private final GatewayReportingProperties properties;

    private final RestClient client;

    private final DdcRequestSigner signer = new DdcRequestSigner();

    private final Clock clock;

    public GatewayReportHttpClient(
            GatewayReportingProperties properties) {
        this(properties, restClient(properties), Clock.systemUTC());
    }

    GatewayReportHttpClient(
            GatewayReportingProperties properties,
            RestClient client,
            Clock clock) {
        this.properties = properties;
        this.client = client;
        this.clock = clock;
    }

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

    public static final class GatewayReportTransportException
            extends RuntimeException {

        private final boolean retryable;

        GatewayReportTransportException(
                String message,
                boolean retryable,
                Throwable cause) {
            super(message, cause);
            this.retryable = retryable;
        }

        public boolean retryable() {
            return retryable;
        }
    }
}
