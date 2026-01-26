package cardillan.mlogassertions.logic;

import cardillan.mlogassertions.Assertions;
import mindustry.ctype.Content;
import mindustry.ctype.MappableContent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.logic.ConditionOp;
import mindustry.logic.LExecutor;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.LogicBlock;

public class LogicInstructions {

    public interface Assertinstruction {
    }

    public static class AssertBoundsI implements LExecutor.LInstruction, Assertinstruction {
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
            Building building = exec.thisv.building();

            if ((value.isobj ? type.objFunction.get(value.objval) : type.function.get(value.num()))
                    && (type != AssertionType.multiple || (value.num() % multiple.num() == 0))
                    && (opMin.function.get(min.num(), value.num()))
                    && (opMax.function.get(value.num(), max.num()))) {
                Assertions.reset((LogicBlock.LogicBuild) building);
            } else {
                Assertions.setMessage((LogicBlock.LogicBuild) building, () -> print(message));

                //skip back to self.
                exec.counter.numval--;
                exec.yield = true;
            }
        }
    }

    public static class AssertEqualsI implements LExecutor.LInstruction, Assertinstruction {
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
            Building building = exec.thisv.building();

            if (ConditionOp.strictEqual.test(expected, actual)) {
                Assertions.reset((LogicBlock.LogicBuild) building);
            } else {
                Assertions.setMessage((LogicBlock.LogicBuild) building, () -> "Assertion failed: " + print(message));
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

    public static class AssertPrintsI implements LExecutor.LInstruction, Assertinstruction {
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
            Building building = exec.thisv.building();

            int flushIndex = this.flushIndex.numi();
            if (flushIndex < 0 || flushIndex > exec.textBuffer.length()) {
                Assertions.setMessage((LogicBlock.LogicBuild) building, () -> "Invalid flush index");
                exec.counter.numval--;
                exec.yield = true;
            } else {
                String text = exec.textBuffer.substring(flushIndex);

                if (!text.equals(expected.obj())) {
                    Assertions.setMessage((LogicBlock.LogicBuild) building,
                            () -> "Assertion failed: " + print(message));
                    exec.counter.numval--;
                    exec.yield = true;
                } else {
                    exec.textBuffer.setLength(flushIndex);
                    Assertions.reset((LogicBlock.LogicBuild) building);
                }
            }
        }
    }

    public static class ErrorI implements LExecutor.LInstruction, Assertinstruction {
        public LVar[] vars;

        public ErrorI(LVar[] vars) {
            this.vars = vars;
        }

        public ErrorI() {
            vars = new LVar[10];
        }

        @Override
        public final void run(LExecutor exec) {
            Building building = exec.thisv.building();

            Assertions.setMessage((LogicBlock.LogicBuild) building, () -> {
                int used = 0;
                StringBuilder sbr = new StringBuilder(print(vars[0]));
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
                    if ((used & (1 << i)) == 0 && nonNull(vars[i])) sbr.append(' ').append(print(vars[i]));
                }

                return sbr.toString();
            });
            exec.counter.numval--;
            exec.yield = true;
        }

        private boolean nonNull(LVar var) {
            return !var.isobj || var.objval != null;
        }
    }

    private static String print(LVar value) {
        if (value.isobj) {
            return toString(value.objval);
        } else {
            if (Math.abs(value.numval - Math.round(value.numval)) < 0.00001) {
                return String.valueOf(Math.round(value.numval));
            } else {
                return String.valueOf(value.numval);
            }
        }
    }

    private static String toString(Object obj) {
        return obj == null ? "null" :
            obj instanceof String s ? s :
            obj instanceof MappableContent content ? content.name :
            obj instanceof Content ? "[content]" :
            obj instanceof Building build ? build.block.name :
            obj instanceof Unit unit ? unit.type.name :
            obj instanceof Enum<?> e ? e.name() :
            obj instanceof Team team ? team.name :
            "[object]";
    }
}
