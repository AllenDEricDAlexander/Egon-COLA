package top.egon.cola.component.common.trace.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = TraceProperties.PREFIX, ignoreInvalidFields = true)
public class TraceProperties {

    public static final String PREFIX = "egon.cola.component.trace";

    private boolean enabled = true;

    private Propagation propagation = new Propagation();

    private Servlet servlet = new Servlet();

    private WebFlux webflux = new WebFlux();

    private RestClient restClient = new RestClient();

    private WebClient webClient = new WebClient();

    private Reactor reactor = new Reactor();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Propagation getPropagation() {
        return propagation;
    }

    public void setPropagation(Propagation propagation) {
        this.propagation = propagation;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }

    public WebFlux getWebflux() {
        return webflux;
    }

    public void setWebflux(WebFlux webflux) {
        this.webflux = webflux;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public WebClient getWebClient() {
        return webClient;
    }

    public void setWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Reactor getReactor() {
        return reactor;
    }

    public void setReactor(Reactor reactor) {
        this.reactor = reactor;
    }

    public static class Propagation {

        private boolean enabled = true;

        private boolean legacyTraceIdReadOnly = true;

        private boolean responseHeaders = true;

        private boolean sourceHeaders = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isLegacyTraceIdReadOnly() {
            return legacyTraceIdReadOnly;
        }

        public void setLegacyTraceIdReadOnly(boolean legacyTraceIdReadOnly) {
            this.legacyTraceIdReadOnly = legacyTraceIdReadOnly;
        }

        public boolean isResponseHeaders() {
            return responseHeaders;
        }

        public void setResponseHeaders(boolean responseHeaders) {
            this.responseHeaders = responseHeaders;
        }

        public boolean isSourceHeaders() {
            return sourceHeaders;
        }

        public void setSourceHeaders(boolean sourceHeaders) {
            this.sourceHeaders = sourceHeaders;
        }
    }

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public boolean isResponseHeaders() {
            return responseHeaders;
        }

        public void setResponseHeaders(boolean responseHeaders) {
            this.responseHeaders = responseHeaders;
        }

        public boolean isAccessLog() {
            return accessLog;
        }

        public void setAccessLog(boolean accessLog) {
            this.accessLog = accessLog;
        }

        public List<String> getExcludedPaths() {
            return excludedPaths;
        }

        public void setExcludedPaths(List<String> excludedPaths) {
            this.excludedPaths = excludedPaths;
        }

        public Duration getSlowRequestThreshold() {
            return slowRequestThreshold;
        }

        public void setSlowRequestThreshold(Duration slowRequestThreshold) {
            this.slowRequestThreshold = slowRequestThreshold;
        }

        public boolean isRecordQuery() {
            return recordQuery;
        }

        public void setRecordQuery(boolean recordQuery) {
            this.recordQuery = recordQuery;
        }

        public boolean isRecordHeaders() {
            return recordHeaders;
        }

        public void setRecordHeaders(boolean recordHeaders) {
            this.recordHeaders = recordHeaders;
        }

        public boolean isRecordRequestBody() {
            return recordRequestBody;
        }

        public void setRecordRequestBody(boolean recordRequestBody) {
            this.recordRequestBody = recordRequestBody;
        }

        public boolean isRecordResponseBody() {
            return recordResponseBody;
        }

        public void setRecordResponseBody(boolean recordResponseBody) {
            this.recordResponseBody = recordResponseBody;
        }

        public boolean isTrustedProxyHeaders() {
            return trustedProxyHeaders;
        }

        public void setTrustedProxyHeaders(boolean trustedProxyHeaders) {
            this.trustedProxyHeaders = trustedProxyHeaders;
        }
    }

    public static class WebFlux extends Servlet {
    }

    public static class RestClient {

        private boolean enabled = true;

        private boolean takeOverExistingTraceparent = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTakeOverExistingTraceparent() {
            return takeOverExistingTraceparent;
        }

        public void setTakeOverExistingTraceparent(boolean takeOverExistingTraceparent) {
            this.takeOverExistingTraceparent = takeOverExistingTraceparent;
        }
    }

    public static class WebClient extends RestClient {
    }

    public static class Reactor {

        private boolean automaticContextPropagation = true;

        public boolean isAutomaticContextPropagation() {
            return automaticContextPropagation;
        }

        public void setAutomaticContextPropagation(
                boolean automaticContextPropagation) {
            this.automaticContextPropagation = automaticContextPropagation;
        }
    }
}
