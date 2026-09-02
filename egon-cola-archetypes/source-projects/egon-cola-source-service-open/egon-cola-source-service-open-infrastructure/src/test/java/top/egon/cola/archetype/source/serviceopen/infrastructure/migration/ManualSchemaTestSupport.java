package top.egon.cola.archetype.source.serviceopen.infrastructure.migration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

final class ManualSchemaTestSupport {

    private ManualSchemaTestSupport() {
    }

    static String read(String location) {
        try (InputStream input = new ClassPathResource(location).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new AssertionError("manual SQL resource is not readable: " + location, failure);
        }
    }
}
