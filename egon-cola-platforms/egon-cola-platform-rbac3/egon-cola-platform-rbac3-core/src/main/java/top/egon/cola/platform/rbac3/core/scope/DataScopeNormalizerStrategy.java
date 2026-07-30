package top.egon.cola.platform.rbac3.core.scope;

import java.util.Map;
import java.util.Set;

public interface DataScopeNormalizerStrategy {

    ScopeDimension dimension();

    Map<String, Set<String>> normalize(Set<String> referenceIds);

    enum ScopeDimension {
        TENANT_ALL,
        ORG,
        ORG_TREE,
        DEPT,
        DEPT_TREE,
        USER,
        SELF,
        NONE
    }
}
