package cardillan.mlogassertions.data;

import arc.graphics.Color;
import mindustry.graphics.Pal;

public enum ValueType {
    nothing     ("null",     true,  Color.darkGray),
    zero        ("integer",  true,  new Color(0x4f4f4fff)),
    color       ("color",    true,  Pal.berylShot),
    integer     ("integer",  true,  Pal.logicWorld),
    number      ("number",   true,  Pal.place),
    link        ("link",     false, Pal.tungstenShot),
    string      ("string",   false, Pal.ammo.cpy().mul(0.75f)),
    content     ("content",  false, Pal.logicOperations),
    building    ("building", false, Pal.logicBlocks),
    unit        ("unit",     false, Pal.logicUnits),
    team        ("team",     false, Pal.logicControl),
    enumerated  ("enum",     false, Pal.logicIo),
    dead        ("dead",     false, Pal.rubble),
    unknown     ("unknown",  false, Color.white),
    ;

    public final String title;
    public final boolean numeric;
    public final String paddedTitle;
    public final Color shade;
    public final Color darkShade;

    ValueType(String title, boolean numeric, Color shade) {
        this.title = title;
        this.numeric = numeric;
        this.paddedTitle = " " + title + " ";
        this.shade = shade;
        this.darkShade = shade.cpy().mul(0.5f);
    }
}
