package top.egon.cola.component.ruleengine.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "egon.cola.component.rule-engine", ignoreInvalidFields = true)
public class RuleEngineProperties {

    private boolean enabled = true;

    private int defaultMaxSteps = 100;

    private long defaultTimeoutMillis = 3000L;

    private int asyncCorePoolSize = 4;

    private int asyncMaxPoolSize = 16;

    private boolean traceEnabled = true;

    private boolean listenerErrorIgnore = true;

    private boolean throwException = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultMaxSteps() {
        return defaultMaxSteps;
    }

    public void setDefaultMaxSteps(int defaultMaxSteps) {
        this.defaultMaxSteps = defaultMaxSteps;
    }

    public long getDefaultTimeoutMillis() {
        return defaultTimeoutMillis;
    }

    public void setDefaultTimeoutMillis(long defaultTimeoutMillis) {
        this.defaultTimeoutMillis = defaultTimeoutMillis;
    }

    public int getAsyncCorePoolSize() {
        return asyncCorePoolSize;
    }

    public void setAsyncCorePoolSize(int asyncCorePoolSize) {
        this.asyncCorePoolSize = asyncCorePoolSize;
    }

    public int getAsyncMaxPoolSize() {
        return asyncMaxPoolSize;
    }

    public void setAsyncMaxPoolSize(int asyncMaxPoolSize) {
        this.asyncMaxPoolSize = asyncMaxPoolSize;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public boolean isListenerErrorIgnore() {
        return listenerErrorIgnore;
    }

    public void setListenerErrorIgnore(boolean listenerErrorIgnore) {
        this.listenerErrorIgnore = listenerErrorIgnore;
    }

    public boolean isThrowException() {
        return throwException;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
}
