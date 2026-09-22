package cardillan.mlogassertions.logic;

import arc.Core;
import arc.graphics.Color;
import arc.util.Log;
import cardillan.mlogassertions.Settings;
import cardillan.mlogassertions.ui.Assertions;
import mindustry.logic.ConditionOp;
import mindustry.logic.LExecutor;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

public class LogicInstructions {

    public interface AssertInstruction {
    }

    public static class AssertBoundsI implements LExecutor.LInstruction, AssertInstruction {
        public AssertionType type = AssertionType.any;
        public LVar multiple;
        public LVar min;
        public AssertOp opMin = AssertOp.lessThanEq;
        public LVar value;
        public AssertOp opMax = AssertOp.lessThanEq;
        public LVar max;
        public LVar message;

        public AssertBoundsI(AssertionType type, LVar multiple, LVar min, AssertOp opMin, LVar value, AssertOp opMax, LVar max, LVar message) {
            this.type = type;
            this.multiple = multiple;
            this.min = min;
            this.opMin = opMin;
            this.value = value;
            this.opMax = opMax;
            this.max = max;
            this.message = message;
        }

        public AssertBoundsI() {
        }

        @Override
        public final void run(LExecutor exec) {
            if ((value.isobj ? type.objFunction.get(value.objval) : type.function.get(value.num()))
                    && (type != AssertionType.multiple || (value.num() % multiple.num() == 0))
                    && (opMin.function.get(min.num(), value.num()))
                    && (opMax.function.get(value.num(), max.num()))) {
                Assertions.reset(exec.build);
            } else {
                assertion("boundsAssertionFailedWithValues", exec, message, min, value, max, opMin.symbol, opMax.symbol);
            }
        }
    }

    public static class AssertEqualsI implements LExecutor.LInstruction, AssertInstruction {
        public LVar expected;
        public LVar actual;
        public LVar message;

        public AssertEqualsI(LVar expected, LVar actual, LVar message) {
            this.expected = expected;
            this.actual = actual;
            this.message = message;
        }

        public AssertEqualsI() {
        }

        @Override
        public final void run(LExecutor exec) {
            if (ConditionOp.strictEqual.test(expected, actual)) {
                Assertions.reset(exec.build);
            } else {
                assertion("assertionFailedWithValues", exec, message, expected, actual);
            }
        }
    }

    public static class AssertFlushI implements LExecutor.LInstruction {
        public LVar flushIndex;

        public AssertFlushI(LVar flushIndex) {
            this.flushIndex = flushIndex;
        }

        public AssertFlushI() {
        }

        @Override
        public final void run(LExecutor exec) {
            flushIndex.setnum(exec.textBuffer.length());
        }
    }

    public static class AssertPrintsI implements LExecutor.LInstruction, AssertInstruction {
        public LVar flushIndex;
        public LVar expected;
        public LVar message;

        public AssertPrintsI(LVar flushIndex, LVar expected, LVar message) {
            this.flushIndex = flushIndex;
            this.expected = expected;
            this.message = message;
        }

        public AssertPrintsI() {
        }

        @Override
        public final void run(LExecutor exec) {
            int flushIndex = this.flushIndex.numi();
            if (flushIndex < 0 || flushIndex > exec.textBuffer.length()) {
                assertion("invalidFlushIndex", exec, "");
            } else {
                String actual = exec.textBuffer.substring(flushIndex);
                if (!actual.equals(expected.obj())) {
                    assertion("assertionFailedWithValues", exec, message, expected, actual);
                } else {
                    exec.textBuffer.setLength(flushIndex);
                    Assertions.reset(exec.build);
                }
            }
        }
    }

    public static class AssertTypeI implements LExecutor.LInstruction, AssertInstruction {
        public AssertionDataType expectedType = AssertionDataType.number;
        public LVar actualValue;
        public LVar message;

        public AssertTypeI(AssertionDataType expectedType, LVar actuallValue, LVar message) {
            this.expectedType = expectedType;
            this.actualValue = actuallValue;
            this.message = message;
        }

        public AssertTypeI() {
        }

        @Override
        public final void run(LExecutor exec) {
            if (expectedType.matches(actualValue)) {
                Assertions.reset(exec.build);
            } else {
                assertion("assertionFailedWithValues", exec, message, expectedType.name(), AssertionDataType.actualType(actualValue));
            }
        }
    }

    public static class BreakpointI implements LExecutor.LInstruction, AssertInstruction {
        public ConditionOp op = ConditionOp.notEqual;
        public LVar value, compare;

        public BreakpointI(ConditionOp op, LVar value, LVar compare) {
            this.op = op;
            this.value = value;
            this.compare = compare;
        }

