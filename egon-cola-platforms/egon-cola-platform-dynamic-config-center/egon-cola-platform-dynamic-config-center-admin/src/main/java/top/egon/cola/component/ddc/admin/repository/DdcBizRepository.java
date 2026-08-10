package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;

import java.util.List;
import java.util.Optional;

public interface DdcBizRepository extends JpaRepository<DdcBizEntity, String> {

    Optional<DdcBizEntity> findByBizCode(String bizCode);

    boolean existsByBizCode(String bizCode);

    boolean existsByBizCodeAndIdNot(String bizCode, String id);

    List<DdcBizEntity> findByBizCodeContainingIgnoreCaseOrBizNameContainingIgnoreCase(
            String bizCode, String bizName);

    @Query("""
            select biz from DdcBizEntity biz
             where (:keyword is null
                    or lower(biz.bizCode) like lower(concat('%', :keyword, '%'))
                    or lower(biz.bizName) like lower(concat('%', :keyword, '%')))
            """)
    Page<DdcBizEntity> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
