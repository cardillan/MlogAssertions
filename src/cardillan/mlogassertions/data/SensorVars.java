package cardillan.mlogassertions.data;

import arc.func.Cons;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.MappableContent;
import mindustry.gen.Building;
import mindustry.gen.Entityc;
import mindustry.gen.Unit;
import mindustry.logic.LAccess;
import mindustry.logic.LVar;
import mindustry.logic.Senseable;

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

    public SensorVars(Senseable entity) {
        super(entity);

        length = accessLabels.length
                + (hasItems() ? itemLabels.length : 0)
                + (hasLiquids() ? liquidLabels.length : 0);

        contents = new Content[length - accessLabels.length];
        labels = new String[length];

        System.arraycopy(accessLabels, 0, labels, 0, accessLabels.length);
        int index = 0;
        if (hasItems()) {
            System.arraycopy(items, 0, contents, index, items.length);
            System.arraycopy(itemLabels, 0, labels, accessLabels.length + index, itemLabels.length);
            index += itemLabels.length;
        }
        if (hasLiquids()) {
            System.arraycopy(liquids, 0, contents, index, liquids.length);
            System.arraycopy(liquidLabels, 0, labels, accessLabels.length + index, liquidLabels.length);
        }
    }

    private boolean hasItems() {
        return entity instanceof Building && ((Building)entity).block.hasItems || entity instanceof Senseable;
    }

    private boolean hasLiquids() {
        return entity instanceof Building && ((Building)entity).block.hasLiquids;
    }

    @Override
    public EntityDataType dataType() {
        return EntityDataType.entity;
    }

    @Override
    public Senseable entity() {
        return entity;
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
        return index >= accessLabels.length ? LVar.invalid(entity.sense(contents[index - accessLabels.length]))
                : entity.senseObject(LAccess.all[index]) != Senseable.noSensed || LVar.invalid(entity.sense(LAccess.all[index]));
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
            Object result = entity.senseObject(LAccess.all[index]);
            return result == Senseable.noSensed ? null : result;
        }
    }

    @Override
    public double num(int index) {
        double value = index >= accessLabels.length ? entity.sense(contents[index - accessLabels.length]) : entity.sense(LAccess.all[index]);
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
    public void setView(boolean sorted, boolean filtered, boolean hideLinks) {
    }

    @Override
    public void eachObject(Cons<Object> getter) {
        for (int index = 0; index < LAccess.all.length; index++) {
            Object value = obj(index);
            if (value != null) getter.get(value);
        }
    }
}
