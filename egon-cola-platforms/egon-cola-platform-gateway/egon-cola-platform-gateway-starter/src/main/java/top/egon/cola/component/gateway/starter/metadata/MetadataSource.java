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
 *
 * <p>表示解析出的元数据值来自哪个声明层级，并按从具体到通用的顺序定义优先级。
 */
public enum MetadataSource {

    /** Declared on the method itself. 直接声明在方法上的值。 */
    METHOD,

    /** Declared on the declaring class or the contract interface. 声明在所属类或契约接口上的值。 */
    CLASS,

    /** Declared on a composed {@code @EgonServiceMeta}, possibly inherited from a supertype. 声明在组合的 {@code @EgonServiceMeta} 上，也可能从父类型继承。 */
    SERVICE_META,

    /** Supplied by application configuration. 由应用配置提供的值。 */
    CONFIGURATION,

    /** No level declared a value; the component default applies. 没有层级声明值，使用组件默认值。 */
    DEFAULT;

    /**
     * Tests whether the value was explicitly declared rather than falling through
     * to the component default.
     *
     * <p>判断该值是否由某个声明层级显式提供，而不是回退到组件默认值。
     *
     * @return {@code true} for every source except {@link #DEFAULT}
     */
    public boolean explicit() {
        return this != DEFAULT;
    }
}
