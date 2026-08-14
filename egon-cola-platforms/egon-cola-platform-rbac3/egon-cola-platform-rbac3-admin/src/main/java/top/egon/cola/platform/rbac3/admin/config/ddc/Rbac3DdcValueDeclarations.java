package top.egon.cola.platform.rbac3.admin.config.ddc;

import top.egon.cola.component.ddc.annotation.DdcValue;

/**
 * Declares the RBAC-only configuration catalog reported to DDC.
 */
public final class Rbac3DdcValueDeclarations {

    @DdcValue(value = "${rbac3.maximum-active-roots:16}", refreshable = false)
    private Integer maximumActiveRoots = 16;
}
