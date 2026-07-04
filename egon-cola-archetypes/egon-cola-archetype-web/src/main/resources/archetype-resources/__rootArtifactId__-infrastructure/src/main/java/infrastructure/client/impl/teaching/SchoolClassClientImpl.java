package ${package}.infrastructure.client.impl.teaching;

import ${package}.domain.client.teaching.SchoolClassClient;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Component("schoolClassClientImpl")
@Validated
@RequiredArgsConstructor
public class SchoolClassClientImpl implements SchoolClassClient {
    @Qualifier("schoolClassRepositoryImpl")
    private final SchoolClassRepository schoolClassRepository;

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        return schoolClassRepository.save(schoolClass);
    }

    @Override
    public Optional<SchoolClass> findById(String schoolClassId) {
        return schoolClassRepository.findById(schoolClassId);
    }
}
