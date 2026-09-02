package top.egon.cola.archetype.source.webopen.adapter.facade.impl;

/** Validates decimal Long identifiers at external adapter boundaries. */
public final class OrganizationIdBoundary {

    private OrganizationIdBoundary() {
    }

    public static Long parse(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " must be a positive decimal Long");
        }
        try {
            long value = Long.parseLong(raw.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(field + " must be a positive decimal Long");
            }
            return value;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(field + " must be a positive decimal Long", failure);
        }
    }
}
