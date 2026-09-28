package top.egon.cola.component.tianshu.admin.repository;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DdcConfigPagingRepositoryTest {

    @Test
    void configAndVersionPagingSqlKeepTenantAndStableLegacyOrder() throws IOException {
        String item = mapper("DdcConfigItemMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(item).contains("<select id=\"selectpage\"", "<select id=\"countpage\"",
                "where c.tenant_id = #{tenantid}", "c.deleted_at is null", "c.deleted = false",
                "order by c.biz_code, c.env, c.app_code, c.config_key, c.id", "limit #{limit} offset #{offset}");

        String version = mapper("DdcConfigVersionMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(version).contains("<select id=\"selectpagebyconfigpublicid\"",
                "<select id=\"countpagebyconfigpublicid\"", "v.tenant_id = #{tenantid}",
                "v.deleted_at is null", "c.public_id = #{configpublicid}",
                "order by v.config_id, v.config_revision desc, v.id desc", "limit #{limit} offset #{offset}");
    }

    @Test
    void publishedVersionCachePageIsBoundToCurrentTenantPointerAndBusinessDeletion() throws IOException {
        String version = mapper("DdcConfigVersionMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(version).contains("<select id=\"selectpublishedruntimepage\"", "c.tenant_id = #{tenantid}",
                "v.tenant_id = #{tenantid}", "c.deleted = false", "v.config_revision = c.published_version",
                "v.change_type &lt;&gt; #{deletetype}", "order by v.config_key asc, v.id asc");
    }

    private String mapper(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mybatis/mapper/tianshu/" + name)) {
            assertThat(input).as(name).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
