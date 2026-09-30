package cardillan.mlogassertions.data;

import cardillan.mlogassertions.ui.VarsDialog;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.MappableContent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;

import static cardillan.mlogassertions.Constants.COLOR_LIMIT;

public abstract class BaseVariableValues implements VariableValues {
    public final long timestamp = (long) (Vars.state.tick / 60.0 * 1000.0);
    public final Building build;

    public BaseVariableValues(Building build) {
        this.build = build;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public Building building() {
        return build;
    }

    @Override
    public String buildingDesc() {
        return build.block.localizedName;
    }

    private String buildingPos;
    @Override
    public String buildingPos() {
        if (buildingPos == null) {
            buildingPos = String.format("%.0f,\u00a0%.0f", build.x() / Vars.tilesize, build.y() / Vars.tilesize);
        }
        return buildingPos;
    }

    private String buildingDescMulti;
    @Override
    public String buildingDescMulti() {
        if (buildingDescMulti == null) {
            buildingDescMulti = String.format("%s\n[gray](%.0f,\u00a0%.0f)", build.block.localizedName, build.x() / Vars.tilesize, build.y() / Vars.tilesize);
        }
        return buildingDescMulti;
    }

    private String time;
    @Override
    public String time() {
        if (time == null) {
            if (timestamp > 86_400_000) {
                int days = (int) (timestamp / 86_400_000);
                long millis = timestamp % 86_400_000;
                time = String.format("%dd %d:%02d:%02d.%03d", days, millis / 3_600_000, millis / 60_000 % 60, millis / 1000 % 60, millis % 1000);
            } else {
                time = String.format("%d:%02d:%02d.%03d", timestamp / 3_600_000, timestamp / 60_000 % 60, timestamp / 1000 % 60, timestamp % 1000);
            }
        }
        return time;
    }

    @Override
    public boolean live() {
        return !(this instanceof Snapshot);
    }

    @Override
    public boolean valid() {
        return true;
    }

    private static final String[] formats = new String[16];
    static {
        for (int i = 0; i < formats.length; i++) formats[i] = "%." + i + "g";
    }

    @Override
    public String formatted(int index, boolean hex, int significantDigits) {
        if (isObj(index)) {
            Object obj = obj(index);
            if (obj instanceof String str) {
                return str;
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
                if (significantDigits >= formats.length) return Double.toString(num).toLowerCase();
                String str = String.format(formats[significantDigits], num);
                if (str.indexOf('e') > 0) return str;
                if (str.indexOf('.') == -1) return str + ".0";  // It's NOT an integer
                int l = str.length() - 1;
                while (l > 0 && str.charAt(l) == '0') l--;
                if (str.charAt(l) == '.') l++;
                return str.substring(0, l + 1);
            }
        }
    }

    public String clipboard(int index, boolean hex) {
        return isObj(index) && obj(index) instanceof String str ? str : formatted(index, hex, 16);
    }

    public static String pos(float x, float y) {
        return String.format("\n[gray](%.0f, %.0f)", x / Vars.tilesize, y / Vars.tilesize);
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
            return  num == 0 ? ValueType.zero :
                    num <= COLOR_LIMIT && num > 0 ? ValueType.color :
                    (long) num == num ? ValueType.integer :
                    ValueType.number;
        }
    }

    protected float[] computeTypeDistribution() {
        int size = size();

        int[] counts = new int[ValueType.values().length];
        for (int i = 0; i < size; i++) {
            counts[type(i).ordinal()]++;
        }

        float[] dist = new float[counts.length];
        for (int i = 0; i < counts.length; i++) {
            dist[i] = (float) ((double) counts[i] / size);
        }
        return dist;
    }
}