        public BreakpointI() {
        }

        @Override
        public void run(LExecutor exec) {
            if (op.test(value, compare)) {
                breakpoint(exec.build, Core.bundle.format("breakpoint.message", exec.counter.numval - 1));
            }
        }
    }

    public static class ErrorI implements LExecutor.LInstruction, AssertInstruction {
        public LVar[] vars = new LVar[10];

        public ErrorI(LVar[] vars) {
            this.vars = vars;
        }

        public ErrorI() {
        }

        @Override
        public final void run(LExecutor exec) {
            LogicBuild building = exec.build;

            Assertions.setMessage(building, () -> buildMessage("", true, vars[0], vars));
            exec.counter.numval--;
            exec.yield = true;
        }
    }

    public static class LogI implements LExecutor.LInstruction, AssertInstruction {
        Log.LogLevel level = Log.LogLevel.info;
        public LVar[] vars = new LVar[10];

        public LogI(Log.LogLevel level, LVar[] vars) {
            this.level = level;
            this.vars = vars;
        }

        public LogI() {
        }

        @Override
        public final void run(LExecutor exec) {
            Log.log(level, buildMessage("[MlogAssertions] ", true, vars[0], vars));
        }
    }

    private static void assertion(String bundleKey, LExecutor exec, Object message, Object... arguments) {
        if (Settings.assertsAreBreakpoints()) {
            if (Settings.disableBreakpoints()) return;  // Avoid unnecessary creation of the message
            breakpoint(exec.build, formatAssertionMessage(bundleKey, message, arguments));
        } else {
            exec.counter.numval--;
            exec.yield = true;
            Assertions.setMessage(exec.build, () -> formatAssertionMessage(bundleKey, message, arguments));
        }
    }

    private static void breakpoint(LogicBuild build, String message) {
        if (Settings.disableBreakpoints()) return;
        Assertions.breakpoint(build, message);
    }

    private static String formatAssertionMessage(String bundleKey, Object message, Object[] arguments) {
        if (message instanceof String || message instanceof LVar var && var.isobj && var.objval instanceof String str && !str.isEmpty()) {
            return buildMessage("", false, message, arguments);
        } else {
            return arguments.length == 0
                    ? Core.bundle.get("assertions.assertionFailed")
                    : Core.bundle.format("assertions." + bundleKey, printArgs(arguments));
        }
    }

    private static String buildMessage(String prefix, boolean appendUnused, Object message, Object[] arguments) {
        int used = 0;
        StringBuilder sbr = new StringBuilder(50).append(prefix).append(print(message));
        int pos = sbr.indexOf("{");
        int offset = message == arguments[0] ? 0 : 1;
        while (pos >= 0) {
            if (sbr.charAt(pos + 1) >= '1' && sbr.charAt(pos + 1) <= '9' && sbr.charAt(pos + 2) == '}') {
                int index = sbr.charAt(pos + 1) - '0' - offset;
                String str = index < arguments.length ? print(arguments[index], true)
                        : Core.bundle.get("assertions.invalidPlaceholder");
                sbr.replace(pos, pos + 3, str);
                pos = sbr.indexOf("{", pos + str.length());
                used |= (1 << index);
            } else {
                pos = sbr.indexOf("{", pos + 1);
            }
        }

        if (appendUnused) {
            for (int i = 1; i < arguments.length; i++) {
                LVar var = (LVar) arguments[i];
                if ((used & (1 << i)) == 0 && nonNull(var)) sbr.append(' ').append(print(var, true));
            }
        }

        return sbr.toString();
    }

    private static boolean nonNull(LVar var) {
        return !"null".equals(var.name);
    }

    private static final double COLOR_LIMIT = Color.white.toDoubleBits();

    private static String print(Object message) {
        return print(message, false);
    }

    private static String print(Object message, boolean formatString) {
        return message instanceof LVar lvar ? print(lvar, formatString) : String.valueOf(message);
    }

    private static String print(LVar var, boolean formatString) {
        if (var.isobj) {
            return formatString && var.objval instanceof String str ? '"' + str + '"' : LExecutor.PrintI.toString(var.objval);
        } else if (var.numval <= COLOR_LIMIT && var.numval > 0) {
            long color = Double.doubleToLongBits(var.numval) & 0xFFFFFFFFL;
            return '%' + Integer.toHexString((int) color);
        } else if ((long) var.numval == var.numval) {
            return String.valueOf((long) var.numval);
        } else {
            return String.valueOf(var.numval);
        }
    }

    private static Object[] printArgs(Object[] args) {
        Object[] printed = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            printed[i] = print(args[i]);
        }
        return printed;
    }
}
