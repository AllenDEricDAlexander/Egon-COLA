package top.egon.cola.component.bytecode.core.classfile.fixture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.function.Supplier;

@FixtureMarker(AnnotatedType.class)
public class DependencyFixture extends BaseType implements ContractType {

    private List<SignatureType> values;

    public ReturnType execute(ParameterType parameter) throws CheckedFailure {
        Object allocated = new AllocatedType();
        CastType cast = (CastType) allocated;
        cast.call();
        Runnable lambda = HelperType::call;
        Supplier<MethodReferenceType> reference = MethodReferenceType::new;
        Class<?> literal = ClassLiteralType.class;
        lambda.run();
        reference.get();
        values = List.of(new SignatureType());
        return new ReturnType(parameter, literal, cast);
    }

}

@Retention(RetentionPolicy.RUNTIME)
@interface FixtureMarker {
    Class<?> value();
}

class BaseType {
}

interface ContractType {
}

class SignatureType {
}

class AnnotatedType {
}

class ParameterType {
}

class ReturnType {
    ReturnType(ParameterType parameter, Class<?> literal, CastType cast) {
    }
}

class CheckedFailure extends Exception {
}

class AllocatedType extends CastType {
}

class CastType {
    void call() {
    }
}

class HelperType {
    static void call() {
    }
}

class MethodReferenceType {
}

class ClassLiteralType {
}
