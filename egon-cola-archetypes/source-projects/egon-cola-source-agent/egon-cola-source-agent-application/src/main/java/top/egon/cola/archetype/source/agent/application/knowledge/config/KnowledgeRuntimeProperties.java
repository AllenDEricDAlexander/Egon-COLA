package top.egon.cola.archetype.source.agent.application.knowledge.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Duration;

/**
 * Application-owned limits for the knowledge use cases.
 *
 * <p>The embedding endpoint is not here: it belongs to the infrastructure that owns the model bean
 * ({@code agent.knowledge.embedding.*}), and this record deliberately binds only the runtime limits
 * and the tenant the request scope is opened with, so a sibling key can never be bound by accident.
 */
public record KnowledgeRuntimeProperties(@Valid @NotNull Runtime runtime, @Valid @NotNull Tenant tenant) {

    /** Longest question and answer stream the QA use case will hold a capacity permit for. */
    private static final Duration MAX_QA_DURATION = Duration.ofMinutes(30);

    public KnowledgeRuntimeProperties {
        runtime = runtime == null ? new Runtime(4, 20L * 1024 * 1024, 10_000, Duration.ofMinutes(2)) : runtime;
        tenant = tenant == null ? new Tenant(0L) : tenant;
    }

    public record Runtime(@Min(1) @Max(32) int qaMaxConcurrent,
                          @Min(1) long maxUploadBytes,
                          @Min(1) int maxDocumentsPerBase,
                          @NotNull Duration qaMaxDuration) {

        public Runtime {
            if (qaMaxConcurrent < 1 || qaMaxConcurrent > 32) {
                throw new IllegalArgumentException("qaMaxConcurrent must be between 1 and 32");
            }
            if (maxUploadBytes < 1) {
                throw new IllegalArgumentException("maxUploadBytes must be positive");
            }
            if (maxDocumentsPerBase < 1) {
                throw new IllegalArgumentException("maxDocumentsPerBase must be positive");
            }
            qaMaxDuration = qaMaxDuration == null ? Duration.ofMinutes(2) : qaMaxDuration;
            if (qaMaxDuration.isZero() || qaMaxDuration.isNegative()
                    || qaMaxDuration.compareTo(MAX_QA_DURATION) > 0) {
                throw new IllegalArgumentException("qaMaxDuration is outside the allowed range");
            }
        }
    }

    public record Tenant(@PositiveOrZero long defaultId) {

        public Tenant {
            if (defaultId < 0) {
                throw new IllegalArgumentException("defaultId must not be negative");
            }
        }
    }
}
