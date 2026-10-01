package cardillan.mlogassertions.logic;

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import cardillan.mlogassertions.data.Snapshot;
import cardillan.mlogassertions.data.SnapshotManager;
import mindustry.game.EventType;
import mindustry.logic.LExecutor;
import mindustry.logic.LExecutor.LInstruction;
import mindustry.logic.LParser;
import mindustry.logic.LStatement;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Instrumentation {
    static boolean initialized = false;
    static Constructor<LParser> parserConstructor;
    static Method parseMethod;
    static final ObjectMap<Class<?>, Field[]> varFields = new ObjectMap<>();

    static final ObjectMap<LogicBuild, Instrumentation> instrumentations = new ObjectMap<>();

    public static void init() {
        try {
            parserConstructor = LParser.class.getDeclaredConstructor(String.class, boolean.class);
            parserConstructor.setAccessible(true);
            parserConstructor.newInstance("", false);       // try it out

            parseMethod = LParser.class.getDeclaredMethod("parse");
            parseMethod.setAccessible(true);

            // Preload all known classes
            Class<?>[] classes = new Class<?>[]{
                    LExecutor.ApplyEffectI.class,
                    LExecutor.ClientDataI.class,
                    LExecutor.ControlI.class,
                    LExecutor.CutsceneI.class,
                    LExecutor.DrawFlushI.class,
                    LExecutor.DrawI.class,
                    LExecutor.EffectI.class,
                    LExecutor.EndI.class,
                    LExecutor.ExplosionI.class,
                    LExecutor.FetchI.class,
                    LExecutor.FlushMessageI.class,
                    LExecutor.FormatI.class,
                    LExecutor.GetBlockI.class,
                    LExecutor.GetFlagI.class,
                    LExecutor.GetLinkI.class,
                    LExecutor.JumpI.class,
                    LExecutor.LocalePrintI.class,
                    LExecutor.LookupI.class,
                    LExecutor.MakeMarkerI.class,
                    LExecutor.NoopI.class,
                    LExecutor.OpI.class,
                    LExecutor.PackColorI.class,
                    LExecutor.PlayMusicI.class,
                    LExecutor.PlaySoundI.class,
                    LExecutor.PrintCharI.class,
                    LExecutor.PrintFlushI.class,
                    LExecutor.PrintI.class,
                    LExecutor.QueryI.class,
                    LExecutor.RadarI.class,
                    LExecutor.ReadI.class,
                    LExecutor.SelectI.class,
                    LExecutor.SenseI.class,
                    LExecutor.SenseWeatherI.class,
                    LExecutor.SetBlockI.class,
                    LExecutor.SetFlagI.class,
                    LExecutor.SetI.class,
                    LExecutor.SetMarkerI.class,
                    LExecutor.SetPropI.class,
                    LExecutor.SetRateI.class,
                    LExecutor.SetRuleI.class,
                    LExecutor.SetWeatherI.class,
                    LExecutor.SpawnBulletI.class,
                    LExecutor.SpawnUnitI.class,
                    LExecutor.SpawnWaveI.class,
                    LExecutor.StopI.class,
                    LExecutor.SyncI.class,
                    LExecutor.UnitBindI.class,
                    LExecutor.UnitControlI.class,
                    LExecutor.UnitLocateI.class,
                    LExecutor.UnpackColorI.class,
                    LExecutor.WaitI.class,
                    LExecutor.WriteI.class
            };

            for (Class<?> clazz : classes) getVarFields(clazz);

            Events.on(EventType.ResetEvent.class, e -> instrumentations.clear());

            Events.on(EventType.BlockBuildEndEvent.class, e -> {
                if (e.breaking && (e.tile.build instanceof LogicBuild b)) {
                    instrumentations.remove(b);
                }
            });

            initialized = true;
        } catch (ReflectiveOperationException e) {
            Log.err("[Mlog Dev Tools] Failed to initialize instrumentation.", e);
        }
    }

    private static Seq<LStatement> parse(String code, boolean privileged) {
        try {
            LParser parser = parserConstructor.newInstance(code, privileged);
            return (Seq<LStatement>) parseMethod.invoke(parser);
        } catch (ReflectiveOperationException e) {
            Log.err("[Mlog Dev Tools] Failed to parse code", e);
            return new Seq<>();
        }
    }

    private static Field[] getVarFields(Class<?> clazz) {
        Field[] fields = varFields.get(clazz);
        if (fields != null) return fields;

        fields = new Field[clazz.getDeclaredFields().length];
        int count = 0;

        for (Field field : clazz.getDeclaredFields()) {
            if (LVar.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                fields[count++] = field;
            }
        }

        fields = Arrays.copyOf(fields, count);
        varFields.put(clazz, fields);

        String[] names = new String[fields.length];
        for (int i = 0; i < fields.length; i++) names[i] = fields[i].getName();
        Log.info("[Mlog Dev Tools] " + clazz.getSimpleName() + Arrays.toString(names));

        return fields;
    }

    static LVar[] getVars(LInstruction instruction, int reservedSpace) {
        if (instruction instanceof LogicInstructions.DevToolsInstruction ix) return ix.vars();

        try {
            Field[] fields = getVarFields(instruction.getClass());
            if (fields == null) return new LVar[0];

            LVar[] result = new LVar[reservedSpace + fields.length];
            for (int i = 0; i < fields.length; i++) {
                result[i + reservedSpace] = (LVar) fields[i].get(instruction);
            }
            return result;
        } catch (InaccessibleObjectException | SecurityException | IllegalAccessException e) {
            return new LVar[0];
        }
    }

    private static StringBuilder sbr = new StringBuilder();
    private static String printInstruction(LStatement statement) {
        sbr.setLength(0);
        statement.write(sbr);
        return sbr.toString();
    }

    public static void startInstructionSnapshots(LExecutor executor, int steps, Snapshot master) {
        if (!initialized || executor.instructions.length == 0) return;

        Instrumentation instrumentation = instrumentations.get(executor.build);
        if (instrumentation == null || instrumentation.executor != executor) {
            instrumentation = new Instrumentation(executor);
            instrumentations.put(executor.build, instrumentation);
        }

        // Include itself
        master.recording().add(master);

        instrumentation.steps = steps;
        instrumentation.master = master;
    }

    private void createSnapshot(int index, LVar[] vars) {
        String text = index >= 0 && index < parsed.size ? parsed.get(index) : "unknown instruction";
        Snapshot snapshot = SnapshotManager.create(executor.build, SnapshotType.recording, index + ": " + text, vars);
        master.recording().add(snapshot);
        SnapshotManager.register(snapshot);
    }

    public final LExecutor executor;
    public final Seq<String> parsed;
    public int steps = 0;
    public Snapshot master;

    public Instrumentation(LExecutor executor) {
        this.executor = executor;
        this.parsed = parse(executor.build.code, executor.privileged).map(Instrumentation::printInstruction);

        for (int i = 0; i < executor.instructions.length; i++) {
            executor.instructions[i] = executor.instructions[i] instanceof LExecutor.WaitI wait
                    ? new InstrumentedWait(wait) : new BasicInstrumentedInstruction(executor.instructions[i]);
        }
    }

    public interface InstrumentedInstruction extends LInstruction {
    }

    public class BasicInstrumentedInstruction implements InstrumentedInstruction {
        LInstruction instruction;
        boolean implicitUnit;
        LVar[] vars = null;

        public BasicInstrumentedInstruction(LInstruction instruction) {
            this.instruction = instruction;
            this.implicitUnit = instruction instanceof LExecutor.UnitBindI
                    || instruction instanceof LExecutor.UnitControlI
                    || instruction instanceof LExecutor.UnitLocateI;
        }

        public LVar[] vars() {
            if (vars == null) vars = getVars(instruction, implicitUnit ? 1 : 0);
            if (implicitUnit && vars != null) vars[0] = executor.unit;
            return vars;
        }

        @Override
        public void run(LExecutor exec) {
            if (steps-- > 0) {
                int index = (int) (exec.counter.numval - 1);
                instruction.run(exec);
                createSnapshot(index, vars());
            } else {
                instruction.run(exec);
            }
        }
    }

    public class InstrumentedWait extends LExecutor.WaitI implements InstrumentedInstruction {
        LExecutor.WaitI instruction;
        LVar[] vars = null;

        public InstrumentedWait(LExecutor.WaitI instruction) {
            this.instruction = instruction;
        }

        public LVar[] vars() {
            if (vars == null) vars = getVars(instruction, 0);
            return vars;
        }

        @Override
        public void run(LExecutor exec) {
            if (steps > 0) {
                int index = (int) (exec.counter.numval - 1);
                instruction.run(exec);

                // Wait ended
                if (instruction.curTime == 0) {
                    steps--;
                    createSnapshot(index, vars());
                }
            } else {
                instruction.run(exec);
            }
        }
    }
}
