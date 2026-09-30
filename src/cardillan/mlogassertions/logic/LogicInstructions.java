package cardillan.mlogassertions.logic;

import arc.Core;
import arc.util.Log;
import cardillan.mlogassertions.Constants;
import cardillan.mlogassertions.Settings;
import cardillan.mlogassertions.data.Snapshots;
import cardillan.mlogassertions.ui.Assertions;
import mindustry.gen.Building;
import mindustry.logic.ConditionOp;
import mindustry.logic.LExecutor;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

public class LogicInstructions {

    public interface AssertInstruction {
    }

    public static class AssertI implements LExecutor.LInstruction, AssertInstruction {
        public ConditionOp op = ConditionOp.notEqual;
        public LVar value, compare;
        public LVar message;

        public AssertI(ConditionOp op, LVar value, LVar compare, LVar message) {
            this.op = op;
            this.value = value;
            this.compare = compare;
            this.message = message;
        }

        public AssertI() {
        }

        @Override
        public final void run(LExecutor exec) {
            if (op.test(value, compare)) {
                Assertions.reset(exec.build);
            } else {
                assertion(exec, "assertionFailedWithValues", message, value, compare, op.symbol);
            }
        }
    }

    public static class AssertBoundsI implements LExecutor.LInstruction, AssertInstruction {
        public AssertionType type = AssertionType.any;
        public LVar multiple;
        public LVar min;
        public ConditionOp opMin = ConditionOp.lessThanEq;
        public LVar value;
        public ConditionOp opMax = ConditionOp.lessThanEq;
        public LVar max;
        public LVar message;

        public AssertBoundsI(AssertionType type, LVar multiple, LVar min, ConditionOp opMin, LVar value, ConditionOp opMax, LVar max, LVar message) {
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
                    && (test(opMin, min.num(), value.num()))
                    && (test(opMax, value.num(), max.num()))) {
                Assertions.reset(exec.build);
            } else {
                assertion(exec, "boundsAssertionFailedWithValues", message, min, value, max, opMin.symbol, opMax.symbol);
            }
        }

