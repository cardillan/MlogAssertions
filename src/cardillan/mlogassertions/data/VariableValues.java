package cardillan.mlogassertions.data;

import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.Entityc;
import mindustry.logic.Senseable;

public interface VariableValues {
    long timestamp();
    String time();

    EntityDataType dataType();
    Senseable entity();
    String entityDesc();
    String entityPos();
    String buildingDescMulti();
    TextureRegion icon();

    boolean live();
    boolean valid();

    int size();
    String label(int index, boolean hex);
    String formatted(int index, boolean hex, int significantDigits);
    String clipboard(int index, boolean hex);
    ValueType type(int index);

    boolean isObj(int index);
    boolean isLink(int index);
    Object obj(int index);
    double num(int index);

    String textBuffer();

    void clear();
    void setView(boolean sorted, boolean filtered, boolean hideLinks);

    void eachObject(Cons<Object> getter);

    /** Stores a number into the given slot. Values of sources which cannot be modified are ignored. */
    default void set(int index, double value) {
    }

    /** Stores an object (a String or null) into the given slot. Values of sources which
     * cannot be modified are ignored. */
    default void set(int index, Object value) {
    }
}
