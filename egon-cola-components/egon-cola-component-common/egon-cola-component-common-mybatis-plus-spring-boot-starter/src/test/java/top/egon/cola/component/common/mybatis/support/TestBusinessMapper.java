package top.egon.cola.component.common.mybatis.support;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

/**
 * Test-only Mapper used for later real-SQL guard cases.
 *
 * <p>The production EgonColaMapper remains intentionally empty. These
 * statements exist only to expose adversarial SQL shapes to the test chain.</p>
 */
@Mapper
public interface TestBusinessMapper extends EgonColaMapper<TestBusinessModel> {

    @Select("select * from test_business_record where tenant_id = #{tenantId}")
    List<TestBusinessModel> explicitTenant(Long tenantId);

    @Update("update test_business_record set tenant_id = #{tenantId} where id = #{id}")
    int forbiddenTenantMutation(Long id, Long tenantId);
}
