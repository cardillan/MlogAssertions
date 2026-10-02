package cardillan.mlogassertions.data;

import arc.graphics.g2d.TextureRegion;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.MappableContent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Posc;
import mindustry.gen.Unit;
import mindustry.logic.Senseable;

import static cardillan.mlogassertions.Constants.COLOR_LIMIT;

public abstract class BaseVariableValues implements VariableValues {
    public final double tick = Vars.state.tick;
    public final Senseable entity;

    public BaseVariableValues(Senseable entity) {
        this.entity = entity;
    }

    @Override
    public Senseable entity() {
        return entity;
    }

    @Override
    public String entityDesc() {
        if (entity instanceof Building b) return b.block.localizedName;
        if (entity instanceof Unit u) return u.type.localizedName;
        return entity.getClass().getSimpleName();
    }

    private String buildingPos;
    @Override
    public String entityPos() {
        if (buildingPos == null) {
            if (entity instanceof Posc p) {
                buildingPos = String.format("%.0f,\u00a0%.0f", p.x() / Vars.tilesize, p.y() / Vars.tilesize);
            } else {
                buildingPos = "";
            }
        }
        return buildingPos;
    }

    private String buildingDescMulti;
    @Override
    public String buildingDescMulti() {
        if (buildingDescMulti == null) {
            if (entity instanceof Posc p) {
                buildingDescMulti = String.format("%s\n[gray](%.0f,\u00a0%.0f)", entityDesc(), p.x() / Vars.tilesize, p.y() / Vars.tilesize);
            } else {
                buildingPos = entityDesc();
            }
        }
        return buildingDescMulti;
    }

    @Override
    public TextureRegion icon() {
        return entity instanceof Building b ? b.block.uiIcon :
                entity instanceof Unit unit ? unit.type.uiIcon :
                        null;
    }

    private String time;
    @Override
    public String time() {
        if (time == null) time = String.format("%,.2f", tick);
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
                       obj instanceof Content c ? "[content]" :
                       obj instanceof Building build ? build.block.name + pos(build.x(), build.y()) + id(build.id) :
                       obj instanceof Unit unit ? unit.type.name + pos(unit.x(), unit.y()) + id(unit.id) :
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
        return String.format(" [gray](%.0f,\u00A0%.0f)", x / Vars.tilesize, y / Vars.tilesize);
    }

    public static String id(int id) {
        return " #" + Integer.toHexString(id);
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
                    objval instanceof Building b ? b.dead() ? ValueType.dead : ValueType.building :
                    objval instanceof Team ? ValueType.team :
                    objval instanceof Unit u ? u.dead() ? ValueType.dead : ValueType.unit :
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
