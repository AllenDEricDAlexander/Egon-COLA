package ${package}.infrastructure.teaching.repo.jpa;

import ${package}.infrastructure.teaching.repo.po.SchoolClassUserPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassUserJpaRepository extends JpaRepository<SchoolClassUserPO, String> {
    List<SchoolClassUserPO> findByGradeIdAndSchoolClassId(
            String gradeId,
            String schoolClassId);

    boolean existsByGradeIdAndSchoolClassIdAndUserId(
            String gradeId,
            String schoolClassId,
            String userId);
}
