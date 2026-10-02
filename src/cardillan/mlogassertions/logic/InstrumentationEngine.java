package cardillan.mlogassertions.logic;

import arc.Events;
import arc.func.Cons;
import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import cardillan.mlogassertions.data.Snapshot;
import mindustry.game.EventType;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.logic.MemoryBlock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static arc.Core.executor;
import static mindustry.logic.LStatements.*;

public class InstrumentationEngine {
    static boolean initialized = false;
    static Constructor<LParser> parserConstructor;
    static Method parseMethod;
    static final ObjectMap<Class<?>, Field[]> varFields = new ObjectMap<>();
    static final ObjectMap<Class<?>, LCategory> categories = new ObjectMap<>();

    static final ObjectMap<LogicBuild, Instrumentation> instrumentations = new ObjectMap<>();

    public static void init() {
        try {
            parserConstructor = LParser.class.getDeclaredConstructor(String.class, boolean.class);
            parserConstructor.setAccessible(true);
            parserConstructor.newInstance("", false);       // try it out

            parseMethod = LParser.class.getDeclaredMethod("parse");
            parseMethod.setAccessible(true);

            // Preload all known classes
            registerInstruction(ApplyEffectI.class, new ApplyStatusStatement());
            registerInstruction(ClientDataI.class, new ClientDataStatement());
            registerInstruction(ControlI.class, new ControlStatement());
            registerInstruction(CutsceneI.class, new CutsceneStatement());
            registerInstruction(DrawFlushI.class, new DrawFlushStatement());
            registerInstruction(DrawI.class, new DrawStatement());
            registerInstruction(EffectI.class, new EffectStatement());
            registerInstruction(EndI.class, new EndStatement());
            registerInstruction(ExplosionI.class, new ExplosionStatement());
            registerInstruction(FetchI.class, new FetchStatement());
            registerInstruction(FlushMessageI.class, new FlushMessageStatement());
            registerInstruction(FormatI.class, new FormatStatement());
            registerInstruction(GetBlockI.class, new GetBlockStatement());
            registerInstruction(GetFlagI.class, new GetFlagStatement());
            registerInstruction(GetLinkI.class, new GetLinkStatement());
            registerInstruction(JumpI.class, new JumpStatement());
            registerInstruction(LocalePrintI.class, new LocalePrintStatement());
            registerInstruction(LookupI.class, new LookupStatement());
            registerInstruction(MakeMarkerI.class, new MakeMarkerStatement());
            registerInstruction(NoopI.class, new InvalidStatement());
            registerInstruction(OpI.class, new OperationStatement());
            registerInstruction(PackColorI.class, new PackColorStatement());
            registerInstruction(PlayMusicI.class, new PlayMusicStatement());
            registerInstruction(PlaySoundI.class, new PlaySoundStatement());
            registerInstruction(PrintCharI.class, new PrintCharStatement());
            registerInstruction(PrintFlushI.class, new PrintFlushStatement());
            registerInstruction(PrintI.class, new PrintStatement());
            registerInstruction(QueryI.class, new QueryStatement());
            registerInstruction(RadarI.class, new RadarStatement());
            registerInstruction(ReadI.class, new ReadStatement());
            registerInstruction(SelectI.class, new SelectStatement());
            registerInstruction(SenseI.class, new SensorStatement());
            registerInstruction(SenseWeatherI.class, new WeatherSenseStatement());
            registerInstruction(SetBlockI.class, new SetBlockStatement());
            registerInstruction(SetFlagI.class, new SetFlagStatement());
            registerInstruction(SetI.class, new SetStatement());
            registerInstruction(SetMarkerI.class, new SetMarkerStatement());
            registerInstruction(SetPropI.class, new SetPropStatement());
            registerInstruction(SetRateI.class, new SetRateStatement());
            registerInstruction(SetRuleI.class, new SetRuleStatement());
            registerInstruction(SetWeatherI.class, new WeatherSetStatement());
            registerInstruction(SpawnBulletI.class, new SpawnBulletStatement());
            registerInstruction(SpawnUnitI.class, new SpawnUnitStatement());
            registerInstruction(SpawnWaveI.class, new SpawnWaveStatement());
            registerInstruction(StopI.class, new StopStatement());
            registerInstruction(SyncI.class, new SyncStatement());
            registerInstruction(UnitBindI.class, new UnitBindStatement());
            registerInstruction(UnitControlI.class, new UnitControlStatement());
            registerInstruction(UnitLocateI.class, new UnitLocateStatement());
            registerInstruction(UnpackColorI.class, new UnpackColorStatement());
            registerInstruction(WaitI.class, new WaitStatement());
            registerInstruction(WriteI.class, new WriteStatement());

            Events.on(EventType.ResetEvent.class, e -> instrumentations.clear());

            Events.on(EventType.BlockBuildEndEvent.class, e -> {
                if (e.breaking && (e.tile.build instanceof LogicBuild b)) {
                    instrumentations.remove(b);
                }
            });

            initialized = true;

            Events.on(EventType.ResetEvent.class, e -> {
                instrumentations.clear();
            });

            Events.on(EventType.BlockBuildEndEvent.class, e -> {
                if (e.breaking && (e.tile.build instanceof LogicBuild build)) {
                    instrumentations.remove(build);
                }
            });

            Events.on(EventType.ConfigEvent.class, e -> {
                if (e.tile instanceof LogicBuild build) {
                    Instrumentation instrumentation = instrumentations.remove(build);
                    if (instrumentation != null && instrumentation.profiling) {
                        startProfiling(build);
                    }
                }
            });
        } catch (ReflectiveOperationException e) {
            Log.err("[Mlog Dev Tools] Failed to initialize instrumentation.", e);
        }
    }

    private static void registerInstruction(Class<? extends LInstruction> instructionClass, LStatement statement) {
        getVarFields(instructionClass);
        categories.put(instructionClass, statement.category());
    }

    public static void startInstructionSnapshots(LogicBuild build, int steps, Snapshot master) {
        getInstrumentation(build, true, instrumentation -> {
            instrumentation.snapshotSteps = steps;
            instrumentation.master = master;
        });
    }

    public static Instrumentation getInstrumentation(LogicBuild build) {
        return getInstrumentation(build, false, instrumentation -> { });
    }

    public static Instrumentation startProfiling(LogicBuild build) {
        return getInstrumentation(build, true, Instrumentation::startProfiling);
    }

    public static Instrumentation stopProfiling(LogicBuild build) {
        return getInstrumentation(build, false, Instrumentation::stopProfiling);
    }

    public static Instrumentation clearProfilingData(LogicBuild build) {
        return getInstrumentation(build, false, Instrumentation::clearProfilingData);
    }

    static Seq<LStatement> parse(String code, boolean privileged) {
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

    static LCategory getCategory(LInstruction instruction) {
        return categories.get(instruction.getClass(), LCategory.unknown);
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

    public static Instrumentation getInstrumentation(LogicBuild build, boolean instrument, Cons<Instrumentation> action) {
        if (!initialized || build.executor.instructions.length == 0) return null;

        Instrumentation instrumentation = instrumentations.get(build);
        if (instrumentation == null || instrumentation.instructions != build.executor.instructions) {
            if (!instrument) {
                instrumentations.remove(build);
                return null;
            }
            instrumentation = new Instrumentation(build.executor);
            instrumentations.put(build, instrumentation);
        }

        if (action != null && instrumentation != null) action.get(instrumentation);
        return instrumentation;
    }
}
