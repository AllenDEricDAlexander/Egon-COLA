package top.egon.cola.component.ddc.service;

import java.util.Map;

@FunctionalInterface
public interface DdcInstanceMetadataContributor {

    Map<String, String> metadata();
}
