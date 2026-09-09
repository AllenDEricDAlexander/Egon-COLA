package top.egon.cola.platform.tianquan.jianshen.admin.config.tianshu;

import top.egon.cola.component.tianshu.annotation.DdcValue;

/**
 * Declares the RBAC-only configuration catalog reported to Tianshu.
 */
public final class Rbac3DdcValueDeclarations {

    @DdcValue(value = "${tianquan-jianshen.maximum-active-roots:16}", refreshable = false)
    private Integer maximumActiveRoots = 16;
}
