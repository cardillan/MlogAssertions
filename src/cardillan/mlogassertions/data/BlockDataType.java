package cardillan.mlogassertions.data;

import arc.Core;

public enum BlockDataType {
    processor   (10000f, "variables"),
    memory      (550f,   "varsdialog.memory"),
    properties  (750f,   "varsdialog.properties"),
    ;

    public final float maxColWidth;
    public final String name;

    BlockDataType(float maxColWidth, String name) {
        this.maxColWidth = maxColWidth;
        this.name = Core.bundle.get(name);
    }
}
