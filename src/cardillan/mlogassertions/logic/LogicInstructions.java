package cardillan.mlogassertions.logic;

import arc.Core;
import arc.graphics.Color;
import arc.util.Log;
import cardillan.mlogassertions.Settings;
import cardillan.mlogassertions.ui.Assertions;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.logic.ConditionOp;
import mindustry.logic.LExecutor;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

import static arc.Core.settings;

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
            LogicBuild build = exec.build;

            if ((value.isobj ? type.objFunction.get(value.objval) : type.function.get(value.num()))
                    && (type != AssertionType.multiple || (value.num() % multiple.num() == 0))
                    && (opMin.function.get(min.num(), value.num()))
                    && (opMax.function.get(value.num(), max.num()))) {
                Assertions.reset(build);
            } else {
                assertion(build, message);

                //skip back to self.
                exec.counter.numval--;
                exec.yield = true;
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
            LogicBuild build = exec.build;

            if (ConditionOp.strictEqual.test(expected, actual)) {
                Assertions.reset(build);
            } else {
                assertion(build, message);
                exec.counter.numval--;
                exec.yield = true;
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
            LogicBuild building = exec.build;

            int flushIndex = this.flushIndex.numi();
            if (flushIndex < 0 || flushIndex > exec.textBuffer.length()) {
                assertion(building, Core.bundle.get("assertions.invalidFlushIndex"));
                exec.counter.numval--;
                exec.yield = true;
            } else {
                String text = exec.textBuffer.substring(flushIndex);

                if (!text.equals(expected.obj())) {
                    assertion(building, message);
                    exec.counter.numval--;
                    exec.yield = true;
                } else {
                    exec.textBuffer.setLength(flushIndex);
                    Assertions.reset(building);
                }
            }
        }
    }

    public static class BreakpointI implements LExecutor.LInstruction, AssertInstruction {
        public ConditionOp op = ConditionOp.notEqual;
        public LVar value, compare;

        public BreakpointI(ConditionOp op, LVar value, LVar compare){
            this.op = op;
            this.value = value;
            this.compare = compare;
        }

        public BreakpointI(){
        }

        @Override
        public void run(LExecutor exec){
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

            Assertions.setMessage(building, () -> buildMessage("", vars));
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
            Log.log(level, buildMessage("[MlogAssertions] ", vars));
        }
    }

    private static void assertion(LogicBuild build, Object message) {
        if (Settings.assertsAreBreakpoints()) {
            if (Settings.disableBreakpoints()) return;  // Avoid unnecessary creation of the message
            breakpoint(build, Core.bundle.format("assertions.assertionFailed", print(message)));
        } else {
            Assertions.setMessage(build, () -> Core.bundle.format("assertions.assertionFailed", print(message)));
        }
    }

    private static void breakpoint(LogicBuild build, String message) {
        if (Settings.disableBreakpoints()) return;

        boolean restoreCamera = false;
        if (Settings.freeCameraOnBreakpoint()) {
            restoreCamera = !settings.getBool("detach-camera", false);
            settings.put("detach-camera", true);
        }
        Core.camera.position.set(build.getX(), build.getY());

        Vars.state.set(GameState.State.paused);
        Vars.world.tiles.eachTile(tile -> {
            if (tile.build instanceof LogicBuild logicBuild) {
                logicBuild.accumulator = 0f;
            }
        });

        Assertions.setBreakpointProc(build, message, restoreCamera);
    }

    private static String buildMessage(String prefix, LVar[] vars) {
        int used = 0;
        StringBuilder sbr = prefix.isEmpty() ? new StringBuilder(print(vars[0])) : new StringBuilder(prefix).append(print(vars[0]));
        int pos = sbr.indexOf("[[");
        while (pos >= 0) {
            if (sbr.charAt(pos + 2) >= '1' && sbr.charAt(pos + 2) <= '9' && sbr.charAt(pos + 3) == ']') {
                int index = sbr.charAt(pos + 2) - '0';
                String str = print(vars[index]);
                sbr.replace(pos, pos + 4, str);
                pos = sbr.indexOf("[[", pos + str.length());
                used |= (1 << index);
            } else {
                pos = sbr.indexOf("[[", pos + 1);
            }
        }

        for (int i = 1; i < vars.length; i++) {
            if ((used & (1 << i)) == 0 && nonNull(vars[i])) sbr.append(' ').append(print(vars[i], true));
        }

        return sbr.toString();
    }

    private static boolean nonNull(LVar var) {
        return !"null".equals(var.name);
    }

    private static final double COLOR_LIMIT = Color.white.toDoubleBits();

    private static String print(Object message) {
        return message instanceof LVar lvar ? print(lvar, false) : String.valueOf(message);
    }

    private static String print(LVar value, boolean formatString) {
        if (value.isobj) {
            return formatString && value.objval instanceof String str ? '"' + str + '"' : LExecutor.PrintI.toString(value.objval);
        } else if (value.numval <= COLOR_LIMIT && value.numval > 0) {
            long color = Double.doubleToLongBits(value.numval) & 0xFFFFFFFFL;
            return '%' + Integer.toHexString((int) color);
        } else {
            return String.valueOf(value.numval);
        }
    }
}
