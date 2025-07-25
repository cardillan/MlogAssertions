package cardillan.mlogassertions;

import arc.Core;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.logic.LExecutor;

public class Settings {
    public static final String maxInstructions = "mlogassertions-max-instructions";

    public static void init() {
        Core.settings.defaults(
                maxInstructions, LExecutor.maxInstructions
        );

        Vars.ui.settings.addCategory("Mlog Assertions", Icon.warningSmall, t -> {
            t.sliderPref(maxInstructions, 1000, 1000, 2000, 100, i -> {
                LExecutor.maxInstructions = i;
                return Integer.toString(i);
            });
        });
    }
}
