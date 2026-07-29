package top.egon.cola.component.accessguard.core;

public record GuardFailure(String category, String code) {

    public GuardFailure {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        category = category.trim();
        code = code.trim();
    }
}
