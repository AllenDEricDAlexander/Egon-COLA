package top.egon.cola.component.common.mybatis.support;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;
import java.util.Map;

/**
 * Test-only Mapper used for later real-SQL guard cases.
 *
 * Shared active reads and versioned deletes are bound in mybatis/TestBusinessMapper.xml.
 * Other statements expose adversarial SQL shapes to the test chain.
 */
@Mapper
public interface TestBusinessMapper extends EgonColaMapper<TestBusinessModel> {

    @Select("select * from test_business_record where tenant_id = #{tenantId} AND deleted_at IS NULL")
    List<TestBusinessModel> explicitTenant(@Param("tenantId") Long tenantId);

    @Update("update test_business_record set tenant_id = #{tenantId} where id = #{id}")
    int forbiddenTenantMutation(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Select("select * from test_global_record")
    List<Map<String, Object>> globalRows();

    @Select("select 1")
    List<Integer> unsupportedStatement();
}
