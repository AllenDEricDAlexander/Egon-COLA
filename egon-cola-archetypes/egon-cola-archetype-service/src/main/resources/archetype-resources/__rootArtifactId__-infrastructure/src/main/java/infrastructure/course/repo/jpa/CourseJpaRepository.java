#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.jpa;

import ${package}.infrastructure.course.repo.po.CoursePo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CourseJpaRepository extends JpaRepository<CoursePo, String> {

    boolean existsByCode(String code);

    Optional<CoursePo> findByCode(String code);
}
