package top.egon.cola.component.common.mybatis.model;

/**
 * Validation groups used by repository operations on an EgonModel.
 */
public final class EgonColaModelValidationGroups {

    private EgonColaModelValidationGroups() {
    }

    public interface Insert {
    }

    public interface Update {
    }

    public interface Delete {
    }

    public interface Query {
    }

    public interface Persisted {
    }

    public enum Operation {
        INSERT(Insert.class),
        UPDATE(Update.class),
        DELETE(Delete.class),
        QUERY(Query.class),
        LOADED(Persisted.class);

        private final Class<?> group;

        Operation(Class<?> group) {
            this.group = group;
        }

        public Class<?> group() {
            return group;
        }
    }
}
