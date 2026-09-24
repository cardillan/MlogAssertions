package cardillan.mlogassertions.ui;

import arc.util.Log;
import mindustry.world.blocks.logic.MemoryBlock.MemoryBuild;

import java.lang.reflect.Field;
import java.util.Arrays;

public class MemoryVars implements VariableValues {
    // Memory block private fields
    private static Field numberField;

    private final String[] decLabels;
    private final String[] hexLabels;
    private final double[] numberMemory;
    private final int length;

    public static void init() {
        try {
            // Memory block private fields
            numberField = MemoryBuild.class.getDeclaredField("memory");
            numberField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            Log.err("[MlogAssertions] Failed to access MemoryBuild data fields", e);
        }
    }

    private static <T> T get(Object instance, Field field, T defaultValue) {
        if (field == null) return defaultValue;

        try {
            //noinspection unchecked
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            Log.err("[MlogAssertions] Failed to access MemoryBuild data fields", e);
            return defaultValue;
        }
    }

    public MemoryVars(MemoryBuild memory) {
        if (numberField == null) {
            numberMemory = new double[0];
        } else {
            numberMemory = get(memory, numberField, new double[0]);
        }

        length = numberMemory.length;

        decLabels = new String[length];
        hexLabels = new String[length];
        for (int i = 0; i < length; i++) {
            decLabels[i] = " " + i + " ";
            hexLabels[i] = " " + Integer.toHexString(i).toUpperCase() + " ";
        }
    }

    @Override
    public boolean processor() {
        return false;
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public String label(int index, boolean hex) {
        return hex ? hexLabels[index] : decLabels[index];
    }

    @Override
    public boolean isObj(int index) {
        return false;
    }

    @Override
    public boolean isLink(int index) {
        return false;
    }

    @Override
    public Object obj(int index) {
        return null;
    }

    @Override
    public double num(int index) {
        return numberMemory[index];
    }

    @Override
    public String textBuffer() {
        return "";
    }

    @Override
    public void clear() {
        if (length > 0) {
            Arrays.fill(numberMemory, 0);
        }
    }

    @Override
    public void setView(boolean sorted, boolean hideTemps, boolean hideLinks) {
        // Do nothing
    }
}
