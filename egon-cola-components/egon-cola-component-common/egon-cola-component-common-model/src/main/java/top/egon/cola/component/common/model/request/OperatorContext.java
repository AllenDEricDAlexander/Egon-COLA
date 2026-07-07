package top.egon.cola.component.common.model.request;

import java.io.Serial;
import java.io.Serializable;

/**
 * Operator identity carried by application requests.
 */
public class OperatorContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String operatorId;

    private String operatorName;

    private String tenantId;

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
