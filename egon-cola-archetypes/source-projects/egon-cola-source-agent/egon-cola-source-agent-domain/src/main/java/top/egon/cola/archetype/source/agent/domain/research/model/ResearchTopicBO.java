package top.egon.cola.archetype.source.agent.domain.research.model;

/** Normalized, bounded business topic for one Deep Research task. */
public record ResearchTopicBO(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 500;

    public ResearchTopicBO {
        value = normalize(value);
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("research topic length is outside the allowed range");
        }
        if (value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("research topic must not contain control characters");
        }
    }

    public static ResearchTopicBO create(String value) {
        return new ResearchTopicBO(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("research topic must not be null");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("research topic must not be blank");
        }
        return normalized;
    }
}
