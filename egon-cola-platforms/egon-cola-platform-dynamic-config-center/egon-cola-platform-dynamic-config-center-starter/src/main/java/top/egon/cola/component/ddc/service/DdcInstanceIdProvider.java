package top.egon.cola.component.ddc.service;

/**
 * Supplies a stable runtime instance identifier when applications need to
 * control identity outside standard DDC configuration.
 */
@FunctionalInterface
public interface DdcInstanceIdProvider {

    String getInstanceId();
}
