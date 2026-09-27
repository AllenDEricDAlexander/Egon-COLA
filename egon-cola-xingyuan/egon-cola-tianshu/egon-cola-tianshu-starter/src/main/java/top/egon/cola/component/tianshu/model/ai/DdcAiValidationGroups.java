package top.egon.cola.component.tianshu.model.ai;

import jakarta.validation.groups.Default;

public final class DdcAiValidationGroups {

    private DdcAiValidationGroups() {
    }

    public interface Create extends Save {
    }

    public interface Save extends Default {
    }

    public interface Publish extends Save {
    }

    public interface Read extends Default {
    }
}
