package top.egon.cola.component.rpc.annotation;

/**
 * What a caller should do once a remote call has exhausted its retries.
 *
 * <p>The constant names {@code FAIL_OPEN}, {@code FAIL_CLOSED} and
 * {@code LOCAL_FALLBACK} are taken verbatim from
 * {@code top.egon.cola.component.accessguard.annotation.FailStrategy}. The two
 * enums describe the same decision in different subsystems, so sharing the
 * vocabulary keeps a single mental model for the reader; two spellings of the
 * same idea would force everyone to learn a mapping that carries no information.
 *
 * <p>Deliberate difference: access-guard spells its sentinel
 * {@code GLOBAL_DEFAULT}, this enum spells it {@link #INHERIT}. Access-guard
 * resolves in one hop to a single global policy, whereas an RPC declaration sits
 * in a multi-level chain (method, reference, service, consumer default), so
 * "inherit from the next layer up" describes the real semantics and
 * "use the global default" would be wrong. The enums are intentionally not
 * shared as one type: rpc-starter must not depend on access-guard.
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
