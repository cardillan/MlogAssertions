package cardillan.mlogassertions.data;

import arc.Events;
import arc.util.Log;
import cardillan.mlogassertions.Accessor;
import cardillan.mlogassertions.Constants;
import cardillan.mlogassertions.logic.InstrumentationEngine;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.ui.dialogs.CustomRulesDialog;

import java.lang.reflect.Field;

public class CustomGameRules {
    public static boolean autoProfile = false;

    private static Field customRules;

    public static void init() {
        try {
            customRules = CustomRulesDialog.class.getDeclaredField("rules");
            customRules.setAccessible(true);

            addRules(Accessor.from(Vars.ui.paused).access("rulesDialog").get(CustomRulesDialog.class));
            addRules(Accessor.from(Vars.ui.custom).access("dialog").access("dialog").get(CustomRulesDialog.class));
            addRules(Accessor.from(Vars.ui.editor).access("infoDialog").access("ruleInfo").get(CustomRulesDialog.class));
            addRules(Accessor.from(Vars.ui.editor).access("playtestDialog").access("dialog").get(CustomRulesDialog.class));
        } catch (Accessor.AccessException | NoSuchFieldException e) {
            Log.err("[Mlog Dev Tools] Error installing custom game rules", e);
            return;
        }

        Events.on(EventType.ResetEvent.class, e -> resetRules());
        Events.on(EventType.RulesLoadEvent.class, e -> applyRules(e.rules));
    }

    private static void resetRules() {
        autoProfile = false;
    }

    private static void applyRules(Rules rules) {
        autoProfile = isAutoProfile(rules);

        if (rules.mode() == Gamemode.editor) return;

        if (autoProfile) {
            MapIndex.processors.each(InstrumentationEngine::startProfiling);
            Log.info("[Mlog Dev Tools] Started profiling " + MapIndex.processors.size + " processors");
        }
    }

    public static boolean isAutoProfile(Rules rules) {
        return Boolean.valueOf(rules.tags.get(Constants.rulesAutoProfile, "false"));
    }

    private static void addRules(CustomRulesDialog dialog) {
        dialog.additionalSetup.add(() -> {
            try {
                Rules rules = (Rules) customRules.get(dialog);

                dialog.category("mlogdevtools");

                dialog.check("@" + Constants.rulesAutoProfile,
                        state -> rules.tags.put(Constants.rulesAutoProfile, String.valueOf(state)),
                        () -> isAutoProfile(rules));

                dialog.hidden(() -> applyRules(rules));
            } catch (IllegalAccessException e) {
                Log.err("[Mlog Dev Tools] Failed to access CustomRulesDialog.rules", e);
            }
        });
    }
}
