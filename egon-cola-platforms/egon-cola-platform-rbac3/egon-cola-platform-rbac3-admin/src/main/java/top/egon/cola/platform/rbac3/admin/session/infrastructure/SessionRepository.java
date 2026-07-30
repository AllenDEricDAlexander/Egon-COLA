package top.egon.cola.platform.rbac3.admin.session.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {

    Optional<SessionEntity> findByTenantIdAndSessionId(Long tenantId, Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionEntity s where s.tenantId = :tenantId and s.sessionId = :sessionId")
    Optional<SessionEntity> lockByTenantIdAndSessionId(
            @Param("tenantId") Long tenantId,
            @Param("sessionId") Long sessionId);

    @Modifying
    @Query("""
            update SessionEntity s
               set s.status = :nextStatus,
                   s.sessionVersion = s.sessionVersion + 1,
                   s.revokedAt = :now,
                   s.revokeReason = :reason
             where s.tenantId = :tenantId and s.userId = :userId
               and s.status = :currentStatus
            """)
    int revokeAllActiveForUser(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("reason") String reason,
            @Param("now") Instant now,
            @Param("currentStatus") SessionEntity.Status currentStatus,
            @Param("nextStatus") SessionEntity.Status nextStatus);
}
