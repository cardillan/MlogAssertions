package cardillan.mlogassertions.logic;

import mindustry.game.Team;
import mindustry.ctype.Content;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.logic.LVar;

public enum AssertDataType {
    number("number"), string("string"), content("content"), building("building"),
    unit("unit"), team("team"),
    ;

    public static final AssertDataType[] all = values();

    private final String token;

    AssertDataType(String token) {
        this.token = token;
    }

    /** The wire format token; also what the type select button shows. */
    public String display() {
        return token;
    }

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

    public static AssertDataType parse(String token) {
        for (AssertDataType type : all) {
            if (type.token.equals(token)) return type;
        }
        throw new IllegalArgumentException("Invalid asserttype data type: '" + token + "'");
    }
}
