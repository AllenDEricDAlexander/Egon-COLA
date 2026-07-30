package top.egon.cola.component.gateway.admin.application.reporting;

public record GatewayReportAuthentication(
        String applicationId,
        String applicationCode,
        String env,
        String namespace,
        String accessKey
) {

    public static final String REQUEST_ATTRIBUTE =
            GatewayReportAuthentication.class.getName();
}
