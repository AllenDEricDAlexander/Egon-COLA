package top.egon.cola.component.ruleengine.result;

import top.egon.cola.component.ruleengine.trace.RuleTrace;

import java.io.Serial;
import java.io.Serializable;

public class RuleResult<R> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean success;

    private final RuleStatus status;

    private final int code;

    private final String message;

    private final R data;

    private final RuleTrace trace;

    private final Throwable exception;

    private final String stoppedNode;

    private final String hitNode;

    private final long costMillis;

    private RuleResult(boolean success, RuleStatus status, int code, String message, R data,
                       RuleTrace trace, Throwable exception, String stoppedNode, String hitNode,
                       long costMillis) {
        this.success = success;
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.trace = trace;
        this.exception = exception;
        this.stoppedNode = stoppedNode;
        this.hitNode = hitNode;
        this.costMillis = costMillis;
    }

    public static <R> RuleResult<R> success(R data) {
        return new RuleResult<>(true, RuleStatus.SUCCESS, RuleStatus.SUCCESS.getCode(),
                RuleStatus.SUCCESS.getMessage(), data, null, null, null, null, 0L);
    }

    public static <R> RuleResult<R> stop(int code, String message, R data) {
        return new RuleResult<>(false, RuleStatus.STOPPED, code, message, data, null, null, null, null, 0L);
    }

    public static <R> RuleResult<R> fail(int code, String message, Throwable exception) {
        return new RuleResult<>(false, RuleStatus.FAILED, code, message, null, null, exception, null, null, 0L);
    }

    public static <R> RuleResult<R> failure(RuleStatus status, String message, Throwable exception) {
        return new RuleResult<>(false, status, status.getCode(), message, null, null, exception, null, null, 0L);
    }

    public static <R> RuleResult<R> timeout(String message) {
        return failure(RuleStatus.TIMEOUT, message, null);
    }

    public static <R> RuleResult<R> maxStepsExceeded(String message) {
        return failure(RuleStatus.MAX_STEPS_EXCEEDED, message, null);
    }

    public static <R> RuleResult<R> noRoute(String message) {
        return failure(RuleStatus.NO_ROUTE, message, null);
    }

    public RuleResult<R> withTrace(RuleTrace trace) {
        return new RuleResult<>(success, status, code, message, data, trace, exception, stoppedNode, hitNode, costMillis);
    }

    public RuleResult<R> withStoppedNode(String stoppedNode) {
        return new RuleResult<>(success, status, code, message, data, trace, exception, stoppedNode, hitNode, costMillis);
    }

    public RuleResult<R> withHitNode(String hitNode) {
        return new RuleResult<>(success, status, code, message, data, trace, exception, stoppedNode, hitNode, costMillis);
    }

    public RuleResult<R> withCostMillis(long costMillis) {
        return new RuleResult<>(success, status, code, message, data, trace, exception, stoppedNode, hitNode, costMillis);
    }

    public boolean isSuccess() {
        return success;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public R getData() {
        return data;
    }

    public RuleTrace getTrace() {
        return trace;
    }

    public Throwable getException() {
        return exception;
    }

    public String getStoppedNode() {
        return stoppedNode;
    }

    public String getHitNode() {
        return hitNode;
    }

    public long getCostMillis() {
        return costMillis;
    }
}
