package cardillan.mlogassertions.data;

import arc.func.Cons;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.MappableContent;
import mindustry.gen.Building;
import mindustry.logic.LAccess;
import mindustry.logic.LVar;
import mindustry.logic.Senseable;
import mindustry.type.Item;
import mindustry.type.Liquid;

public class SensorVars extends BaseVariableValues {
    protected static String[] accessLabels = new String[LAccess.all.length];
    protected static String[] itemLabels = new String[Vars.content.items().size];
    protected static String[] liquidLabels = new String[Vars.content.liquids().size];
    protected static MappableContent[] items = new MappableContent[Vars.content.items().size];
    protected static MappableContent[] liquids = new MappableContent[Vars.content.liquids().size];

    static {
        for (int i = 0; i < accessLabels.length; i++) {
            accessLabels[i] = " @" + LAccess.all[i].name() + " ";
        }

        for (int i = 0; i < itemLabels.length; i++) {
            items[i] = Vars.content.items().get(i);
            itemLabels[i] = " @" + items[i].name + " ";
        }
        for (int i = 0; i < liquidLabels.length; i++) {
            liquids[i] = Vars.content.liquids().get(i);
            liquidLabels[i] = " @" + liquids[i].name + " ";
        }
    }

    public final int length;
    public final Content[] contents;
    public final String[] labels;

    public SensorVars(Building build) {
        super(build);

        length = accessLabels.length
                + (build.block.hasItems ? itemLabels.length : 0)
                + (build.block.hasLiquids ? liquidLabels.length : 0);

        contents = new Content[length - accessLabels.length];
        labels = new String[length];

        System.arraycopy(accessLabels, 0, labels, 0, accessLabels.length);
        int index = 0;
        if (build.block.hasItems) {
            System.arraycopy(items, 0, contents, index, items.length);
            System.arraycopy(itemLabels, 0, labels, accessLabels.length + index, itemLabels.length);
            index += itemLabels.length;
        }
        if (build.block.hasLiquids) {
            System.arraycopy(liquids, 0, contents, index, liquids.length);
            System.arraycopy(liquidLabels, 0, labels, accessLabels.length + index, liquidLabels.length);
        }
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
        return index >= accessLabels.length ? LVar.invalid(build.sense(contents[index - accessLabels.length]))
                : build.senseObject(LAccess.all[index]) != Senseable.noSensed || LVar.invalid(build.sense(LAccess.all[index]));
    }

    @Override
    public boolean isLink(int index) {
        return false;
    }

    @Override
    public Object obj(int index) {
        if (index >= accessLabels.length) {
            return null;
        } else {
            Object result = build.senseObject(LAccess.all[index]);
            return result == Senseable.noSensed ? null : result;
        }
    }

    @Override
    public double num(int index) {
        double value = index >= accessLabels.length ? build.sense(contents[index - accessLabels.length]) : build.sense(LAccess.all[index]);
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
