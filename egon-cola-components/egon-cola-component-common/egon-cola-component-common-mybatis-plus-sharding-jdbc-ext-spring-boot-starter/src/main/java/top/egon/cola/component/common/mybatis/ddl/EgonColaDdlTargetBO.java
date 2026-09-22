package top.egon.cola.component.common.mybatis.ddl;

import com.baomidou.mybatisplus.extension.ddl.IDdl;
import com.baomidou.mybatisplus.extension.ddl.history.IDdlGenerator;
import com.baomidou.mybatisplus.extension.ddl.history.PostgreDdlGenerator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import top.egon.cola.component.common.core.enums.EgonEnum;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Physical initialization target. An ordinary value, deliberately not an IDdl Spring bean.
 */
public record EgonColaDdlTargetBO(@NotBlank @Pattern(regexp = "[a-zA-Z_][a-zA-Z0-9_-]{0,62}") String alias,
                                  @NotBlank @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String schema,
                                  @NotNull RoleEnum role, @JsonIgnore @NotNull DataSource dataSource,
                                  @NotNull @Valid EgonColaDdlManifestBO manifest,
                                  @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String routeFingerprint) implements IDdl {

    public EgonColaDdlTargetBO {
        if (alias == null || !alias.matches("[a-zA-Z_][a-zA-Z0-9_-]{0,62}") || schema == null || !schema.matches("[a-z_][a-z0-9_]{0,62}") || routeFingerprint == null || !routeFingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid DDL target identifier or routing fingerprint");
        }
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(manifest, "manifest");
    }

    @Override
    public void runScript(Consumer<DataSource> consumer) {
        Objects.requireNonNull(consumer, "consumer").accept(dataSource);
    }

    @Override
    @JsonIgnore
    public List<String> getSqlFiles() {
        return manifest.scripts().stream().map(EgonColaDdlManifestBO.ScriptBO::path).toList();
    }

    @Override
    @JsonIgnore
    public IDdlGenerator getDdlGenerator() {
        return PostgreDdlGenerator.newInstanceWithSchema(schema);
    }

    @Override
    public String toString() {
        return "EgonColaDdlTargetBO[alias=" + alias + ", schema=" + schema + ", role=" + role + ", family=" + manifest.family() + ", routeFingerprint=" + routeFingerprint + "]";
    }

    public enum RoleEnum implements EgonEnum {
        MASTER_DATA(0, "MASTER_DATA"),
        SHARD(1, "SHARD");

        private final int code;
        private final String message;

        RoleEnum(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
