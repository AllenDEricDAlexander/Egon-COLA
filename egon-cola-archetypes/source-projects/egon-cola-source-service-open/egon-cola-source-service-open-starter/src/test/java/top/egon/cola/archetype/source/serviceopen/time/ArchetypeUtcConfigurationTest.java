package top.egon.cola.archetype.source.serviceopen.time;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.FileSystemResource;

import static org.junit.jupiter.api.Assertions.*;

class ArchetypeUtcConfigurationTest {

    private static final Path MODULE = Path.of("").toAbsolutePath();

    @Test
    void everyProfileDeclaresTheSameUtcIsoContract() throws Exception {
        var loader = new YamlPropertySourceLoader();
        for (String file : new String[]{"application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml"}) {
            var sources = loader.load(file, new FileSystemResource(MODULE.resolve("src/main/resources").resolve(file)));
            assertEquals(1, sources.size(), file);
            var source = sources.getFirst();
            assertEquals("UTC", source.getProperty("spring.jackson.time-zone"), file);
            assertEquals(false, source.getProperty("spring.jackson.serialization.write-dates-as-timestamps"), file);
            assertNull(source.getProperty("spring.jackson.date-format"), file);
            String text = Files.readString(MODULE.resolve("src/main/resources").resolve(file));
            assertFalse(text.contains("APP_TIME_ZONE"), file);
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                    .withPropertyValues("spring.jackson.time-zone=" + source.getProperty("spring.jackson.time-zone"),
                            "spring.jackson.serialization.write-dates-as-timestamps="
                                    + source.getProperty("spring.jackson.serialization.write-dates-as-timestamps"))
                    .run(context -> {
                        ObjectMapper mapper = context.getBean(ObjectMapper.class);
                        TimeZone original = TimeZone.getDefault();
                        try {
                            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                                assertEquals("\"2026-10-13T01:00:00.123456Z\"", mapper.writeValueAsString(
                                        Instant.parse("2026-10-13T01:00:00.123456Z")));
                                assertEquals("\"2026-10-13\"", mapper.writeValueAsString(LocalDate.of(2026, 10, 13)));
                                assertEquals("\"09:30:00\"", mapper.writeValueAsString(LocalTime.of(9, 30)));
                            }
                        } finally {
                            TimeZone.setDefault(original);
                        }
                    });
        }
    }

    @Test
    void launchAndExistingContainerUseUtcDefaults() throws Exception {
        String launch = Files.readString(MODULE.resolve("src/main/launch/launch.args"));
        assertTrue(launch.contains("-Duser.timezone=UTC"));
        Path familyRoot = MODULE;
        while (!Files.exists(familyRoot.resolve("README.md"))) {
            familyRoot = familyRoot.getParent();
            assertNotNull(familyRoot, "family root must exist");
        }
        Path docker = familyRoot.resolve("deploy/container/Dockerfile");
        String container = Files.readString(docker);
        assertTrue(container.contains("ENV TZ=UTC"));
        assertTrue(container.contains("-Duser.timezone=UTC"));
        assertFalse(container.contains("Asia/Shanghai"));
    }
}
