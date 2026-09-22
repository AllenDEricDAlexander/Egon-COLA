package top.egon.cola.component.codegen.validation;

/**
 * Validation groups for generator configuration and plan application.
 *
 * <p>Constraints that belong to a scenario declare that group explicitly. The Default group is not
 * used to weaken those requirements.</p>
 */
public interface CodegenGroups {

    /**
     * Planning requires DDL input, profile and output root.
     */
    interface Plan {
    }

    /**
     * Apply requires a validated plan id and the same bound output root.
     */
    interface Apply {
    }
}
