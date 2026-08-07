package top.egon.cola.component.common.trace.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = TraceProperties.PREFIX, ignoreInvalidFields = true)
@Getter
@Setter
public class TraceProperties {

    public static final String PREFIX = "egon.cola.component.trace";

    private boolean enabled = true;

    private Propagation propagation = new Propagation();

    private Servlet servlet = new Servlet();

    private WebFlux webflux = new WebFlux();

    private RestClient restClient = new RestClient();

    private WebClient webClient = new WebClient();

    private Reactor reactor = new Reactor();

    @Getter
    @Setter
    public static class Propagation {

        private boolean enabled = true;

        private boolean legacyTraceIdReadOnly = true;

        private boolean responseHeaders = true;
    }

    @Getter
    @Setter
    public static class Servlet {

        private boolean enabled = true;

        private int order = Integer.MIN_VALUE + 100;

        private boolean responseHeaders = true;

        private boolean accessLog = true;

        private List<String> excludedPaths = new ArrayList<>();

        private Duration slowRequestThreshold = Duration.ofSeconds(1);

        private boolean recordQuery = false;

        private boolean recordHeaders = false;

        private boolean recordRequestBody = false;

        private boolean recordResponseBody = false;

        private boolean trustedProxyHeaders = false;
    }

    @Getter
    @Setter
    public static class WebFlux extends Servlet {
    }

    @Getter
    @Setter
    public static class RestClient {

        private boolean enabled = true;

        private boolean takeOverExistingTraceparent = false;
    }

    @Getter
    @Setter
    public static class WebClient extends RestClient {
    }

    @Getter
    @Setter
    public static class Reactor {

        private boolean automaticContextPropagation = true;
    }
}
