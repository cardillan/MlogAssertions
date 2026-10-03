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
    public final int[] executions;
    public final int[] branching;
    public int coverage = 0;
    public int totalSteps = 0;
    public int maxSteps = 0;

    // Snapshotting
    public Snapshot master;
    public int snapshotSteps = 0;

    public Instrumentation(LExecutor executor) {
        this.executor = executor;
        instructions = executor.instructions;
        maxInstructionScale = ((LogicBlock)executor.build.block).maxInstructionScale;
        executions = new int[executor.instructions.length];
        branching = new int[executor.instructions.length];
        colors = new Color[executor.instructions.length];

        for (int i = 0; i < instructions.length; i++) {
            LExecutor.LInstruction instruction = instructions[i];
            colors[i] = InstrumentationEngine.getCategory(instruction).color;
            branching[i] = instruction instanceof LExecutor.JumpI ? 0 : -1;
            instructions[i] = instrument(instruction);
        }

        size = executions.length;

        Seq<LStatement> parsedSeq = InstrumentationEngine.parse(executor.build.code, executor.privileged);
        source = new String[size];
        for (int i = 0; i < size; i++) source[i] = i < parsedSeq.size ? printInstruction(parsedSeq.get(i)) : "unknown instruction";
    }

    public void startProfiling() {
        profiling = true;
    }

    public void stopProfiling() {
        profiling = false;
    }

    public void clearProfilingData() {
        Arrays.fill(executions, 0);
        for (int i = 0; i < branching.length; i++) branching[i] = Math.min(branching[i], 0);
        coverage = 0;
        maxSteps = 0;
        totalSteps = 0;
    }

    private void recordStep(int index, int steps) {
        if (profiling && steps > 0 && index >= 0 && index < executions.length) {
            totalSteps += steps;
            if (executions[index] == 0) coverage++;
            int num = executions[index] += steps;
            if (num > maxSteps) maxSteps = num;
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
            if (vars == null) vars = InstrumentationEngine.getVars(instruction, implicitUnit ? 1 : 0);
            if (implicitUnit && vars != null) vars[0] = executor.unit;
            return vars;
        }

        @Override
        public void run(LExecutor exec) {
            int index = (int) (exec.counter.numval - 1);
            recordStep(index, 1);

            instruction.run(exec);
            if (index != (int) (exec.counter.numval - 1) && branching[index] >= 0) branching[index]++;

            if (snapshotSteps > 0) {
                snapshotSteps--;
                createSnapshot(index, vars());
            }
        }
    }

    private class InstrumentedWait extends LExecutor.WaitI implements InstrumentedInstruction {
        LExecutor.WaitI instruction;
        LVar[] vars = null;
        float lostAccumulator = 0f;

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
            this.curTime = instruction.curTime;

            if (instruction.curTime == 0) {
                // Wait ended: the accumulator is going to be consumed.
                // Zero-length wait doesn't consume the accumulator,
                // but not acknowledging that would make it appear the instruction isn't executed at all
                recordStep(index, 1);

                if (snapshotSteps > 0) {
                    snapshotSteps--;
                    createSnapshot(index, vars());
                }
            } else {
                LogicBlock.LogicBuild build = exec.build;
                float futureAccumulator = build.accumulator + build.edelta() * build.ipt;
                if (futureAccumulator > maxInstructionScale * build.ipt) {
                    lostAccumulator += futureAccumulator - maxInstructionScale * build.ipt;
                    int lostSteps = (int) lostAccumulator;
                    recordStep(index, lostSteps);
                    lostAccumulator -= lostSteps;
                }
            }
        }
    }
}
