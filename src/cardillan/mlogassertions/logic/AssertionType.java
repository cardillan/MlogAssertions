package cardillan.mlogassertions.logic;

import java.util.Objects;

public enum AssertionType {
    any(num -> true, obj -> true),
    notNull(num -> true, Objects::nonNull),
    decimal(num -> true),
    integer(num -> num == (long) num),
    multiple(num -> num == (long) num),
    ;

    public static final AssertionType[] all = values();
    public final AssertionTypeObjLambda objFunction;
    public final AssertionTypeLambda function;

    AssertionType(AssertionTypeLambda function) {
        this(function, obj -> false);
    }

    AssertionType(AssertionTypeLambda function, AssertionTypeObjLambda objFunction) {
        this.function = function;
        this.objFunction = objFunction;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public interface AssertionTypeObjLambda {
        boolean get(Object obj);
    }

    public interface AssertionTypeLambda {
        boolean get(double val);
    }
}
