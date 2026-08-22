package top.egon.cola.platform.idp.admin.oauth.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;

import java.util.Optional;

public interface IdentityClientRepository
        extends JpaRepository<IdentityClientEntity, String> {

    /**
     * 以数据库写锁读取 Client，供 Secret 轮换的 expectedVersion CAS 使用。
     *
     * <p>Loads a Client with a database write lock for Secret-rotation CAS.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @return 被锁定的 Client；locked Client
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select client from IdentityClientEntity client "
            + "where client.clientId = :clientId")
    Optional<IdentityClientEntity> findByClientIdForUpdate(
            @Param("clientId") String clientId
    );
}
