package top.egon.cola.archetype.source.lightopen.infrastructure.user.repo;

import top.egon.cola.archetype.source.lightopen.infrastructure.user.dao.PermissionDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.dao.RoleDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.dao.RolePermissionDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.dao.UserDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.dao.UserRoleDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.po.PermissionPO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.po.RolePO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.po.UserPO;
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
