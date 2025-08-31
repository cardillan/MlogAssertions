package cardillan.mlogassertions;

import arc.Core;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.logic.LExecutor;

import java.lang.reflect.Modifier;

public class Settings {
    public static final String maxInstructions = "mlogassertions-max-instructions";

    public static void init() {
        if (canSetInstructions()) {
            Core.settings.defaults(
                    maxInstructions, LExecutor.maxInstructions
            );

            Vars.ui.settings.addCategory("Mlog Assertions", Icon.warningSmall, t -> {
                t.sliderPref(maxInstructions, 1000, 1000, 2000, 100, i -> {
                    LExecutor.maxInstructions = i;
                    return Integer.toString(i);
                });
            });

            LExecutor.maxInstructions = Core.settings.getInt(Settings.maxInstructions);
        }
    }

    public static boolean canSetInstructions() {
        try {
            return !Modifier.isFinal(LExecutor.class.getField("maxInstructions").getModifiers());
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
