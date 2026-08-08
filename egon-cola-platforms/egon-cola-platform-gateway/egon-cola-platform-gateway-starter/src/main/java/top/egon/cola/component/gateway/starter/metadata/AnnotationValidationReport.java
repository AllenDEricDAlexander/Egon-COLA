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
 */
public final class AnnotationValidationReport {

    /** Findings accumulated in insertion order. */
    private final List<Violation> violations = new ArrayList<>();

    /** Severity of a single annotation validation finding. */
    public enum Severity {
        /** A fatal declaration problem that prevents startup. */
        ERROR,

        /** A suspicious but legal declaration that does not prevent startup. */
        WARNING
    }

    /**
     * Describes one annotation validation finding.
     *
     * @param severity severity assigned to the finding
     * @param location human-readable declaration site, e.g. {@code com.acme.OrderService#place}
     * @param field    the annotation attribute at fault
     * @param message  what is wrong and what to do about it
     */
    public record Violation(Severity severity, String location, String field, String message) {

        /**
         * Validates and initializes a finding.
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
     * @param location human-readable declaration site
     * @param field annotation attribute at fault
     * @param message description of the problem and expected correction
     * @return this report
     */
    public AnnotationValidationReport error(String location, String field, String message) {
        violations.add(new Violation(Severity.ERROR, location, field, message));
        return this;
    }

    /**
     * Adds a non-fatal annotation validation finding.
     *
     * @param location human-readable declaration site
     * @param field annotation attribute at fault
     * @param message description of the suspicious declaration
     * @return this report
     */
    public AnnotationValidationReport warning(String location, String field, String message) {
        violations.add(new Violation(Severity.WARNING, location, field, message));
        return this;
    }

    /**
     * Records an error when {@code valid} is {@code false}.
     *
     * @param valid whether the declaration satisfies the requirement
     * @param location human-readable declaration site
     * @param field annotation attribute at fault
     * @param message description used when the requirement fails
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
     * @return all recorded findings
     */
    public List<Violation> violations() {
        return List.copyOf(violations);
    }

    /**
     * Returns the fatal findings in insertion order.
     *
     * @return the recorded errors
     */
    public List<Violation> errors() {
        return violations.stream().filter(v -> v.severity() == Severity.ERROR).toList();
    }

    /**
     * Returns the non-fatal findings in insertion order.
     *
     * @return the recorded warnings
     */
    public List<Violation> warnings() {
        return violations.stream().filter(v -> v.severity() == Severity.WARNING).toList();
    }

    /**
     * Tests whether at least one fatal finding has been recorded.
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
