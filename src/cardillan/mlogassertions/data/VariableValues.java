package cardillan.mlogassertions.data;

import arc.func.Cons;
import arc.func.Cons2;
import mindustry.gen.Building;

public interface VariableValues {
    long timestamp();
    String time();

    Building building();
    String buildingDesc();
    boolean processor();
    boolean valid();

    int size();
    String label(int index, boolean hex);
    String formatted(int index, boolean hex);
    String clipboard(int index, boolean hex);
    ValueType type(int index);

    boolean isObj(int index);
    boolean isLink(int index);
    Object obj(int index);
    double num(int index);

    String textBuffer();

    void clear();
    void setView(boolean sorted, boolean hideTemps, boolean hideLinks);

    void eachObject(Cons<Object> getter);

    /** Stores a number into the given slot. Values of sources which cannot be modified are ignored. */
    default void set(int index, double value) {
    }

    /** Stores an object (a String or null) into the given slot. Values of sources which
     * cannot be modified are ignored. */
    default void set(int index, Object value) {
    }
}
