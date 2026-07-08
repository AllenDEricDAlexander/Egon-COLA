package top.egon.cola.component.ruleengine.context;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RuleContext {

    private final String requestId;

    private final String traceId;

    private final Instant startTime;

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final List<String> executionPath = new CopyOnWriteArrayList<>();

    private final List<Throwable> errors = new CopyOnWriteArrayList<>();

    private final AtomicInteger stepCount = new AtomicInteger();

    private volatile boolean proceed = true;

    private volatile boolean stopped;

    private volatile String currentNode;

    private volatile String previousNode;

    private volatile int maxSteps = 100;

    private volatile Instant deadline;

    private RuleContext(String requestId, String traceId) {
        this.requestId = normalize(requestId, "req-");
        this.traceId = normalize(traceId, "trace-");
        this.startTime = Instant.now();
    }

    public static RuleContext create() {
        return new RuleContext(null, null);
    }

    public static RuleContext create(String requestId, String traceId) {
        return new RuleContext(requestId, traceId);
    }

    public RuleContext maxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
        return this;
    }

    public RuleContext timeout(Duration timeout) {
        this.deadline = timeout == null ? null : Instant.now().plus(timeout);
        return this;
    }

    public RuleContext set(String key, Object value) {
        if (value == null) {
            attributes.remove(key);
            return this;
        }
        attributes.put(key, value);
        return this;
    }

    public Object get(String key) {
        return attributes.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = attributes.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public boolean contains(String key) {
        return attributes.containsKey(key);
    }

    public Object remove(String key) {
        return attributes.remove(key);
    }

    public void proceed() {
        this.proceed = true;
        this.stopped = false;
    }

    public void stop() {
        this.proceed = false;
        this.stopped = true;
    }

    public int incrementStep() {
        return stepCount.incrementAndGet();
    }

    public boolean isExceededMaxSteps() {
        return stepCount.get() > maxSteps;
    }

    public boolean isTimeout() {
        return deadline != null && !Instant.now().isBefore(deadline);
    }

    public void enterNode(String nodeCode) {
        this.previousNode = currentNode;
        this.currentNode = nodeCode;
        this.executionPath.add(nodeCode);
    }

    public void addError(Throwable throwable) {
        if (throwable != null) {
            errors.add(throwable);
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public boolean isProceed() {
        return proceed;
    }

    public boolean isStopped() {
        return stopped;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public String getPreviousNode() {
        return previousNode;
    }

    public List<String> getExecutionPath() {
        return Collections.unmodifiableList(new ArrayList<>(executionPath));
    }

    public int getStepCount() {
        return stepCount.get();
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public List<Throwable> getErrors() {
        return Collections.unmodifiableList(new ArrayList<>(errors));
    }

    private static String normalize(String value, String prefix) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return prefix + UUID.randomUUID();
    }
}
