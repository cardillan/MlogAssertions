package cardillan.mlogassertions.ui;

public interface VariableValues {
    boolean processor();

    int size();
    String label(int index, boolean hex);
    boolean isObj(int index);
    Object obj(int index);
    double num(int index);

    String textBuffer();

    void clear();
    void setView(boolean sorted, boolean hideTemps);
}
