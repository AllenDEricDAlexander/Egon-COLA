package top.egon.cola.component.tianshu.admin.repository;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcRuntimeMetadataTypeHandler;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcPublishPersistenceContractTest {

    private static final String BASE = "top.egon.cola.component.tianshu.admin.";
    private static final List<String> TABLES = List.of("DdcPublishTask", "DdcPublishAck", "DdcInstance", "DdcOperationLog");

    @Test
    void operationalRowsUseGuardedMpModelsAndKeepExternalConfigIdsSeparate() throws Exception {
        for (String name : TABLES) {
            Class<?> po = requiredType(BASE + "model.po." + name + "PO");
            Class<?> bo = requiredType(BASE + "model.bo." + name + "BO");
            Class<?> mapper = requiredType(BASE + "dao." + name + "Mapper");
            Class<?> implementation = requiredType(BASE + "repository.impl." + name + "RepositoryImpl");

            assertThat(po.getSuperclass()).isEqualTo(EgonModel.class);
            assertThat(po.getAnnotation(TableName.class).value()).isEqualTo(table(name));
            assertThat(EgonModel.class.isAssignableFrom(bo)).isFalse();
            assertThat(EgonColaMapper.class.isAssignableFrom(mapper)).isTrue();
            assertThat(EgonColaRepository.class.isAssignableFrom(implementation)).isTrue();
        }
        assertThat(fieldNames(requiredType(BASE + "model.po.DdcPublishTaskPO")))
                .contains("changeId", "configId", "targetVersion", "attemptCount", "ackCount", "status");
        assertThat(fieldNames(requiredType(BASE + "model.bo.DdcPublishTaskBO")))
                .contains("configItemId", "configId", "targetVersion", "attemptCount", "ackCount", "status");
        assertThat(fieldNames(requiredType(BASE + "model.po.DdcPublishAckPO")))
                .contains("changeId", "instanceId", "leaseId", "targetVersion", "ackStatus");
        assertThat(requiredType(BASE + "model.po.DdcPublishAckPO").getDeclaredField("resourceChecksum")
                .getAnnotation(TableField.class).value()).isEqualTo("content_checksum");
        assertThat(fieldNames(requiredType(BASE + "model.po.DdcInstancePO")))
                .contains("instanceId", "leaseId", "leaseExpireAt", "resourceVersion", "runtimeMetadata");
        assertThat(fieldNames(requiredType(BASE + "model.po.DdcOperationLogPO")))
                .contains("operationType", "operator", "operatorIp", "operationContent");
    }

    @Test
    void runtimeMetadataUsesAFieldScopedMybatisTypeHandler() throws Exception {
        Class<?> po = requiredType(BASE + "model.po.DdcInstancePO");
        Field metadata = po.getDeclaredField("runtimeMetadata");
        TableField mapping = metadata.getAnnotation(TableField.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.typeHandler()).isEqualTo(requiredType(BASE + "model.convertor.DdcRuntimeMetadataTypeHandler"));
        assertThat(BaseTypeHandler.class.isAssignableFrom(mapping.typeHandler())).isTrue();
    }

    @Test
    void runtimeMetadataTypeHandlerStoresAndLoadsJsonWithoutChangingOtherColumns() throws Exception {
        DdcRuntimeMetadataTypeHandler handler = new DdcRuntimeMetadataTypeHandler();
        java.sql.PreparedStatement statement = mock(java.sql.PreparedStatement.class);
        handler.setParameter(statement, 1, Map.of("zone", "blue"), JdbcType.VARCHAR);
        verify(statement).setString(1, "{\"zone\":\"blue\"}");

        java.sql.ResultSet resultSet = mock(java.sql.ResultSet.class);
        when(resultSet.getString("runtime_metadata")).thenReturn("{\"zone\":\"blue\"}");
        assertThat(handler.getResult(resultSet, "runtime_metadata")).containsEntry("zone", "blue");
    }

    @Test
    void operationalMapperSqlUsesLogicalTablesAndPreservesLifecycleStatePredicates() throws Exception {
        for (String name : TABLES) {
            String mapperName = name + "Mapper";
            String xml = mapper(mapperName + ".xml").toLowerCase(java.util.Locale.ROOT);
            String table = table(name);
            assertThat(xml).contains("from " + table, "tenant_id", "deleted_at is null", "selectactivebyid",
                    "selectactivebyids", "deleteversionedbyid")
                    .doesNotContain("from " + table + "_t0", "update " + table + "_t0", "<delete");
        }
        String taskXml = mapper("DdcPublishTaskMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(taskXml).contains("selectforupdatebychangeid", "for update", "claimstaleforrecovery",
                "updated_at &lt;", "attempt_count", "status", "failure_stage", "error_message");
        String ackXml = mapper("DdcPublishAckMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(ackXml).contains("selectforupdatebychangeidandinstanceidandleaseid", "for update",
                "lease_id is null", "markincompletetimeout", "ack_status is null", "resettargets");
        String instanceXml = mapper("DdcInstanceMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(instanceXml).contains("markofflineifleasematches", "markresourceadmissionoffline",
                "runtime_metadata", "resource_server_id", "resource_version", "admission_expires_at");
    }

    @Test
    void everyOperationalMapperXmlRegistersItsDeclaredCommands() throws Exception {
        Configuration configuration = operationalMappers();
        for (String name : TABLES) {
            String namespace = BASE + "dao." + name + "Mapper";
            assertThat(configuration.hasStatement(namespace + ".selectActiveById")).isTrue();
            assertThat(configuration.hasStatement(namespace + ".selectActiveByIds")).isTrue();
            assertThat(configuration.hasStatement(namespace + ".deleteVersionedById")).isTrue();
        }
        assertThat(configuration.hasStatement(BASE + "dao.DdcPublishTaskMapper.transitionToPublishing")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcPublishAckMapper.markIncompleteTimeout")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcInstanceMapper.markResourceAdmissionOffline")).isTrue();
    }

    @Test
    void mapperSqlRetainsTenantStateCasLeaseAndNullPredicates() throws Exception {
        Configuration configuration = operationalMappers();
        Map<String, Object> ackParameters = new HashMap<>();
        ackParameters.put("tenantId", 101L);
        ackParameters.put("changeId", "123");
        ackParameters.put("instanceId", "instance-1");
        ackParameters.put("leaseId", null);
        String ackSql = configuration.getMappedStatement(BASE
                        + "dao.DdcPublishAckMapper.selectForUpdateByChangeIdAndInstanceIdAndLeaseId")
                .getBoundSql(ackParameters).getSql().toLowerCase(java.util.Locale.ROOT);
        Map<String, Object> taskParameters = new HashMap<>();
        taskParameters.put("tenantId", 101L);
        taskParameters.put("changeId", "123");
        taskParameters.put("dispatchableStatuses", List.of("PENDING", "PUBLISHING"));
        taskParameters.put("pendingStatus", "PENDING");
        taskParameters.put("publishingStatus", "PUBLISHING");
        taskParameters.put("dispatchedAt", java.time.LocalDateTime.of(2026, 9, 28, 10, 0));
        taskParameters.put("actor", "system:test");
        String transitionSql = configuration.getMappedStatement(BASE + "dao.DdcPublishTaskMapper.transitionToPublishing")
                .getBoundSql(taskParameters).getSql().toLowerCase(java.util.Locale.ROOT);

        assertThat(ackSql).contains("tenant_id = ?", "change_id = ?", "instance_id = ?", "lease_id is null",
                "deleted_at is null", "for update");
        assertThat(transitionSql).contains("tenant_id = ?", "change_id = ?", "deleted_at is null",
                "status in", "attempt_count = case", "version = version + 1");
    }

    private Configuration operationalMappers() throws Exception {
        Configuration configuration = new Configuration();
        for (String name : TABLES) {
            String mapperName = name + "Mapper";
            String resource = "mybatis/mapper/tianshu/" + mapperName + ".xml";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertThat(input).as(resource).isNotNull();
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        return configuration;
    }

    private static String table(String modelName) {
        return switch (modelName) {
            case "DdcPublishTask" -> "ddc_publish_task";
            case "DdcPublishAck" -> "ddc_publish_ack";
            case "DdcInstance" -> "ddc_instance";
            case "DdcOperationLog" -> "ddc_operation_log";
            default -> throw new IllegalArgumentException(modelName);
        };
    }

    private Class<?> requiredType(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException failure) {
            throw new AssertionError("Missing operational persistence contract: " + name, failure);
        }
    }

    private Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
    }

    private String mapper(String name) throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("mybatis/mapper/tianshu/" + name)) {
            assertThat(input).as(name).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
