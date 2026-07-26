package top.egon.cola.component.common.structure.tree;

/**
 * Options for tree building.
 */
public class TreeOptions {

    private boolean keepOrphansAsRoots = true;

    private boolean failOnCycle;

    public boolean isKeepOrphansAsRoots() {
        return keepOrphansAsRoots;
    }

    public void setKeepOrphansAsRoots(boolean keepOrphansAsRoots) {
        this.keepOrphansAsRoots = keepOrphansAsRoots;
    }

    public boolean isFailOnCycle() {
        return failOnCycle;
    }

    /**
     * When enabled, a parent cycle raises {@link IllegalArgumentException} instead of being broken
     * by promoting the closing node to a root.
     */
    public void setFailOnCycle(boolean failOnCycle) {
        this.failOnCycle = failOnCycle;
    }
}
