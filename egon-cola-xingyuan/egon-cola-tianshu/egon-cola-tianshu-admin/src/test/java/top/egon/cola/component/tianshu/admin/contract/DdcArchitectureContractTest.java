package top.egon.cola.component.tianshu.admin.contract;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.tianshu.admin.DynamicConfigCenterAdminApplication;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DdcArchitectureContractTest {

    private static final Path MAIN = Path.of("src/main/java/top/egon/cola/component/tianshu/admin");
    private static final Path TEST = Path.of("src/test/java/top/egon/cola/component/tianshu/admin");

    @Test
    void moduleSourcesAndTestsDoNotDependOnJpaPersistence() throws IOException {
        List<String> violations = Stream.concat(javaFiles(MAIN), javaFiles(TEST))
                .filter(this::usesJpaPersistence)
                .map(Path::toString)
                .toList();

        assertThat(violations).as("JPA persistence references").isEmpty();
    }

    @Test
    void runtimePomNoLongerCarriesJpaFlywayOrSqlite() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        assertThat(pom).doesNotContain("spring-boot-starter-data-jpa", "flyway-core",
                "flyway-database-postgresql", "hibernate-community-dialects")
                .containsPattern("(?s)<dependency>\\s*<groupId>org\\.xerial</groupId>\\s*"
                        + "<artifactId>sqlite-jdbc</artifactId>\\s*<scope>test</scope>\\s*</dependency>");
        for (String profile : List.of("application.yml", "application-local.yml", "application-test.yml")) {
            String configuration = Files.readString(Path.of("src/main/resources", profile));
            assertThat(configuration).doesNotContain("spring.flyway", "jdbc:sqlite", "org.sqlite", "spring.jpa");
        }
    }

    @Test
    void moduleDoesNotExposeSpringDataPageTypes() throws IOException {
        List<String> violations = Stream.concat(javaFiles(MAIN), javaFiles(TEST))
                .filter(this::usesSpringDataPage)
                .map(Path::toString)
                .toList();

        assertThat(violations).as("Spring Data page API references").isEmpty();
    }

    @Test
    void controllersAndServicesDoNotDependOnMapperOrPersistenceModels() throws IOException {
        List<Path> controllers = javaFiles(MAIN.resolve("controller")).toList();
        List<Path> services = javaFiles(MAIN.resolve("service")).toList();
        assertThat(controllers.stream().filter(this::usesMapperOrPersistenceModel).toList()).isEmpty();
        assertThat(services.stream().filter(this::usesMapperOrPersistenceModel).toList()).isEmpty();
    }

    @Test
    void mapperInterfacesAndXmlStayInsideTheMpPersistenceBoundary() throws IOException {
        Path dao = MAIN.resolve("dao");
        Path mapperResources = Path.of("src/main/resources/mybatis/mapper/tianshu");
        try (Stream<Path> mappers = Files.list(dao)) {
            for (Path mapper : mappers.filter(path -> path.toString().endsWith("Mapper.java")).toList()) {
                String source = Files.readString(mapper);
                assertThat(EgonColaMapper.class.isAssignableFrom(loadMapper(mapper))).isTrue();
                String xml = mapperResources.resolve(mapper.getFileName().toString().replace(".java", ".xml")).toString();
                assertThat(Files.exists(Path.of(xml))).as(xml).isTrue();
                assertThat(source).doesNotContain("EntityManager", "JpaRepository");
            }
        }
    }

    @Test
    void applicationScansOnlyTheAdminMapperPackage() {
        MapperScan scan = DynamicConfigCenterAdminApplication.class.getAnnotation(MapperScan.class);

        assertThat(scan).isNotNull();
        assertThat(scan.value()).containsExactly("top.egon.cola.component.tianshu.admin.dao");
        assertThat(scan.lazyInitialization()).isEqualTo("true");
    }

    @Test
    void datasourceKeysStayStructurallyEqualAcrossShippedProfiles() {
        Set<String> expected = Set.of("spring.datasource.url", "spring.datasource.username",
                "spring.datasource.password", "spring.datasource.driver-class-name");

        for (String profile : List.of("application.yml", "application-local.yml", "application-test.yml")) {
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new ClassPathResource(profile));
            Properties properties = yaml.getObject();
            assertThat(properties).isNotNull();
            Set<String> actual = properties.stringPropertyNames().stream()
                    .filter(key -> key.startsWith("spring.datasource."))
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(actual).as(profile).isEqualTo(expected);
        }
    }

    @Test
    void activeActiveIntegrationUsesManagedDdlWithoutJpaOrFlyway() throws IOException {
        String test = Files.readString(TEST.resolve("integration/DdcActiveActiveAdminIT.java"));

        assertThat(test).contains("DdcPersistenceMigrationCli.main(new String[]{\"initialize\"})",
                "initializeManagedPersistenceSchema")
                .doesNotContain("--spring.jpa.hibernate.ddl-auto", "--spring.flyway.enabled");
    }

    private boolean usesJpaPersistence(Path source) {
        try {
            String text = Files.readString(source);
            return text.lines().anyMatch(line -> line.matches("\\s*import\\s+(jakarta\\.persistence\\.|"
                    + "org\\.springframework\\.data\\.jpa\\.|org\\.hibernate\\.)[^;]*;\\s*")
                    || line.matches("\\s*@(DataJpaTest|EntityScan|EnableJpaRepositories)\\b.*"));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect source: " + source, failure);
        }
    }

    private boolean usesSpringDataPage(Path source) {
        try {
            String text = Files.readString(source);
            return text.lines().anyMatch(line -> line.matches("\\s*import\\s+org\\.springframework\\.data\\.domain\\.(Page|PageImpl|PageRequest|Pageable|Sort)(;|\\..*;)"));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect source: " + source, failure);
        }
    }

    private boolean usesMapperOrPersistenceModel(Path source) {
        try {
            String text = Files.readString(source);
            return text.contains("admin.dao.") || text.contains("admin.model.po.")
                    || text.contains("admin.model.entity.");
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect source: " + source, failure);
        }
    }

    private Stream<Path> javaFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList().stream();
        }
    }

    private Class<?> loadMapper(Path path) {
        String packageName = "top.egon.cola.component.tianshu.admin.dao.";
        String className = packageName + path.getFileName().toString().replace(".java", "");
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException failure) {
            throw new AssertionError("Missing Mapper class " + className, failure);
        }
    }
}
