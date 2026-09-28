package cardillan.mlogassertions.data;

import arc.func.Cons;
import arc.util.Log;
import mindustry.world.blocks.logic.MemoryBlock.MemoryBuild;

import java.lang.reflect.Field;
import java.util.Arrays;

public class MemoryVars extends BaseVariableValues {
    // Share cell labels across all instances
    public static String[] decLabels = new String[0];
    public static String[] hexLabels = new String[0];

    // Memory block private fields
    private static Object sentinel;
    private static Field objectField;
    private static Field numberField;

    public final Object[] objectMemory;
    public final double[] numberMemory;
    public final int length;

    public static void init() {
        try {
            // Memory block private fields
            Field sentinelField = MemoryBuild.class.getDeclaredField("sentinel");
            sentinelField.setAccessible(true);
            sentinel = sentinelField.get(null);
            objectField = MemoryBuild.class.getDeclaredField("objectMemory");
            objectField.setAccessible(true);
            numberField = MemoryBuild.class.getDeclaredField("numberMemory");
            numberField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            Log.err("[MlogAssertions] Failed to access MemoryBuild data fields", e);
        }
    }

    public MemoryVars(MemoryBuild build) {
        this(build, true);
    }

    protected MemoryVars(MemoryBuild build, boolean live) {
        super(build);

        Object[] objectMemory = get(build, objectField, new Object[0]);
        double[] numberMemory = get(build, numberField, new double[0]);

        length = Math.min(objectMemory.length, numberMemory.length);
        this.objectMemory = live ? objectMemory : Arrays.copyOf(objectMemory, length);
        this.numberMemory = live ? numberMemory : Arrays.copyOf(numberMemory, length);

        if (length > decLabels.length) {
            decLabels = new String[length];
            hexLabels = new String[length];
            for (int i = 0; i < length; i++) {
                decLabels[i] = " " + i + " ";
                hexLabels[i] = " " + Integer.toHexString(i).toUpperCase() + " ";
            }
        }
    }

    private static <T> T get(Object instance, Field field, T defaultValue) {
        if (objectField == null || numberField == null || sentinel == null) return defaultValue;

        try {
            //noinspection unchecked
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            Log.err("[MlogAssertions] Failed to access MemoryBuild data fields", e);
            return defaultValue;
        }
    }

    @Override
    public BlockDataType dataType() {
        return BlockDataType.memory;
    }

    @Override
    public float maxColWidth() {
        return 550f;
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
        return objectMemory[index] != sentinel;
    }

    @Override
    public boolean isLink(int index) {
        return false;
    }

    @Override
    public Object obj(int index) {
        return objectMemory[index];
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
            Arrays.fill(objectMemory, sentinel);
            Arrays.fill(numberMemory, 0);
        }
    }

    @Override
    public void set(int index, double value) {
        if (index < 0 || index >= length || sentinel == null) return;

        objectMemory[index] = sentinel;
        numberMemory[index] = value;
    }

    @Override
    public void set(int index, Object value) {
        if (index < 0 || index >= length || sentinel == null) return;

        objectMemory[index] = value;
    }

    @Override
    public void setView(boolean sorted, boolean filtered, boolean hideLinks) {
        // Do nothing
    }

    @Override
    public void eachObject(Cons<Object> getter) {
        for (int index = 0; index < size(); index++) {
            if (objectMemory[index] != sentinel) getter.get(objectMemory[index]);
        }
    }
}
