package top.egon.cola.component.yuheng.edge.controller.domain;

public record EdgeLogoutFailureResult(
        String code,
        String message,
        boolean localLoggedOut,
        String remoteState) {
}
