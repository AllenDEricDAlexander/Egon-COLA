package top.egon.cola.component.gateway.starter.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataResolverTest {

    @Nested
    @DisplayName("precedence")
    class Precedence {

        @Test
        @DisplayName("the most specific declared level wins")
        void mostSpecificWins() {
            ResolvedMetadata<Long> resolved = MetadataResolver.<Long>chain()
                    .candidate(MetadataSource.METHOD, 100L)
                    .candidate(MetadataSource.CLASS, 200L)
                    .candidate(MetadataSource.CONFIGURATION, 300L)
                    .orDefault(3000L);

            assertEquals(100L, resolved.value());
            assertEquals(MetadataSource.METHOD, resolved.source());
        }

        @Test
        @DisplayName("resolution falls through each unset level in turn")
        void fallsThroughUnsetLevels() {
            ResolvedMetadata<Long> resolved = MetadataResolver.<Long>chain()
                    .candidate(MetadataSource.METHOD, -1L)
                    .candidate(MetadataSource.CLASS, -1L)
                    .candidate(MetadataSource.SERVICE_META, 250L)
                    .candidate(MetadataSource.CONFIGURATION, 300L)
                    .orDefault(3000L);

            assertEquals(250L, resolved.value());
            assertEquals(MetadataSource.SERVICE_META, resolved.source());
        }

        @Test
        @DisplayName("the component default applies when every level is unset")
        void defaultsWhenAllUnset() {
            ResolvedMetadata<Long> resolved = MetadataResolver.<Long>chain()
                    .candidate(MetadataSource.METHOD, -1L)
                    .candidate(MetadataSource.CLASS, -1L)
                    .orDefault(3000L);

            assertEquals(3000L, resolved.value());
            assertEquals(MetadataSource.DEFAULT, resolved.source());
            assertFalse(resolved.explicit());
        }

        @Test
        @DisplayName("precedence follows the level, not the order candidates were added")
        void orderOfAdditionIsIrrelevant() {
            // Guards against a caller silently inverting the chain by reordering calls.
            ResolvedMetadata<Long> resolved = MetadataResolver.<Long>chain()
                    .candidate(MetadataSource.CONFIGURATION, 300L)
                    .candidate(MetadataSource.METHOD, 100L)
                    .candidate(MetadataSource.CLASS, 200L)
                    .orDefault(3000L);

            assertEquals(100L, resolved.value());
        }
    }

    @Nested
    @DisplayName("sentinels distinguish unset from set-to-the-default")
    class Sentinels {

        @Test
        @DisplayName("an explicit zero overrides a less specific non-zero value")
        void explicitZeroOverrides() {
            // The point of the whole sentinel design: 0 means "do not retry" and must beat a
            // service-level 5. If 0 were treated as unset this would silently resolve to 5.
            ResolvedMetadata<Integer> resolved = MetadataResolver.<Integer>chain()
                    .candidate(MetadataSource.METHOD, 0)
                    .candidate(MetadataSource.CLASS, 5)
                    .orDefault(2);

            assertEquals(0, resolved.value());
            assertEquals(MetadataSource.METHOD, resolved.source());
        }

        @Test
        @DisplayName("a blank string is unset but a non-blank one is not")
        void blankStringsAreUnset() {
            assertEquals("fallback", MetadataResolver.<String>chain()
                    .candidate(MetadataSource.METHOD, "")
                    .candidate(MetadataSource.CLASS, "   ")
                    .orDefault("fallback").value());

            assertEquals("declared", MetadataResolver.<String>chain()
                    .candidate(MetadataSource.METHOD, "declared")
                    .orDefault("fallback").value());
        }

        @Test
        @DisplayName("an empty collection is unset")
        void emptyCollectionsAreUnset() {
            ResolvedMetadata<List<String>> resolved = MetadataResolver.<List<String>>chain()
                    .candidate(MetadataSource.METHOD, List.of())
                    .candidate(MetadataSource.CLASS, List.of("a"))
                    .orDefault(List.of());

            assertEquals(List.of("a"), resolved.value());
        }

        @Test
        @DisplayName("a null candidate is unset rather than an error")
        void nullCandidatesAreUnset() {
            // Configuration levels are frequently absent, so null must be ordinary input.
            assertEquals(7L, MetadataResolver.<Long>chain()
                    .candidate(MetadataSource.CONFIGURATION, null)
                    .candidate(MetadataSource.CLASS, 7L)
                    .orDefault(3000L).value());
        }

        @Test
        @DisplayName("a custom sentinel replaces the default test")
        void customSentinel() {
            // An enum's unset marker is a constant, not a negative number.
            ResolvedMetadata<Strategy> resolved = MetadataResolver.<Strategy>chain()
                    .candidate(MetadataSource.METHOD, Strategy.INHERIT)
                    .candidate(MetadataSource.CLASS, Strategy.RANDOM)
                    .unsetWhen(value -> value == null || value == Strategy.INHERIT)
                    .orDefault(Strategy.ROUND_ROBIN);

            assertEquals(Strategy.RANDOM, resolved.value());
            assertEquals(MetadataSource.CLASS, resolved.source());
        }

        private enum Strategy {
            INHERIT, ROUND_ROBIN, RANDOM
        }
    }

    @Nested
    @DisplayName("validation report")
    class ValidationReport {

        @Test
        @DisplayName("all findings are collected rather than failing on the first")
        void collectsEveryFinding() {
            AnnotationValidationReport report = new AnnotationValidationReport()
                    .error("com.acme.A#one", "weight", "must be 1..10000")
                    .error("com.acme.B#two", "version", "must be semver")
                    .warning("com.acme.C#three", "retries", "retries on a non-idempotent method");

            assertEquals(2, report.errors().size());
            assertEquals(1, report.warnings().size());
            assertTrue(report.hasErrors());
        }

        @Test
        @DisplayName("the thrown message names every problem at once")
        void messageListsEveryProblem() {
            AnnotationValidationReport report = new AnnotationValidationReport()
                    .error("com.acme.A#one", "weight", "must be 1..10000")
                    .error("com.acme.B#two", "version", "must be semver");

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    report::throwIfInvalid);

            assertTrue(thrown.getMessage().contains("com.acme.A#one"));
            assertTrue(thrown.getMessage().contains("com.acme.B#two"));
            assertTrue(thrown.getMessage().contains("2 error(s)"));
        }

        @Test
        @DisplayName("warnings alone do not block startup")
        void warningsDoNotThrow() {
            new AnnotationValidationReport()
                    .warning("com.acme.C#three", "retries", "suspicious but legal")
                    .throwIfInvalid();
        }

        @Test
        @DisplayName("require records a violation and reports the outcome")
        void requireRecordsAndReports() {
            AnnotationValidationReport report = new AnnotationValidationReport();

            assertFalse(report.require(false, "com.acme.A#one", "weight", "must be positive"));
            assertTrue(report.require(true, "com.acme.A#one", "weight", "must be positive"));
            assertEquals(1, report.errors().size());
        }
    }
}
