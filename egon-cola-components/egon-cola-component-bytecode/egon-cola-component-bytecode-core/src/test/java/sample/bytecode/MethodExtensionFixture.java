package sample.bytecode;

import top.egon.cola.component.methodextension.annotation.MethodExtension;

public class MethodExtensionFixture implements MethodExtensionContract {

    private int calls;

    @MethodExtension
    public MethodExtensionFixture() {
    }

    @MethodExtension
    public int primitive(int value) {
        calls++;
        return value + 1;
    }

    @MethodExtension
    public String reference(String value) {
        calls++;
        return "body-" + value;
    }

    @MethodExtension
    public void voidValue() {
        calls++;
    }

    @MethodExtension
    protected String protectedValue() {
        calls++;
        return "protected-body";
    }

    @MethodExtension
    String packageValue() {
        calls++;
        return "package-body";
    }

    @MethodExtension
    private String privateValue() {
        calls++;
        return "private-body";
    }

    @MethodExtension
    public final synchronized String finalSynchronized() {
        calls++;
        return "final-body";
    }

    @Override
    public String inherited(String value) {
        calls++;
        return "inherited-" + value;
    }

    public String callProtected() {
        return protectedValue();
    }

    public String callPackage() {
        return packageValue();
    }

    public String callPrivate() {
        return privateValue();
    }

    public int calls() {
        return calls;
    }

    @MethodExtension
    public static String staticValue() {
        return "static-body";
    }

    @MethodExtension
    public native String nativeValue();
}
