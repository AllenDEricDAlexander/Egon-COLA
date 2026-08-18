package top.egon.cola.platform.rbac3.admin.iam.resource.report;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.FrontendResourceType;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto.CiResourceReportRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.service.CiResourceReportCanonicalizer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Contract-level replacement for the live database IT; no service is started by this suite. */
class CiResourceReportServiceIT {

    @Test
    void canonicalChecksumIsStableForEquivalentResourceOrder() {
        CiResourceReportRequestDTO first = request(List.of(
                resource("route.b"), resource("route.a")));
        CiResourceReportRequestDTO second = request(List.of(
                resource("route.a"), resource("route.b")));
        assertThat(CiResourceReportCanonicalizer.checksum(first))
                .isEqualTo(CiResourceReportCanonicalizer.checksum(second));
    }

    private static CiResourceReportRequestDTO request(
            List<CiResourceReportRequestDTO.Resource> resources) {
        CiResourceReportRequestDTO unchecked = new CiResourceReportRequestDTO(
                "build-1", "unchecked", 0L, resources, List.of());
        return new CiResourceReportRequestDTO(
                unchecked.buildId(), CiResourceReportCanonicalizer.checksum(unchecked),
                unchecked.expectedApplicationVersion(), unchecked.resources(), unchecked.fields());
    }

    private static CiResourceReportRequestDTO.Resource resource(String code) {
        return new CiResourceReportRequestDTO.Resource(
                FrontendResourceType.ROUTE, code, code, null, code + ":read",
                "/" + code, code, null, 1, false);
    }
}
