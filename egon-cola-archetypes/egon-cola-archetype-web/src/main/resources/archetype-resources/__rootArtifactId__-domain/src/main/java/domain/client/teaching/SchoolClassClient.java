package ${package}.domain.client.teaching;

import ${package}.domain.entities.teaching.SchoolClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public interface SchoolClassClient {
    SchoolClass save(@NotNull SchoolClass schoolClass);

    Optional<SchoolClass> findById(@NotBlank String schoolClassId);
}
