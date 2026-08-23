package ${package}.infrastructure.user.repo;

import ${package}.infrastructure.user.repo.mapper.PermissionMapper;
import ${package}.infrastructure.user.repo.mapper.RoleMapper;
import ${package}.infrastructure.user.repo.mapper.RolePermissionMapper;
import ${package}.infrastructure.user.repo.mapper.UserMapper;
import ${package}.infrastructure.user.repo.mapper.UserRoleMapper;
import ${package}.infrastructure.user.repo.po.PermissionPO;
import ${package}.infrastructure.user.repo.po.RolePO;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import ${package}.infrastructure.user.repo.po.UserPO;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import org.h2.jdbcx.JdbcDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperSqlIntegrationTest {

    @Test
    void executes_explicit_mapper_xml_against_manual_master_schema() throws Exception {
        DataSource dataSource = dataSource();
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(
                    "db/manual/postgresql/master-data/001__create_light_master_data_schema.sql"));
        }

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(UserMapper.class);
        configuration.addMapper(RoleMapper.class);
        configuration.addMapper(PermissionMapper.class);
        configuration.addMapper(UserRoleMapper.class);
        configuration.addMapper(RolePermissionMapper.class);

        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setConfiguration(configuration);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mybatis/mapper/user/*.xml"));
        factoryBean.afterPropertiesSet();
        SqlSessionFactory factory = factoryBean.getObject();

        try (SqlSession session = factory.openSession(true)) {
            UserMapper users = session.getMapper(UserMapper.class);
            RoleMapper roles = session.getMapper(RoleMapper.class);
            PermissionMapper permissions = session.getMapper(PermissionMapper.class);
            UserRoleMapper userRoles = session.getMapper(UserRoleMapper.class);
            RolePermissionMapper rolePermissions = session.getMapper(RolePermissionMapper.class);
            Instant now = Instant.now();

            assertThat(users.insert(new UserPO(1001L, "ext-1001", "Mario",
                    "mario@example.com", "ACTIVE", now))).isOne();
            assertThat(roles.insert(new RolePO("teacher", "Teacher", "ACTIVE", now))).isOne();
            assertThat(permissions.insert(new PermissionPO(
                    "course:read", "Read courses", "ACTIVE", now))).isOne();
            assertThat(userRoles.insertRelation(new UserRolePO(1001L, "teacher", now))).isOne();
            assertThat(rolePermissions.insertRelation(new RolePermissionPO(
                    "teacher", "course:read", now))).isOne();

            assertThat(userRoles.findByUserId(1001L)).hasSize(1);
            assertThat(rolePermissions.findByRoleCodes(List.of("teacher"))).hasSize(1);
            assertThat(permissions.findByCodesOrderByCode(List.of("course:read")))
                    .extracting(PermissionPO::getCode)
                    .containsExactly("course:read");
        }
    }

    private static DataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:mapper-sql;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
