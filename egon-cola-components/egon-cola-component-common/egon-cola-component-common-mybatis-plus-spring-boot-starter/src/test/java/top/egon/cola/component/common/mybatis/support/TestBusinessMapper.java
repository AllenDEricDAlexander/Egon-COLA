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
 * <p>The production EgonColaMapper remains intentionally empty. These
 * statements exist only to expose adversarial SQL shapes to the test chain.</p>
 */
@Mapper
public interface TestBusinessMapper extends EgonColaMapper<TestBusinessModel> {

    @Select("select * from test_business_record where tenant_id = #{tenantId}")
    List<TestBusinessModel> explicitTenant(@Param("tenantId") Long tenantId);

    @Update("update test_business_record set tenant_id = #{tenantId} where id = #{id}")
    int forbiddenTenantMutation(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Update("update test_business_record set is_deleted = #{deleted} where id = #{id}")
    int forbiddenLogicDeleteMutation(@Param("id") Long id, @Param("deleted") Boolean deleted);

    @Select("select * from test_global_record")
    List<Map<String, Object>> globalRows();

    @Select("select 1")
    List<Integer> unsupportedStatement();
}
