package ${package}.infrastructure.teaching.repo.impl;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.teaching.repos.GradeRepository;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.infrastructure.teaching.repo.converter.GradePOConverter;
import ${package}.infrastructure.teaching.repo.mapper.GradeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("gradeRepositoryImpl")
@RequiredArgsConstructor
public class GradeRepositoryImpl implements GradeRepository {
    private final GradeMapper gradeMapper;
    private final GradePOConverter converter;

    @Override public Optional<Grade> findById(Long gradeId) {
        return Optional.ofNullable(gradeMapper.selectById(gradeId)).map(converter::toEntity);
    }

    @Override public Optional<Grade> findByCode(GradeCode code) {
        return Optional.ofNullable(gradeMapper.selectByCode(code.value()))
                .map(converter::toEntity);
    }

    @Override public boolean existsByCode(GradeCode code) {
        return gradeMapper.countByCode(code.value()) > 0;
    }

    @Override public Grade save(Grade grade) {
        try {
            var po = converter.toPO(grade);
            int affected = gradeMapper.selectById(grade.id()) == null
                    ? gradeMapper.insert(po)
                    : gradeMapper.updateById(po);
            requireAffected(affected, "save grade");
            return converter.toEntity(po);
        } catch (DataIntegrityViolationException exception) {
            throw new OrganizationPortException(
                OrganizationDomainErrorCode.CONFLICT, "grade persistence conflict", exception);
        }
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
