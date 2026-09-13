package top.egon.cola.component.common.mybatis.ddl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ordered, content-addressed classpath SQL; previously applied entries remain an immutable prefix. */
public record EgonColaDdlManifestBO(@NotBlank String family, @NotEmpty List<@NotNull @Valid ScriptBO> scripts) {

    public EgonColaDdlManifestBO {
        if (!Set.of("light", "light-open", "service", "service-open", "web", "web-open").contains(family)
                || scripts == null || scripts.isEmpty()) {
            throw new IllegalArgumentException("DDL manifest requires a supported family and scripts");
        }
        scripts = List.copyOf(scripts);
        String previous = "";
        Set<String> paths = new HashSet<>();
        for (ScriptBO script : scripts) {
            if (script.version().compareTo(previous) <= 0 || !paths.add(script.path())) {
                throw new IllegalArgumentException("DDL versions must increase strictly with unique resource paths");
            }
            previous = script.version();
        }
    }

    public record ScriptBO(
            @NotBlank @Pattern(regexp = "[0-9]{8}_[0-9]{3}") String version,
            @NotBlank String path,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String sha256) {

        public ScriptBO {
            if (version == null || !version.matches("[0-9]{8}_[0-9]{3}")
                    || path == null || path.length() > 500 || !path.matches("[a-zA-Z0-9_-]+(?:/[a-zA-Z0-9_-]+)*\\.sql")
                    || sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Invalid DDL version, canonical classpath SQL path or SHA-256");
            }
        }
    }
}
