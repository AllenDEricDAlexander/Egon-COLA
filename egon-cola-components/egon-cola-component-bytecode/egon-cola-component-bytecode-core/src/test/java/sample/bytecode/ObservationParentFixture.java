package sample.bytecode;

public class ObservationParentFixture {

    public static final IllegalStateException PARENT_FAILURE =
            new IllegalStateException("parent-failure");

    protected ObservationParentFixture(boolean fail) {
        if (fail) {
            throw PARENT_FAILURE;
        }
    }
}
