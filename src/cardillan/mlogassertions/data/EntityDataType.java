package cardillan.mlogassertions.data;

import arc.Core;

public enum EntityDataType {
    processor   (10000f, "variables"),
    memory      (550f,   "varsdialog.memory"),
    entity      (750f,   "varsdialog.properties"),
    ;

    public final float maxColWidth;
    public final String name;

    EntityDataType(float maxColWidth, String name) {
        this.maxColWidth = maxColWidth;
        this.name = Core.bundle.get(name);
    }
}
