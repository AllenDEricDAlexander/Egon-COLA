package ${package}.infrastructure.repo.teaching.jpa;

import ${package}.infrastructure.repo.teaching.po.SchoolClassUserPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassUserJpaRepository extends JpaRepository<SchoolClassUserPO, Long> {
    List<SchoolClassUserPO> findByUserId(String userId);

    List<SchoolClassUserPO> findBySchoolClassId(String schoolClassId);

    boolean existsByUserIdAndSchoolClassId(String userId, String schoolClassId);

    boolean existsBySchoolClassIdAndUserId(String schoolClassId, String userId);
}
