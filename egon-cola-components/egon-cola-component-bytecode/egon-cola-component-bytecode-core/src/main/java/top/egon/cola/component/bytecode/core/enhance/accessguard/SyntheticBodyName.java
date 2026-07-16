package top.egon.cola.component.bytecode.core.enhance.accessguard;

public final class SyntheticBodyName {

    private SyntheticBodyName() {
    }

    public static String from(long methodId) {
        return "egon$guard$" + Long.toUnsignedString(methodId, 16);
    }
}
