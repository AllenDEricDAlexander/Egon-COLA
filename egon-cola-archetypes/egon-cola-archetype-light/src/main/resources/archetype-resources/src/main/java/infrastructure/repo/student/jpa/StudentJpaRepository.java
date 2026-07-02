package ${package}.infrastructure.repo.student.jpa;

import ${package}.infrastructure.repo.student.po.StudentPo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentJpaRepository extends JpaRepository<StudentPo, String> {
    boolean existsByEmail(String email);
}
