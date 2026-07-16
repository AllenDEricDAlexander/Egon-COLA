package top.egon.cola.component.bytecode.api.architecture;

public enum DependencyKind {
    EXTENDS,
    IMPLEMENTS,
    FIELD,
    PARAMETER,
    RETURN,
    THROWS,
    SIGNATURE,
    ANNOTATION,
    NEW,
    ARRAY,
    CAST,
    INSTANCEOF,
    FIELD_READ,
    FIELD_WRITE,
    METHOD_CALL,
    CONSTRUCTOR_CALL,
    METHOD_HANDLE,
    INVOKEDYNAMIC,
    LAMBDA_TARGET,
    CONSTANT_DYNAMIC,
    CONSTANT_POOL
}