        private boolean test(ConditionOp op, double a, double b) {
            switch (op) {
                case lessThan: return a < b;
                case lessThanEq: return a <= b;
                default: return false;
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
                assertion(exec, "assertionEqualFailedWithValues", message, expected, actual);
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
                assertion(exec, "invalidFlushIndex", "");
            } else {
                String actual = exec.textBuffer.substring(flushIndex);
                exec.textBuffer.setLength(flushIndex);

                if (!actual.equals(expected.obj())) {
                    assertion(exec, "assertionEqualFailedWithValues", message, expected, actual);
                } else {
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
                assertion(exec, "assertionEqualFailedWithValues", message, expectedType.name(), AssertionDataType.actualType(actualValue));
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

            Assertions.setMessage(building, () -> buildMessage(exec, "", true, vars[0], vars));
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
            Log.log(level, buildMessage(exec, "[Mlog Dev Tools] ", true, vars[0], vars));
        }
    }

    public static class SnapshotI implements LExecutor.LInstruction, AssertInstruction {
        public SnapshotType type = SnapshotType.isolated;
        public LVar block, message;

        public SnapshotI(SnapshotType type, LVar block, LVar message) {
            this.type = type;
            this.block = block;
            this.message = message;
        }

        public SnapshotI() {
        }

        @Override
        public void run(LExecutor exec) {
            if (block.obj() instanceof Building building) {
                Snapshots.create(building, type, message.isobj && message.objval == null
                        ? "Mlog " + type + " snapshot"
                        : buildMessage(exec, "", false, message, new Object[0]));
            }
        }

        private String message(LExecutor exec) {
            if (message.obj() instanceof String str) {
                int pos = str.indexOf("{");
                if (pos < 0) return str;

                StringBuilder sbr = new StringBuilder(str);
                while (pos >= 0) {
                    int end = sbr.indexOf("}", pos);
                    if (end < 0) break;
                    LVar var = exec.optionalVar(sbr.substring(pos + 1, end));
                    if (var != null) {
                        String replacement = print(var);
                        sbr.replace(pos, end + 1, replacement);
                        end = pos + replacement.length();
                    }
                    pos = end < sbr.length() ? sbr.indexOf("{", end) : -1;
                }
                return sbr.toString();
            } else {
                return print(message);
            }
        }
    }

    private static void assertion(LExecutor exec, String bundleKey, Object message, Object... arguments) {
        if (Settings.assertsAreBreakpoints()) {
            if (Settings.disableBreakpoints()) return;  // Avoid unnecessary creation of the message
            breakpoint(exec.build, formatAssertionMessage(exec, bundleKey, message, arguments));
        } else {
            Assertions.setMessage(exec.build, () -> formatAssertionMessage(exec, bundleKey, message, arguments));
            if (Settings.snapshotOnAssertion() && Snapshots.maxSnapshots > 0) {
                Snapshots.create(exec.build, SnapshotType.isolated, formatAssertionMessage(exec, bundleKey, message, arguments));
            }
            exec.counter.numval--;
            exec.yield = true;
        }
    }

    private static void breakpoint(LogicBuild build, String message) {
        if (Settings.disableBreakpoints()) return;
        if (Settings.snapshotOnAssertion() && Snapshots.maxSnapshots > 0) {
            Snapshots.create(build, SnapshotType.connected, "Breakpoint snapshot at #" + ((int)build.executor.counter.numval - 1));
        }
        Assertions.breakpoint(build, message);
    }

    private static String formatAssertionMessage(LExecutor exec, String bundleKey, Object message, Object[] arguments) {
        if (message instanceof String || message instanceof LVar var && var.isobj && var.objval instanceof String str && !str.isEmpty()) {
            return buildMessage(exec, "", false, message, arguments);
        } else {
            return arguments.length == 0
                    ? Core.bundle.get("assertions.assertionFailed")
                    : Core.bundle.format("assertions." + bundleKey, printArgs(arguments));
        }
    }

    private static String buildMessage(LExecutor exec, String prefix, boolean appendUnused, Object message, Object[] arguments) {
        int used = 0;
        StringBuilder sbr = new StringBuilder(50).append(prefix).append(print(message));
        // When the first argument is the message, shift the placeholder positions
        int offset = arguments.length > 0 && message == arguments[0] ? 1 : 0;

        int pos = sbr.indexOf("{");
        while (pos >= 0) {
            int end = sbr.indexOf("}", pos);
            if (end < 0) break;

            String str;
            if (end == pos + 2 && sbr.charAt(pos + 1) >= '1' && sbr.charAt(pos + 1) <= '9') {
                int index = sbr.charAt(pos + 1) - '1' + offset;
                str = index < arguments.length ? print(arguments[index], true) : null;
                used |= (1 << index);
            } else {
                LVar var = exec.optionalVar(sbr.substring(pos + 1, end));
                str = var == null ? null : var.name.equals("@counter") ? String.valueOf((int) var.numval - 1) : print(var);
            }
            if (str != null) {
                sbr.replace(pos, end + 1, str);
                end = pos + str.length();
                if (end >= sbr.length()) break;
            }
            pos = sbr.indexOf("{", end);
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

    private static String print(Object message) {
        return print(message, false);
    }

    private static String print(Object message, boolean formatString) {
        return message instanceof LVar lvar ? print(lvar, formatString) : String.valueOf(message);
    }

    private static String print(LVar var, boolean formatString) {
        if (var.isobj) {
            return formatString && var.objval instanceof String str ? '"' + str + '"' : LExecutor.PrintI.toString(var.objval);
        } else if (var.numval <= Constants.COLOR_LIMIT && var.numval > 0) {
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
