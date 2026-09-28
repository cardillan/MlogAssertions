package cardillan.mlogassertions.data;

import arc.Core;

public enum BlockDataType {
    processor   ("variables"),
    memory      ("varsdialog.memory"),
    properties  ("varsdialog.properties"),
    ;

    public final String name;

    BlockDataType(String name) {
        this.name = Core.bundle.get(name);
    }
}
