package top.egon.cola.component.ddc.configuration.runtime;

/**
 * 在应用需要绕过标准 DDC 配置控制实例身份时，提供稳定的运行时实例标识。
 * Supplies a stable runtime instance identifier when applications need to control identity outside standard DDC configuration.
 */
@FunctionalInterface
public interface DdcInstanceIdProvider {

    /**
     * 返回当前运行时的稳定实例标识。
     * Returns the stable identifier for the current runtime instance.
     *
     * @return 实例标识; instance identifier
     */
    String getInstanceId();
}
