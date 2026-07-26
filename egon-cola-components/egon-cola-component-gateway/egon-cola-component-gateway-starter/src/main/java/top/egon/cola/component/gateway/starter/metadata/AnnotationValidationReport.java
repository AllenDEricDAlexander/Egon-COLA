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

    private final List<Violation> violations = new ArrayList<>();

    /** Severity of a single finding. */
    public enum Severity {
        ERROR,
        WARNING
    }

    /**
     * @param location human-readable declaration site, e.g. {@code com.acme.OrderService#place}
     * @param field    the annotation attribute at fault
     * @param message  what is wrong and what to do about it
     */
    public record Violation(Severity severity, String location, String field, String message) {

        public Violation {
            Objects.requireNonNull(severity, "severity");
            location = Objects.requireNonNull(location, "location");
            field = Objects.requireNonNull(field, "field");
            message = Objects.requireNonNull(message, "message");
        }

        @Override
        public String toString() {
            return severity + " " + location + " [" + field + "] " + message;
        }
    }

    public AnnotationValidationReport error(String location, String field, String message) {
        violations.add(new Violation(Severity.ERROR, location, field, message));
        return this;
    }

    public AnnotationValidationReport warning(String location, String field, String message) {
        violations.add(new Violation(Severity.WARNING, location, field, message));
        return this;
    }

    /** Records an error when {@code valid} is false; returns {@code valid} for chaining. */
    public boolean require(boolean valid, String location, String field, String message) {
        if (!valid) {
            error(location, field, message);
        }
        return valid;
    }

    public List<Violation> violations() {
        return List.copyOf(violations);
    }

    public List<Violation> errors() {
        return violations.stream().filter(v -> v.severity() == Severity.ERROR).toList();
    }

    public List<Violation> warnings() {
        return violations.stream().filter(v -> v.severity() == Severity.WARNING).toList();
    }

    public boolean hasErrors() {
        return violations.stream().anyMatch(v -> v.severity() == Severity.ERROR);
    }

    /**
     * Throws if any error was recorded, listing every finding.
     *
     * <p>Called before the application is allowed to serve traffic, so a service whose
     * declarations do not make sense never reaches a state where it can accept a request.
     */
    public void throwIfInvalid() {
        if (!hasErrors()) {
            return;
        }
        throw new IllegalStateException(describe());
    }

    /** Renders every finding, errors first, as a single multi-line message. */
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
