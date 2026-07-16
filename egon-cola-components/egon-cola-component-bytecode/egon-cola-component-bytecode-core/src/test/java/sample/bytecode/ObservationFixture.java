package sample.bytecode;

import java.util.concurrent.Executor;

public final class ObservationFixture {

    public int integer(int value) { return value; }

    public long longValue(long value) { return value; }

    public double doubleValue(double value) { return value; }

    public float floatValue(float value) { return value; }

    public boolean booleanValue(boolean value) { return value; }

    public char charValue(char value) { return value; }

    public Object reference(Object value) { return value; }

    public void voidValue() { }

    public static String staticValue() { return "static"; }

    public final synchronized String sameClassCall() { return privateValue(); }

    private String privateValue() { return "private"; }

    public int recurse(int value) {
        return value == 0 ? 0 : value + recurse(value - 1);
    }

    public int tryFinally(boolean fail) {
        try {
            if (fail) {
                throw new IllegalArgumentException();
            }
            return 1;
        } finally {
            System.nanoTime();
        }
    }

    public void failure(IllegalStateException failure) { throw failure; }

    public void execute(Executor executor, Runnable task) { executor.execute(task); }
}
