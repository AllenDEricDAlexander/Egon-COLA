package ${package}.infrastructure.teaching.repo.jpa;

import ${package}.infrastructure.teaching.repo.po.SchoolClassUserPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassUserJpaRepository extends JpaRepository<SchoolClassUserPO, Long> {
    List<SchoolClassUserPO> findByGradeIdAndSchoolClassId(
            Long gradeId,
            Long schoolClassId);

    long countByGradeIdAndSchoolClassIdAndUserId(
            Long gradeId,
            Long schoolClassId,
            Long userId);
}
