package top.egon.cola.platform.tianquan.jianshen.admin.registration.ci;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.FrontendResourceType;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.dto.CiResourceRegistrationRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.service.CiResourceRegistrationCanonicalizer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Contract-level replacement for the live database IT; no service is started by this suite. */
class CiResourceRegistrationServiceIT {

    @Test
    void canonicalChecksumIsStableForEquivalentResourceOrder() {
        CiResourceRegistrationRequestDTO first = request(List.of(
                resource("route.b"), resource("route.a")));
        CiResourceRegistrationRequestDTO second = request(List.of(
                resource("route.a"), resource("route.b")));
        assertThat(CiResourceRegistrationCanonicalizer.checksum(first))
                .isEqualTo(CiResourceRegistrationCanonicalizer.checksum(second));
    }

    private static CiResourceRegistrationRequestDTO request(
            List<CiResourceRegistrationRequestDTO.Resource> resources) {
        CiResourceRegistrationRequestDTO unchecked = new CiResourceRegistrationRequestDTO(
                "build-1", "unchecked", 0L, resources, List.of());
        return new CiResourceRegistrationRequestDTO(
                unchecked.buildId(), CiResourceRegistrationCanonicalizer.checksum(unchecked),
                unchecked.expectedApplicationVersion(), unchecked.resources(), unchecked.fields());
    }

    private static CiResourceRegistrationRequestDTO.Resource resource(String code) {
        return new CiResourceRegistrationRequestDTO.Resource(
                FrontendResourceType.ROUTE, code, code, null, code + ":read",
                List.of(code + ".list"), "/" + code, code, null, 1, false);
    }
}
