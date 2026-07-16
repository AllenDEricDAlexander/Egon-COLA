package sample.bytecode;

import top.egon.cola.component.methodextension.annotation.MethodExtension;

public interface MethodExtensionContract {

    @MethodExtension
    String inherited(String value);
}
