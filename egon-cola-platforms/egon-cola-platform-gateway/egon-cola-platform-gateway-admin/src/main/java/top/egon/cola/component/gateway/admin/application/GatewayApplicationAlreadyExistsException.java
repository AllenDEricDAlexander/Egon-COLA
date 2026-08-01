package top.egon.cola.component.gateway.admin.application;

public class GatewayApplicationAlreadyExistsException
        extends RuntimeException {

    private final String existingApplicationId;

    public GatewayApplicationAlreadyExistsException(
            String existingApplicationId) {
        super("gateway application already exists: "
                + existingApplicationId);
        this.existingApplicationId = existingApplicationId;
    }

    public String existingApplicationId() {
        return existingApplicationId;
    }
}
