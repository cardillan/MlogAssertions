package cardillan.mlogassertions.logic;

import mindustry.game.Team;
import mindustry.ctype.Content;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.logic.LVar;

public enum AssertDataType {
    number, string, content, building, unit, team;

    public static final AssertDataType[] all = values();

    public boolean matches(LVar var) {
        if (this == number) return !var.isobj;
        if (!var.isobj) return false;
        Object o = var.objval;
        if (o instanceof String) return this == string;
        if (o instanceof Content) return this == content;
        if (o instanceof Building) return this == building;
        if (o instanceof Unit) return this == unit;
        if (o instanceof Team) return this == team;
        return false;
    }

    /** The classification a failure message shows for the actual value, using the same
     * taxonomy as the game's own variable panel. */
    public static String actualType(LVar var) {
        if (!var.isobj) return "number";
        if (var.objval == null) return "null";
        if (var.objval instanceof String) return "string";
        if (var.objval instanceof Content) return "content";
        if (var.objval instanceof Building) return "building";
        if (var.objval instanceof Unit) return "unit";
        if (var.objval instanceof Team) return "team";
        if (var.objval instanceof Enum<?>) return "enum";
        return "unknown";
    }
}
