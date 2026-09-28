package top.egon.cola.component.tianshu.admin.repository;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DdcOperationalPagingRepositoryTest {

    @Test
    void taskAndInstancePagesParseAsTenantScopedLogicalTableQueries() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, "DdcPublishTaskMapper.xml");
        parse(configuration, "DdcInstanceMapper.xml");

        String taskSql = configuration.getMappedStatement(
                "top.egon.cola.component.tianshu.admin.dao.DdcPublishTaskMapper.selectSearch")
                .getBoundSql(Map.of("tenantId", 101L, "bizCode", "infra", "env", "prod", "appCode", "yuheng",
                        "status", "FAILED", "changeId", "019", "offset", 0L, "limit", 10))
                .getSql().toLowerCase();
        String instanceSql = configuration.getMappedStatement(
                "top.egon.cola.component.tianshu.admin.dao.DdcInstanceMapper.selectPage")
                .getBoundSql(Map.of("tenantId", 101L, "bizCode", "infra", "env", "prod", "appCode", "yuheng",
                        "offset", 0L, "limit", 10))
                .getSql().toLowerCase();

        assertThat(taskSql).contains("from ddc_publish_task", "t.tenant_id = ?", "t.deleted_at is null",
                "t.status = ?", "order by t.created_at desc, t.id desc", "limit ? offset ?")
                .doesNotContain("ddc_publish_task_t0");
        assertThat(instanceSql).contains("from ddc_instance", "tenant_id = ?", "deleted_at is null",
                "order by updated_at desc, id desc", "limit ? offset ?")
                .doesNotContain("ddc_instance_t0");
        assertThat(configuration.hasStatement(
                "top.egon.cola.component.tianshu.admin.dao.DdcPublishTaskMapper.countSearch")).isTrue();
        assertThat(configuration.hasStatement(
                "top.egon.cola.component.tianshu.admin.dao.DdcInstanceMapper.countPage")).isTrue();
    }

    private void parse(Configuration configuration, String mapperName) throws Exception {
        String resource = "mybatis/mapper/tianshu/" + mapperName;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).as(resource).isNotNull();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }
}
