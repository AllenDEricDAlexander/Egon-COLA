package ${package}.infrastructure.user.repo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperSqlIntegrationTest {

    @Test
    void dao_xml_uses_logical_light_tables_and_explicit_columns() throws IOException {
        for (String resource : List.of(
                "mybatis/mapper/user/UserDAO.xml",
                "mybatis/mapper/user/RoleDAO.xml",
                "mybatis/mapper/user/PermissionDAO.xml",
                "mybatis/mapper/user/UserRoleDAO.xml",
                "mybatis/mapper/user/RolePermissionDAO.xml")) {
            String xml = read(resource);
            assertThat(xml).contains("repo.dao.").contains("light_")
                    .doesNotContain("SELECT *");
        }
    }

    private static String read(String resource) throws IOException {
        try (InputStream input = MapperSqlIntegrationTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertThat(input).as("mapper resource %s", resource).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
