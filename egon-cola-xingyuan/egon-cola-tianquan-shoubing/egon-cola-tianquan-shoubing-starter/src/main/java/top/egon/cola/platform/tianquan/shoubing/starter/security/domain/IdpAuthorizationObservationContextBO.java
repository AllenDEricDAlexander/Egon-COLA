package top.egon.cola.platform.tianquan.shoubing.starter.security.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.function.BooleanSupplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/** Server-owned delivery state shared by authorization stages of one request. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Slf4j
public class IdpAuthorizationObservationContextBO {
    @Builder.Default @JsonIgnore @Setter(AccessLevel.NONE)
    private volatile boolean active = true;
    @Builder.Default @JsonIgnore @Setter(AccessLevel.NONE)
    private volatile DeliveryStateEnum delivery = DeliveryStateEnum.NEW;
    @JsonIgnore @Setter(AccessLevel.NONE)
    private boolean delivering;

    public enum DeliveryStateEnum { NEW, CONFIRMED, FAILED }

    public synchronized boolean denyOnce(BooleanSupplier directStrictHandoff) {
        if (directStrictHandoff == null || delivery == null) {
            delivery = DeliveryStateEnum.FAILED;
            throw new IllegalArgumentException("OAUTH_AUDIT_HANDOFF_INVALID");
        }
        if (delivery == DeliveryStateEnum.CONFIRMED) { return true; }
        if (!active || delivery == DeliveryStateEnum.FAILED) { return false; }
        if (delivering) {
            delivery = DeliveryStateEnum.FAILED;
            throw new IllegalStateException("OAUTH_AUDIT_REENTRANT_HANDOFF");
        }
        delivering = true;
        try {
            boolean confirmed = directStrictHandoff.getAsBoolean();
            if (delivery == DeliveryStateEnum.FAILED) { return false; }
            delivery = confirmed ? DeliveryStateEnum.CONFIRMED : DeliveryStateEnum.FAILED;
            return confirmed;
        } catch (RuntimeException failure) {
            delivery = DeliveryStateEnum.FAILED;
            log.debug("Authorization delivery unavailable; failureType={}", failure.getClass().getSimpleName());
            return false;
        } catch (Error failure) {
            delivery = DeliveryStateEnum.FAILED;
            throw failure;
        } finally { delivering = false; }
    }

    public synchronized void close() { active = false; }

    @JsonIgnore
    public boolean isConfirmed() { return delivery == DeliveryStateEnum.CONFIRMED; }

    @JsonIgnore
    public boolean isFailed() { return delivery == DeliveryStateEnum.FAILED; }
}
