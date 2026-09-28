package top.egon.cola.component.tianshu.admin.repository;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DdcMetadataPagingRepositoryTest {

    private static final Map<String, String> MAPPERS = Map.of(
            "DdcBizMapper.xml", "ORDER BY biz_code ASC, id ASC",
            "DdcEnvMapper.xml", "ORDER BY e.sort_order, e.env_code, e.id",
            "DdcAppMapper.xml", "ORDER BY a.biz_code, a.app_code, a.id",
            "DdcNamespaceMapper.xml", "ORDER BY biz_code, namespace_code, id",
            "DdcNamespaceEnvAppBindingMapper.xml", "ORDER BY n.biz_code, n.namespace_code, b.env_code, a.app_code, b.id");

    @Test
    void metadataPagingSqlPreservesStableSortAndTenantScopedFilters() throws IOException {
        for (Map.Entry<String, String> entry : MAPPERS.entrySet()) {
            String xml = mapper(entry.getKey()).toLowerCase(java.util.Locale.ROOT);
            String pageStatement = entry.getKey().equals("DdcNamespaceEnvAppBindingMapper.xml")
                    ? "<select id=\"selectjoinedpage\"" : "<select id=\"selectpage\"";
            assertThat(xml).contains(pageStatement)
                    .contains(entry.getValue().toLowerCase(java.util.Locale.ROOT))
                    .contains("limit #{limit} offset #{offset}")
                    .contains("tenant_id = #{tenantid}")
                    .contains("deleted_at is null");
        }
    }

    @Test
    void optionalMetadataFiltersRetainNullAndVisibilitySemantics() throws IOException {
        String env = mapper("DdcEnvMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(env).contains("bizcode != null and bizcode != '' and namespacecode != null")
                .contains("b.enabled = true", "n.enabled = true", "exists (select 1");

        String app = mapper("DdcAppMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(app).contains("namespacecode != null and namespacecode != '' and env != null")
                .contains("b.app_id = a.id", "n.namespace_code = #{namespacecode}", "a.enabled = #{enabled}");

        String binding = mapper("DdcNamespaceEnvAppBindingMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(binding).contains("join ddc_namespace n on n.tenant_id = b.tenant_id")
                .contains("join ddc_app a on a.tenant_id = b.tenant_id")
                .contains("n.deleted_at is null and a.deleted_at is null")
                .contains("b.enabled = true and n.enabled = true");
    }

    private String mapper(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("mybatis/mapper/tianshu/" + name)) {
            assertThat(input).as(name + " mapper XML").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
