package top.egon.cola.component.accessguard.context;

import java.util.LinkedHashMap;
import java.util.Map;

public class AccessGuardContext {

    private String ruleName;

    private String methodSignature;

    private String accessKey;

    private String accessKeyHash;

    private long startNanos = System.nanoTime();

    private Map<String, Object> attributes = new LinkedHashMap<>();

    private AccessGuardResult result;

    public String ruleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String methodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String accessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String accessKeyHash() {
        return accessKeyHash;
    }

    public void setAccessKeyHash(String accessKeyHash) {
        this.accessKeyHash = accessKeyHash;
    }

    public long startNanos() {
        return startNanos;
    }

    public void setStartNanos(long startNanos) {
        this.startNanos = startNanos;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public AccessGuardResult result() {
        return result;
    }

    public void setResult(AccessGuardResult result) {
        this.result = result;
    }
}
