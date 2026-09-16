package cardillan.mlogassertions.logic;

import mindustry.ai.UnitCommand;
import mindustry.ai.UnitStance;
import mindustry.entities.bullet.BulletType;
import mindustry.game.Team;
import mindustry.ctype.Content;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.logic.*;

import java.util.Arrays;
import java.util.Comparator;

public enum AssertDataType {
    // Basic types
    number(0),
    string(0, String.class),

    // General and specific contents
    content(0, Content.class),
    item(1, Item.class),
    block(1, Block.class),
    bulletType(1, BulletType.class),
    liquid(1, Liquid.class),
    statusEffect(1, StatusEffect.class),
    unitType(1, UnitType.class),
    weather(1, Weather.class),
    team(1, Team.class),
    unitCommand(1, UnitCommand.class),
    unitStance(1, UnitStance.class),

    // Any unit
    unit(0, Unit.class),

    // General and specific buildings
    building(0, Building.class),
    processor(1, LogicBlock.LogicBuild.class),
    memory(1, MemoryBlock.MemoryBuild.class),
    message(1, MessageBlock.MessageBuild.class),
    display(1, LogicDisplay.LogicDisplayBuild.class),
    canvas(1, CanvasBlock.CanvasBuild.class),

    // Other special values
    property(0, LAccess.class),
    readable(0, LReadable.class),
    writable(0, LWritable.class),
    senseable(0, Senseable.class),
    ;

    private final int level;
    private final Class<?> objectClass;

    AssertDataType(int level, Class<?> objectClass) {
        this.level = level;
        this.objectClass = objectClass;
    }

    AssertDataType(int level) {
        this(level, null);
    }

    public static final AssertDataType[] all = values();
    public static final AssertDataType[] sorted;

    static {
        sorted = values();
        Arrays.sort(sorted, Comparator.comparingInt(a -> -a.level));
    }

    public boolean matches(LVar var) {
        if (this == number) return !var.isobj;
        return var.isobj && objectClass.isInstance(var.objval);
    }

    /** The classification a failure message shows for the actual value, using the same
     * taxonomy as the game's own variable panel. */
    public static String actualType(LVar var) {
        if (!var.isobj) return "number";
        if (var.objval == null) return "null";

        for (AssertDataType type: sorted) {
            if (type.objectClass != null && type.objectClass.isInstance(var.objval)) return type.name();
        }
        return "unknown";
    }
}
