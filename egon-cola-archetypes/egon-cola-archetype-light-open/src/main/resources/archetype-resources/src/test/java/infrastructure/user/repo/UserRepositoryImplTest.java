package ${package}.infrastructure.user.repo;

import ${package}.infrastructure.user.repo.dao.PermissionDAO;
import ${package}.infrastructure.user.repo.dao.RoleDAO;
import ${package}.infrastructure.user.repo.dao.RolePermissionDAO;
import ${package}.infrastructure.user.repo.dao.UserDAO;
import ${package}.infrastructure.user.repo.dao.UserRoleDAO;
import ${package}.infrastructure.user.repo.po.PermissionPO;
import ${package}.infrastructure.user.repo.po.RolePO;
import ${package}.infrastructure.user.repo.po.UserPO;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryImplTest {

    @Test
    void user_persistence_contract_uses_common_mapper_and_model() {
        assertThat(EgonColaMapper.class).isAssignableFrom(UserDAO.class);
        assertThat(EgonColaMapper.class).isAssignableFrom(RoleDAO.class);
        assertThat(EgonColaMapper.class).isAssignableFrom(PermissionDAO.class);
        assertThat(EgonColaMapper.class).isAssignableFrom(UserRoleDAO.class);
        assertThat(EgonColaMapper.class).isAssignableFrom(RolePermissionDAO.class);
        assertThat(EgonModel.class).isAssignableFrom(UserPO.class);
        assertThat(EgonModel.class).isAssignableFrom(RolePO.class);
        assertThat(UserPO.class.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class)
                .value()).isEqualTo("light_users");
    }
}
