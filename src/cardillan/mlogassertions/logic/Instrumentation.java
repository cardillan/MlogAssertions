package cardillan.mlogassertions.logic;

import arc.graphics.Color;
import arc.struct.Seq;
import cardillan.mlogassertions.data.Snapshot;
import cardillan.mlogassertions.data.SnapshotManager;
import mindustry.logic.LExecutor;
import mindustry.logic.LStatement;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.LogicBlock;

import java.util.Arrays;

public class Instrumentation {
    public final LExecutor executor;

    // Profiling: constant data
    public final int maxInstructionScale;
    public final LExecutor.LInstruction[] instructions;
    public final String[] source;
    public final Color[] colors;
    public final int size;

    // Profiling: live data
    public boolean profiling = false;
    public final int[] branching;
    public final int[] steps;
    public final float[] time;
    public int coverage = 0;
    public int maxSteps = 0;
    public int totalSteps = 0;
    public float maxTime = 0;
    public float totalTime = 0;
    public float lostQuota = 0;

    // Snapshotting
    public Snapshot master;
    public int snapshotSteps = 0;

    public Instrumentation(LExecutor executor) {
        this.executor = executor;
        this.size = executor.instructions.length;
        this.instructions = executor.instructions;
        this.maxInstructionScale = ((LogicBlock)executor.build.block).maxInstructionScale;

        this.steps = new int[size];
        this.time = new float[size];
        this.branching = new int[size];
        this.colors = new Color[size];
        this.source = new String[size];

        Seq<LStatement> parsedSeq = InstrumentationEngine.parse(executor.build.code, executor.privileged);

        for (int i = 0; i < instructions.length; i++) {
            LExecutor.LInstruction instruction = instructions[i];
            source[i] = i < parsedSeq.size ? printInstruction(parsedSeq.get(i)) : "unknown instruction";
            colors[i] = InstrumentationEngine.getCategory(instruction).color;
            branching[i] = instruction instanceof LExecutor.JumpI ? 0 : -1;
            instructions[i] = instrument(instruction);
        }
    }

    public void startProfiling() {
        profiling = true;
    }

    public void stopProfiling() {
        profiling = false;
    }

    public void clearProfilingData() {
        Arrays.fill(steps, 0);
        Arrays.fill(time, 0f);
        for (int i = 0; i < branching.length; i++) branching[i] = Math.min(branching[i], 0);
        coverage = 0;
        maxSteps = 0;
        totalSteps = 0;
        maxTime = 0;
        totalTime = 0;
        lostQuota = 0;
    }

    private void recordStep(LExecutor exec, int index) {
        if (profiling && index >= 0 && index < steps.length) {
            int newCounter = (int) (exec.counter.numval);
            if (newCounter != index + 1 && branching[index] >= 0) branching[index]++;

            boolean step = true;
            float curTime = 1f;
            if (exec.yield) {
                // Yielding: account for possibly lost execution quota
                LogicBlock.LogicBuild build = exec.build;
                float futureAccumulator = build.accumulator + build.edelta() * build.ipt;
                float loss = futureAccumulator - maxInstructionScale * build.ipt;
                curTime = Math.max(0, loss);
                lostQuota += curTime;

                // Detect wait 0: the instruction yields, but makes a step
                step = newCounter != index;
            }

            if (step) {
                if (steps[index] == 0) coverage++;

                totalSteps++;
                int updatedSteps = steps[index]++;
                if (updatedSteps > maxSteps) maxSteps = updatedSteps;
            }

            totalTime += curTime;
            float updatedTime = time[index] += curTime;
            if (updatedTime > maxTime) maxTime = updatedTime;
        }
    }

    private void createSnapshot(int index, LVar[] vars) {
        String text = index >= 0 && index < source.length ? source[index] : "unknown instruction";
        Snapshot snapshot = SnapshotManager.create(executor.build, SnapshotType.recording, index + ": " + text, vars);
        master.recording().add(snapshot);
        SnapshotManager.register(snapshot);
    }

    private InstrumentedInstruction instrument(LExecutor.LInstruction instruction) {
        // Repeated instrumentation shouldn't happen, but if it does, we need to handle it gracefully.
        if (instruction instanceof InstrumentedInstruction ix) instruction = ix.instruction();
        return instruction instanceof LExecutor.WaitI wait ? new InstrumentedWait(wait) : new BasicInstrumentedInstruction(instruction);
    }

    private static StringBuilder sbr = new StringBuilder();
    private static String printInstruction(LStatement statement) {
        sbr.setLength(0);
        statement.write(sbr);
        return sbr.toString();
    }

    private interface InstrumentedInstruction extends LExecutor.LInstruction {
        LExecutor.LInstruction instruction();
    }

    private class BasicInstrumentedInstruction implements InstrumentedInstruction {
        LExecutor.LInstruction instruction;
        boolean implicitUnit;
        LVar[] vars = null;

        public BasicInstrumentedInstruction(LExecutor.LInstruction instruction) {
            this.instruction = instruction;
            this.implicitUnit = instruction instanceof LExecutor.UnitBindI
                    || instruction instanceof LExecutor.UnitControlI
                    || instruction instanceof LExecutor.UnitLocateI;
        }

        @Override
        public LExecutor.LInstruction instruction() {
            return instruction;
        }

        public LVar[] vars() {
            if (vars == null) {
                vars = InstrumentationEngine.getVars(instruction, implicitUnit ? 1 : 0);
                if (implicitUnit) vars[0] = executor.unit;
            }
            return vars;
        }

        @Override
        public void run(LExecutor exec) {
            int index = (int) (exec.counter.numval - 1);
            instruction.run(exec);
            recordStep(exec, index);

            if (snapshotSteps > 0) {
                snapshotSteps--;
                createSnapshot(index, vars());
            }
        }
    }

    private class InstrumentedWait extends LExecutor.WaitI implements InstrumentedInstruction {
        LExecutor.WaitI instruction;
        LVar[] vars = null;

        public InstrumentedWait(LExecutor.WaitI instruction) {
            this.instruction = instruction;
            this.value = instruction.value;
            this.curTime = instruction.curTime;
        }

        public LExecutor.WaitI instruction() {
            return instruction;
        }

        public LVar[] vars() {
            if (vars == null) vars = InstrumentationEngine.getVars(instruction, 0);
            return vars;
        }

        @Override
        public void run(LExecutor exec) {
            int index = (int) (exec.counter.numval - 1);
            instruction.run(exec);
            recordStep(exec, index);

            curTime = instruction.curTime;
            if (curTime == 0 && snapshotSteps > 0) {
                snapshotSteps--;
                createSnapshot(index, vars());
            }
        }
    }
}
