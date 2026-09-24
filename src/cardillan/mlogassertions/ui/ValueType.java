package cardillan.mlogassertions.ui;

import arc.graphics.Color;
import mindustry.graphics.Pal;

public enum ValueType {
    nothing     ("null",     Color.darkGray),
    color       ("color",    Pal.berylShot),
    integer     ("integer",  Pal.logicWorld),
    number      ("number",   Pal.place),
    link        ("link",     Pal.tungstenShot),
    string      ("string",   Pal.ammo),
    content     ("content",  Pal.logicOperations),
    building    ("building", Pal.logicBlocks),
    unit        ("unit",     Pal.logicUnits),
    team        ("team",     Pal.logicControl),
    enumerated  ("enum",     Pal.logicIo),
    unknown     ("unknown",  Color.white),
    ;

    public final String title;
    public final String paddedTitle;
    public final Color shade;
    public final Color darkShade;

    ValueType(String title, Color shade) {
        this.title = title;
        this.paddedTitle = " " + title + " ";
        this.shade = shade;
        this.darkShade = shade.cpy().mul(0.5f);
    }
}
