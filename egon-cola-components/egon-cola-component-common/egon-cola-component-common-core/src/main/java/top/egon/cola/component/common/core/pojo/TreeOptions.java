package top.egon.cola.component.common.core.pojo;

/**
 * Options controlling tree construction.
 */
public class TreeOptions {

    private boolean keepOrphansAsRoots = true;

    private boolean failOnCycle;

    public boolean isKeepOrphansAsRoots() {
        return keepOrphansAsRoots;
    }

    public TreeOptions setKeepOrphansAsRoots(boolean keepOrphansAsRoots) {
        this.keepOrphansAsRoots = keepOrphansAsRoots;
        return this;
    }

    public boolean isFailOnCycle() {
        return failOnCycle;
    }

    public TreeOptions setFailOnCycle(boolean failOnCycle) {
        this.failOnCycle = failOnCycle;
        return this;
    }
}
