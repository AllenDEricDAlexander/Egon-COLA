package top.egon.cola.component.common.model.request;

import java.io.Serial;
import java.io.Serializable;

/**
 * Base request carrying optional operator context.
 */
public class BaseRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private OperatorContext operator;

    public OperatorContext getOperator() {
        return operator;
    }

    public void setOperator(OperatorContext operator) {
        this.operator = operator;
    }
}
