package ${package}.infrastructure.repo.teaching.jpa;

import ${package}.infrastructure.repo.teaching.po.SchoolClassUserPo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassUserJpaRepository extends JpaRepository<SchoolClassUserPo, Long> {
    List<SchoolClassUserPo> findByUserId(String userId);

    List<SchoolClassUserPo> findBySchoolClassId(String schoolClassId);

    boolean existsByUserIdAndSchoolClassId(String userId, String schoolClassId);
}
