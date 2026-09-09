package top.egon.cola.platform.tianquan.jianshen.admin.config.ddc;

import top.egon.cola.component.tianshu.annotation.DdcValue;

/**
 * Declares the RBAC-only configuration catalog reported to DDC.
 */
public final class Rbac3DdcValueDeclarations {

    @DdcValue(value = "${rbac3.maximum-active-roots:16}", refreshable = false)
    private Integer maximumActiveRoots = 16;
}
