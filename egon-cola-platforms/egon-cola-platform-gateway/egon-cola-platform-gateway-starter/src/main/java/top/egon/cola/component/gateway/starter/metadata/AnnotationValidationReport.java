package top.egon.cola.component.gateway.starter.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Accumulates annotation problems so they can be reported together.
 *
 * <p>Failing on the first bad declaration makes fixing a misconfigured service a sequence of
 * build-run-fix cycles, one problem at a time. This collects every violation and reports them in
 * one message.
 *
 * <p>Errors are fatal; warnings describe a legal but suspicious combination — such as retries on
 * a non-idempotent method — that should be visible without blocking startup.
 *
 * <p>该报告会集中收集注解问题：错误会阻止启动，警告只提示可疑但合法的配置组合。
 */
public final class AnnotationValidationReport {

    /** Findings accumulated in insertion order. 按加入顺序累积的校验发现结果。 */
    private final List<Violation> violations = new ArrayList<>();

    /** Severity of a single annotation validation finding. 单条注解校验发现的严重级别。 */
    public enum Severity {
        /** A fatal declaration problem that prevents startup. 会阻止应用启动的致命声明问题。 */
        ERROR,

        /** A suspicious but legal declaration that does not prevent startup. 合法但可疑、不会阻止启动的声明。 */
        WARNING
    }

    /**
     * Describes one annotation validation finding.
     *
     * <p>描述一条注解校验发现，并记录其严重级别、声明位置、问题属性和修正建议。
     *
     * @param severity severity assigned to the finding，分配给发现结果的严重级别
     * @param location human-readable declaration site, e.g. {@code com.acme.OrderService#place}，可读的声明位置
     * @param field    the annotation attribute at fault，出问题的注解属性
     * @param message  what is wrong and what to do about it，问题及修正方式
     */
    public record Violation(Severity severity, String location, String field, String message) {

        /**
         * Validates and initializes a finding.
         *
         * <p>校验并初始化一条发现结果，所有组件都必须非空。
         *
         * @throws NullPointerException if any component is {@code null}
         */
        public Violation {
            Objects.requireNonNull(severity, "severity");
            location = Objects.requireNonNull(location, "location");
            field = Objects.requireNonNull(field, "field");
            message = Objects.requireNonNull(message, "message");
        }

        /**
         * Renders the finding as its severity, location, field and message.
         *
         * <p>将发现结果渲染为包含级别、位置、属性和消息的单行文本。
         *
         * @return the single-line finding description
         */
        @Override
        public String toString() {
            return severity + " " + location + " [" + field + "] " + message;
        }
    }

    /**
     * Adds a fatal annotation validation finding.
     *
     * <p>添加一条致命的注解校验发现，并返回当前报告以便继续收集。
     *
     * @param location human-readable declaration site
     * @param field annotation attribute at fault
     * @param message description of the problem and expected correction
     *
     * @return this report
     */
    public AnnotationValidationReport error(String location, String field, String message) {
        violations.add(new Violation(Severity.ERROR, location, field, message));
        return this;
    }

    /**
     * Adds a non-fatal annotation validation finding.
     *
     * <p>添加一条非致命的注解校验发现，并返回当前报告以便继续收集。
     *
     * @param location human-readable declaration site
     * @param field annotation attribute at fault
     * @param message description of the suspicious declaration
     *
     * @return this report
     */
    public AnnotationValidationReport warning(String location, String field, String message) {
        violations.add(new Violation(Severity.WARNING, location, field, message));
        return this;
    }

    /**
     * Records an error when {@code valid} is {@code false}.
     *
     * <p>当条件不满足时记录错误；返回传入的条件值。
     *
     * @param valid whether the declaration satisfies the requirement
     * @param location human-readable declaration site
     * @param field annotation attribute at fault
     * @param message description used when the requirement fails
     *
     * @return the supplied {@code valid} value
     */
    public boolean require(boolean valid, String location, String field, String message) {
        if (!valid) {
            error(location, field, message);
        }
        return valid;
    }

    /**
     * Returns an immutable snapshot of all findings in insertion order.
     *
     * <p>以不可变快照返回按加入顺序记录的全部发现。
     *
     * @return all recorded findings
     */
    public List<Violation> violations() {
        return List.copyOf(violations);
    }

    /**
     * Returns the fatal findings in insertion order.
     *
     * <p>按加入顺序返回所有会阻止启动的错误发现。
     *
     * @return the recorded errors
     */
    public List<Violation> errors() {
        return violations.stream().filter(v -> v.severity() == Severity.ERROR).toList();
    }

    /**
     * Returns the non-fatal findings in insertion order.
     *
     * <p>按加入顺序返回所有非致命警告发现。
     *
     * @return the recorded warnings
     */
    public List<Violation> warnings() {
        return violations.stream().filter(v -> v.severity() == Severity.WARNING).toList();
    }

    /**
     * Tests whether at least one fatal finding has been recorded.
     *
     * <p>判断报告中是否至少存在一条错误发现。
     *
     * @return {@code true} when the report contains an error
     */
    public boolean hasErrors() {
        return violations.stream().anyMatch(v -> v.severity() == Severity.ERROR);
    }

    /**
     * Throws if any error was recorded, listing every finding.
     *
     * <p>Called before the application is allowed to serve traffic, so a service whose
     * declarations do not make sense never reaches a state where it can accept a request.
     *
     * <p>若存在错误则抛出异常并列出全部发现，使无效声明的服务不会开始接收请求。
     *
     * @throws IllegalStateException if the report contains at least one error
     */
    public void throwIfInvalid() {
        if (!hasErrors()) {
            return;
        }
        throw new IllegalStateException(describe());
    }

    /**
     * Renders every finding as a single multi-line message.
     *
     * <p>Findings are sorted by severity, location and field, which places errors
     * before warnings because of the enum declaration order.
     *
     * <p>将全部发现按严重级别、位置和属性排序后渲染为多行报告文本。
     *
     * @return the validation report description
     */
    public String describe() {
        String body = violations.stream()
                .sorted(java.util.Comparator
                        .comparing(Violation::severity)
                        .thenComparing(Violation::location)
                        .thenComparing(Violation::field))
                .map(violation -> "  - " + violation)
                .collect(Collectors.joining(System.lineSeparator()));
        return String.format(Locale.ROOT,
                "%d service annotation problem(s) found (%d error(s), %d warning(s)):%s%s",
                violations.size(), errors().size(), warnings().size(), System.lineSeparator(), body);
    }
}
