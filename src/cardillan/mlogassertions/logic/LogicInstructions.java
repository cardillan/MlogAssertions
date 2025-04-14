package cardillan.mlogassertions.logic;

import cardillan.mlogassertions.Assertions;
import mindustry.gen.Building;
import mindustry.logic.LExecutor;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.LogicBlock;

public class LogicInstructions {

    public static class AssertI implements LExecutor.LInstruction {
        public AssertionType type = AssertionType.any;
        public LVar multiple;
        public LVar min;
        public AssertOp opMin = AssertOp.lessThanEq;
        public LVar value;
        public AssertOp opMax = AssertOp.lessThanEq;
        public LVar max;
        public LVar message;

        public AssertI(AssertionType type, LVar multiple, LVar min, AssertOp opMin, LVar value, AssertOp opMax, LVar max, LVar message) {
            this.type = type;
            this.multiple = multiple;
            this.min = min;
            this.opMin = opMin;
            this.value = value;
            this.opMax = opMax;
            this.max = max;
            this.message = message;
        }

        public AssertI() {
        }

        @Override
        public final void run(LExecutor exec) {
            Building building = exec.thisv.building();

            LVar vaVal = var(exec, value);
            if ((vaVal.isobj ? type.objFunction.get(vaVal.objval) : type.function.get(vaVal.numval))
                    && (type != AssertionType.multiple || (vaVal.numval % var(exec, multiple).numval == 0))
                    && (opMin.function.get(num(exec,min), num(exec,value)))
                    && (opMax.function.get(num(exec,value), num(exec,max)))) {
                Assertions.remove((LogicBlock.LogicBuild) building);
            } else {
                LVar message = var(exec, this.message);
                Assertions.add((LogicBlock.LogicBuild) building, LExecutor.PrintI.toString(message.objval));

                //skip back to self.
                exec.counter.numval--;
                //exec.yield = true;
            }
        }

        // For easier switching between v7 and v8 logic

        private LVar var(LExecutor exec, LVar var) {
            return var;
        }

        private double num(LExecutor exec, LVar var) {
            return var.num();
        }
    }
}
