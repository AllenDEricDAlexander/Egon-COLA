package sample.bytecode;

public final class ConstructorObservationFixture extends ObservationParentFixture {

    public static final IllegalArgumentException BODY_FAILURE =
            new IllegalArgumentException("body-failure");

    private final String value;
    private boolean outerBody;

    public ConstructorObservationFixture() {
        this(false, false);
        outerBody = true;
    }

    public ConstructorObservationFixture(boolean parentFailure, boolean bodyFailure) {
        super(parentFailure);
        value = "direct";
        outerBody = false;
        if (bodyFailure) {
            throw BODY_FAILURE;
        }
    }

    private ConstructorObservationFixture(String value) {
        super(false);
        this.value = value;
        outerBody = false;
    }

    public static ConstructorObservationFixture privateConstructor() {
        return new ConstructorObservationFixture("private");
    }

    public String value() {
        return value;
    }

    public boolean outerBody() {
        return outerBody;
    }
}
