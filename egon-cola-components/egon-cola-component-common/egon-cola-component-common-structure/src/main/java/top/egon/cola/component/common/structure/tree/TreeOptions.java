package top.egon.cola.component.common.structure.tree;

/**
 * Options for tree building.
 */
public class TreeOptions {

    private boolean keepOrphansAsRoots = true;

    public boolean isKeepOrphansAsRoots() {
        return keepOrphansAsRoots;
    }

    public void setKeepOrphansAsRoots(boolean keepOrphansAsRoots) {
        this.keepOrphansAsRoots = keepOrphansAsRoots;
    }
}
