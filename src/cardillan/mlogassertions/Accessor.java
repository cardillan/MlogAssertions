package cardillan.mlogassertions;

import arc.util.Log;

import java.lang.reflect.Field;

public class Accessor {
    private static final Accessor instance = new Accessor();

    Object target;

    private Accessor() {
    }

    public static Accessor from(Object target) {
        instance.target = target;
        return instance;
    }

    public Accessor access(String name) throws AccessException {
        Object newTarget;
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            newTarget = field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AccessException("Cannot access field " + target.getClass().getSimpleName() + "." + name, e);
        }
        target = newTarget;
        return this;
    }

    public <T> T get(Class<T> type) throws AccessException {
        try {
            return type.cast(target);
        } catch (ClassCastException e) {
            throw new AccessException("Expected " + type.getSimpleName() + ", got " + target.getClass().getSimpleName(), e);
        }
    }

    public static class AccessException extends Exception {
        public AccessException(String message, Throwable e) {
            super(message, e);
        }
    }
}
