package top.egon.cola.component.bytecode.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "egon.cola.component.bytecode")
public class BytecodeProperties {

    private boolean enabled = true;
    private final Executor executor = new Executor();
    private final Runtime runtime = new Runtime();
    private final Endpoint endpoint = new Endpoint();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public static class Executor {

        private boolean enabled = true;
        private boolean propagateMdc = true;
        private boolean metrics = true;
        private double samplingRate = 1.0;
        private List<String> include = new ArrayList<>();
        private List<String> exclude = new ArrayList<>();
        private Map<String, String> names = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isPropagateMdc() {
            return propagateMdc;
        }

        public void setPropagateMdc(boolean propagateMdc) {
            this.propagateMdc = propagateMdc;
        }

        public boolean isMetrics() {
            return metrics;
        }

        public void setMetrics(boolean metrics) {
            this.metrics = metrics;
        }

        public double getSamplingRate() {
            return samplingRate;
        }

        public void setSamplingRate(double samplingRate) {
            if (samplingRate < 0.0 || samplingRate > 1.0) {
                throw new IllegalArgumentException("executor.sampling-rate must be between 0 and 1");
            }
            this.samplingRate = samplingRate;
        }

        public List<String> getInclude() {
            return include;
        }

        public void setInclude(List<String> include) {
            this.include = include == null ? new ArrayList<>() : new ArrayList<>(include);
        }

        public List<String> getExclude() {
            return exclude;
        }

        public void setExclude(List<String> exclude) {
            this.exclude = exclude == null ? new ArrayList<>() : new ArrayList<>(exclude);
        }

        public Map<String, String> getNames() {
            return names;
        }

        public void setNames(Map<String, String> names) {
            this.names = names == null ? new LinkedHashMap<>() : new LinkedHashMap<>(names);
        }
    }

    public static class Runtime {

        private int failureCapacity = 32;

        public int getFailureCapacity() {
            return failureCapacity;
        }

        public void setFailureCapacity(int failureCapacity) {
            if (failureCapacity < 1 || failureCapacity > 1024) {
                throw new IllegalArgumentException(
                        "runtime.failure-capacity must be between 1 and 1024");
            }
            this.failureCapacity = failureCapacity;
        }
    }

    public static class Endpoint {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
