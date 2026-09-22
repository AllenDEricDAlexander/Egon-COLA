package top.egon.cola.component.common.mybatis.model;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Validation groups used by repository operations on an EgonModel.
 */
public final class EgonColaModelValidationGroups {

    private EgonColaModelValidationGroups() {
    }

    public interface Insert {
    }

    /** Requires an existing id and caller-supplied expected version. */
    public interface Update {
    }

    /** Logical deletion retains optimistic locking and never restores an inactive row. */
    public interface Delete {
    }

    public interface Query {
    }

    public interface Persisted {
    }

    public enum Operation implements EgonEnum {
        INSERT(0, "INSERT", Insert.class),
        UPDATE(1, "UPDATE", Update.class),
        DELETE(2, "DELETE", Delete.class),
        QUERY(3, "QUERY", Query.class),
        LOADED(4, "LOADED", Persisted.class);

        private final int code;
        private final String message;
        private final Class<?> group;

        Operation(int code, String message, Class<?> group) {
            this.code = code;
            this.message = message;
            this.group = group;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }

        public Class<?> group() {
            return group;
        }
    }
}
