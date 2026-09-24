package cardillan.mlogassertions.ui;

import cardillan.mlogassertions.Constants;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.MappableContent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Unit;

import static cardillan.mlogassertions.Constants.COLOR_LIMIT;

public abstract class LogicVariableValues implements VariableValues {

    @Override
    public String formatted(int index, boolean hex) {
        if (isObj(index)) {
            Object obj = obj(index);
            if (obj instanceof String str) {
                return str.length() > 40 ? str.substring(0, 40).trim() + "[gold]..." : str;
            } else {
                return obj == null ? "null" :
                       obj instanceof MappableContent content ? content.name :
                       obj instanceof Content ? "[content]" :
                       obj instanceof Building build ? build.block.name + pos(build.x(), build.y()) :
                       obj instanceof Unit unit ? unit.type.name + pos(unit.x(), unit.y()) :
                       obj instanceof Enum<?> e ? e.name() :
                       obj instanceof Team team ? team.name :
                       "[object]";
            }
        } else {
            double num = num(index);
            if (num <= COLOR_LIMIT && num > 0) {
                long color = Double.doubleToLongBits(num) & 0xFFFFFFFFL;
                String str = Integer.toHexString((int) color);
                if (str.length() < 8) str = "0".repeat(8 - str.length()) + str;
                return '%' + str + " [#" + str.substring(0, 6) + "]\ue86b";
            } else if ((long) num == num) {
                return hex ? "0x" + Long.toHexString((long) num).toUpperCase() : Long.toString((long) num);
            } else {
                return hex ? Double.toHexString(num).toLowerCase() : Double.toString(num).toLowerCase();
            }
        }
    }

    public String clipboard(int index, boolean hex) {
        return isObj(index) && obj(index) instanceof String str ? str : formatted(index, hex);
    }

    private String pos(float x, float y) {
        return String.format(" (%.1f,\u00a0%.1f)", x / Vars.tilesize, y / Vars.tilesize);
    }

    @Override
    public ValueType type(int index) {
        if (isLink(index)) {
            return ValueType.link;
        } else if (isObj(index)) {
            Object objval = obj(index);
            return  objval == null ? ValueType.nothing :
                    objval instanceof String ? ValueType.string :
                    objval instanceof Content ? ValueType.content :
                    objval instanceof Building ? ValueType.building :
                    objval instanceof Team ? ValueType.team :
                    objval instanceof Unit ? ValueType.unit :
                    objval instanceof Enum<?> ? ValueType.enumerated :
                    ValueType.unknown;
        } else {
            double num = num(index);
            return  num <= COLOR_LIMIT && num > 0 ? ValueType.color :
                    (long) num == num ? ValueType.integer :
                    ValueType.number;
        }
    }
}
