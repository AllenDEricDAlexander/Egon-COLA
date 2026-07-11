package ${package}.infrastructure.repo.teaching.impl;

import ${package}.domain.entities.teaching.Grade;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.repos.teaching.GradeRepository;
import ${package}.domain.vos.teaching.GradeCode;
import ${package}.infrastructure.repo.teaching.converter.GradePOConverter;
import ${package}.infrastructure.repo.teaching.jpa.GradeJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("gradeRepositoryImpl")
public class GradeRepositoryImpl implements GradeRepository {
    private final GradeJpaRepository gradeJpaRepository;
    private final GradePOConverter converter;

    public GradeRepositoryImpl(GradeJpaRepository gradeJpaRepository, GradePOConverter converter) {
        this.gradeJpaRepository = gradeJpaRepository;
        this.converter = converter;
    }

    @Override public Optional<Grade> findById(String gradeId) {
        return gradeJpaRepository.findById(gradeId).map(converter::toEntity);
    }

    @Override public Optional<Grade> findByCode(GradeCode code) {
        return gradeJpaRepository.findByCode(code.value()).map(converter::toEntity);
    }

    @Override public boolean existsByCode(GradeCode code) {
        return gradeJpaRepository.existsByCode(code.value());
    }

    @Override public Grade save(Grade grade) {
        try {
            return converter.toEntity(gradeJpaRepository.save(converter.toPO(grade)));
        } catch (DataIntegrityViolationException exception) {
            throw new OrganizationPortException(
                OrganizationDomainErrorCode.CONFLICT, "grade persistence conflict", exception);
        }
    }
}
