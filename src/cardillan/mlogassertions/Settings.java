package cardillan.mlogassertions;

import arc.Core;
import cardillan.mlogassertions.ui.Assertions;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.logic.LExecutor;

import java.lang.reflect.Modifier;

public class Settings {
    static final int[] UPDATES_PER_TICK = { 1, 5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000 };

    public static void init() {
        Core.settings.defaults(
                Constants.maxInstructions, LExecutor.maxInstructions,
                Constants.minWaitTimeUpdate, Assertions.minWaitTimeUpdate,
                Constants.processorUpdatesPerTick, 4,
                Constants.warnEffectFrequency, Assertions.warnEffectFrequency
        );

        Vars.ui.settings.addCategory("Mlog Assertions", Icon.warningSmall, t -> {
            t.checkPref(Constants.disableBreakpoints, false);
            t.checkPref(Constants.assertsAreBreakpoints, false);
            t.checkPref(Constants.freeCameraOnBreakpoint, true);

            if (canSetInstructions()) {
                t.sliderPref(Constants.maxInstructions, 1000, 1000, 2000, 100, i -> {
                    LExecutor.maxInstructions = i;
                    return Integer.toString(i);
                });
            }

            t.sliderPref(Constants.minWaitTimeUpdate, 1000, 0, 10000, 500, i -> {
                Assertions.minWaitTimeUpdate = i;
                return i == 0 ? "none" : Double.toString(i / 1000.0);
            });

            t.sliderPref(Constants.processorUpdatesPerTick, 4, 0, UPDATES_PER_TICK.length - 1, i -> {
                Assertions.processorUpdatesPerTick = updatesPerTick(i);
                return Integer.toString(Assertions.processorUpdatesPerTick);
            });

            t.sliderPref(Constants.warnEffectFrequency, 0, -5, 60, 5, i -> {
                Assertions.warnEffectFrequency = i;
                return i < 0 ? "never" : i == 0 ? "once" : "every " + i + " sec";
            });
        });

        if (canSetInstructions()) {
            LExecutor.maxInstructions = Core.settings.getInt(Constants.maxInstructions);
        }
        Assertions.minWaitTimeUpdate = Core.settings.getInt(Constants.minWaitTimeUpdate);
        Assertions.processorUpdatesPerTick = updatesPerTick(Core.settings.getInt(Constants.processorUpdatesPerTick, 4));
        Assertions.warnEffectFrequency = Core.settings.getInt(Constants.warnEffectFrequency);
    }

    public static boolean disableBreakpoints() {
        return Core.settings.getBool(Constants.disableBreakpoints, false);
    }

    public static int updatesPerTick(int index) {
        if (index >= 0 && index < UPDATES_PER_TICK.length) return UPDATES_PER_TICK[index];
        for (int i = UPDATES_PER_TICK.length - 1; i >= 0; i--) {
            if (UPDATES_PER_TICK[i] <= index) return UPDATES_PER_TICK[i];
        }
        return UPDATES_PER_TICK[UPDATES_PER_TICK.length - 1];
    }

    public static boolean assertsAreBreakpoints() {
        return Core.settings.getBool(Constants.assertsAreBreakpoints, false);
    }

    public static boolean detachCameraOnBreakpoint() {
        return Core.settings.getBool(Constants.freeCameraOnBreakpoint, true);
    }

    public static boolean canSetInstructions() {
        try {
            return !Modifier.isFinal(LExecutor.class.getField("maxInstructions").getModifiers());
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
