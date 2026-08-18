package top.egon.cola.platform.rbac3.admin.iam.resource.report.service;

import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto.CiResourceReportRequestDTO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

/** Produces the stable checksum shared by CI and the RBAC report endpoint. */
public final class CiResourceReportCanonicalizer {

    private CiResourceReportCanonicalizer() {
    }

    public static String checksum(CiResourceReportRequestDTO request) {
        String canonical = request.buildId() + "|resources="
                + request.resources().stream()
                .sorted(Comparator.comparing(value -> value.type().name() + ':' + value.code()))
                .map(value -> String.join("|",
                        value.type().name(), value.code(), value.name(),
                        nullToEmpty(value.parentCode()), nullToEmpty(value.permissionCode()),
                        nullToEmpty(value.path()), nullToEmpty(value.componentKey()),
                        nullToEmpty(value.routeCode()), String.valueOf(value.order()),
                        String.valueOf(value.hidden())))
                .reduce("", (left, right) -> left + right + ";")
                + "|fields="
                + request.fields().stream()
                .sorted(Comparator.comparing(value -> value.resourceCode() + ':' + value.fieldCode()))
                .map(value -> String.join("|", value.resourceCode(), value.fieldCode(),
                        value.jsonPath(), value.dataType()))
                .reduce("", (left, right) -> left + right + ";");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
