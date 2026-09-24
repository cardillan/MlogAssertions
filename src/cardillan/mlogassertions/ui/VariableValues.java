package cardillan.mlogassertions.ui;

public interface VariableValues {
    boolean processor();

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
}
