package top.egon.cola.platform.rbac3.starter.manifest;

import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;

@FunctionalInterface
public interface Rbac3ManifestContributor {
    ResourceManifest contribute();
}
