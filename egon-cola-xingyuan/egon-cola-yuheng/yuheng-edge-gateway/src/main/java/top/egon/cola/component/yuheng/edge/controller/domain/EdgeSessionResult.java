package top.egon.cola.component.yuheng.edge.controller.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EdgeSessionResult(
        boolean authenticated,
        String tenantId,
        String subject,
        String displayName) {
}
