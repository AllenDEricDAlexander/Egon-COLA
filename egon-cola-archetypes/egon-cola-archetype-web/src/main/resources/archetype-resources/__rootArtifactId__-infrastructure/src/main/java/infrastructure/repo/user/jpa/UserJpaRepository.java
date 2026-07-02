package ${package}.infrastructure.repo.user.jpa;

import ${package}.infrastructure.repo.user.po.UserPo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserPo, String> {
    boolean existsByEmail(String email);
}
