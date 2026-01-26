package cardillan.mlogassertions;

import arc.Core;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.logic.LExecutor;

import java.lang.reflect.Modifier;

public class Settings {
    public static final String maxInstructions = "max-instructions";
    public static final String minWaitTimeUpdate = "min-wait-time-update";
    public static final String processorUpdatesPerTick = "processor-updates-per-tick";

    public static void init() {
        Core.settings.defaults(
                maxInstructions, LExecutor.maxInstructions,
                minWaitTimeUpdate, Assertions.minWaitTimeUpdate,
                processorUpdatesPerTick, Assertions.processorUpdatesPerTick
        );

        Vars.ui.settings.addCategory("Mlog Assertions", Icon.warningSmall, t -> {
            if (canSetInstructions()) {
                t.sliderPref(maxInstructions, 1000, 1000, 2000, 100, i -> {
                    LExecutor.maxInstructions = i;
                    return Integer.toString(i);
                });
                t.row();

            }

            t.sliderPref(minWaitTimeUpdate, 1000, 0, 10000, 500, i -> {
                Assertions.minWaitTimeUpdate = i;
                return i == 0 ? "none" : Double.toString(i / 1000.0);
            });
            t.row();

            t.sliderPref(processorUpdatesPerTick, 50, 5, 200, 5, i -> {
                Assertions.processorUpdatesPerTick = i;
                return Integer.toString(i);
            });
            t.row();
        });

        if (canSetInstructions()) {
            LExecutor.maxInstructions = Core.settings.getInt(Settings.maxInstructions);
        }
        Assertions.minWaitTimeUpdate = Core.settings.getInt(Settings.minWaitTimeUpdate);
        Assertions.processorUpdatesPerTick = Core.settings.getInt(Settings.processorUpdatesPerTick);
    }

    public static boolean canSetInstructions() {
        try {
            return !Modifier.isFinal(LExecutor.class.getField("maxInstructions").getModifiers());
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
