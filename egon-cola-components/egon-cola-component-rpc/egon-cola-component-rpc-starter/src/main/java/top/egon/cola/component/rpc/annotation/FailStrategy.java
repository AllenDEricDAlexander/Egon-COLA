package top.egon.cola.component.rpc.annotation;

/**
 * What a caller should do once a remote call has exhausted its retries.
 *
 * <p>The constant names {@code FAIL_OPEN}, {@code FAIL_CLOSED} and
 * {@code LOCAL_FALLBACK} align with the Access Guard V2 failure-policy
 * vocabulary. The enums are intentionally not shared as one type because
 * rpc-starter must not depend on access-guard; {@link #INHERIT} remains specific
 * to the RPC declaration hierarchy.
 */
public enum FailStrategy {

    /**
     * No opinion at this declaration site; the enclosing layer decides.
     */
    INHERIT,

    /**
     * Let the caller proceed on failure, treating the remote result as absent.
     * Appropriate when the call enriches a response rather than gating it.
     */
    FAIL_OPEN,

    /**
     * Propagate the failure and abort the caller. The default posture for any
     * call whose result the caller cannot correctly continue without.
     */
    FAIL_CLOSED,

    /**
     * Hand off to a locally declared fallback so the caller sees a degraded but
     * well-formed result instead of an exception.
     */
    LOCAL_FALLBACK
}
