package top.egon.cola.archetype.source.agent.adapter.config;

import jakarta.validation.constraints.NotBlank;

/** Immutable settings for explicitly constructed legacy fixtures, excluded from production binding. */
@Deprecated
public record DeepResearchApiProperties(@NotBlank String apiKey) {
}
