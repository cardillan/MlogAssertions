package cardillan.mlogassertions.data;

import arc.func.Cons;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.gen.Building;
import mindustry.logic.LAccess;
import mindustry.logic.LVar;
import mindustry.logic.Senseable;
import mindustry.type.Item;
import mindustry.type.Liquid;

public class SensorVars extends BaseVariableValues {
    protected static Content[] contents = new Content[Vars.content.items().size + Vars.content.liquids().size];
    protected static String[] labels = new String[LAccess.all.length + contents.length];
    protected static int contentOffset;
    protected static int length;

    static {
        length = 0;

        for (int i = 0; i < LAccess.all.length; i++) {
            labels[length++] = " @" + LAccess.all[i].name() + " ";
        }

        contentOffset = length;

        for (Item item : Vars.content.items()) {
            contents[length - contentOffset] = item;
            labels[length++] = " @" + item.name + " ";
        }
        for (Liquid liquid : Vars.content.liquids()) {
            contents[length - contentOffset] = liquid;
            labels[length++] = " @" + liquid.name + " ";
        }
    }

    public final Building build;

    public SensorVars(Building build) {
        this.build = build;
    }

    @Override
    public Building building() {
        return build;
    }

    @Override
    public boolean processor() {
        return true;
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public String label(int index, boolean hex) {
        return labels[index];
    }

    @Override
    public boolean isObj(int index) {
        return index >= contentOffset ? LVar.invalid(build.sense(contents[index - contentOffset]))
                : build.senseObject(LAccess.all[index]) != Senseable.noSensed || LVar.invalid(build.sense(LAccess.all[index]));
    }

    @Override
    public boolean isLink(int index) {
        return false;
    }

    @Override
    public Object obj(int index) {
        if (index >= contentOffset) {
            return null;
        } else {
            Object result = build.senseObject(LAccess.all[index]);
            return result == Senseable.noSensed ? null : result;
        }
    }

    @Override
    public double num(int index) {
        double value = index >= contentOffset ? build.sense(contents[index - contentOffset]) : build.sense(LAccess.all[index]);
        return Double.isNaN(value) ? 0 : value;
    }

    @Override
    public String textBuffer() {
        return "";
    }

    @Override
    public void clear() {
        // Do nothing
    }

    @Override
    public void setView(boolean sorted, boolean hideTemps, boolean hideLinks) {
    }

    @Override
    public void eachObject(Cons<Object> getter) {
        for (int index = 0; index < LAccess.all.length; index++) {
            Object value = obj(index);
            if (value != null) getter.get(value);
        }
    }
}
