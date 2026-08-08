package top.egon.cola.component.gateway.starter.metadata;

/**
 * Which level of the declaration chain supplied a resolved metadata value.
 *
 * <p>Tracked so the management surface can show <em>where</em> a setting came from. Without it a
 * user editing a timeout cannot tell whether they are looking at something declared on the
 * method, inherited from the service, or merely the component default — and therefore cannot
 * tell which level they need to change.
 *
 * <p>Declared most specific first; {@link #ordinal()} is the precedence order.
 */
public enum MetadataSource {

    /** Declared on the method itself. */
    METHOD,

    /** Declared on the declaring class or the contract interface. */
    CLASS,

    /** Declared on a composed {@code @EgonServiceMeta}, possibly inherited from a supertype. */
    SERVICE_META,

    /** Supplied by application configuration. */
    CONFIGURATION,

    /** No level declared a value; the component default applies. */
    DEFAULT;

    /**
     * Tests whether the value was explicitly declared rather than falling through
     * to the component default.
     *
     * @return {@code true} for every source except {@link #DEFAULT}
     */
    public boolean explicit() {
        return this != DEFAULT;
    }
}
