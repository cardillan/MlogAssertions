package cardillan.mlogassertions;

import arc.Core;
import arc.func.Intc;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import cardillan.mlogassertions.data.Snapshots;
import cardillan.mlogassertions.ui.Assertions;
import cardillan.mlogassertions.ui.LogicDialogAddon;
import cardillan.mlogassertions.ui.VarsDialog;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.logic.LExecutor;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable.Setting;
import mindustry.ui.dialogs.SettingsMenuDialog.StringProcessor;

import java.lang.reflect.Modifier;

import static arc.Core.settings;

public class Settings {
    static final int[] UPDATES_PER_TICK = { 1, 5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000 };

    public static void init() {
        Core.settings.defaults(
                Constants.maxInstructions, LExecutor.maxInstructions,
                Constants.minWaitTimeUpdate, Assertions.minWaitTimeUpdate,
                Constants.processorUpdatesPerTick, 4,
                Constants.warnEffectFrequency, Assertions.warnEffectFrequency,
                Constants.varsSignificantDigits, VarsDialog.significantDigits,
                Constants.variableUpdateFrequency, VarsDialog.updateFrequency,
                Constants.tripleTapSpeed, LogicDialogAddon.tripleTapSpeed,
                Constants.snapshotLimit, Snapshots.maxSnapshots
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
                return i == 0 ? Core.bundle.get("setting.min-wait-time-update.none") : Double.toString(i / 1000.0);
            });

            t.sliderPref(Constants.processorUpdatesPerTick, 4, 0, UPDATES_PER_TICK.length - 1, i -> {
                Assertions.processorUpdatesPerTick = updatesPerTick(i);
                return Integer.toString(Assertions.processorUpdatesPerTick);
            });

            t.sliderPref(Constants.warnEffectFrequency, 0, -5, 60, 5, i -> {
                Assertions.warnEffectFrequency = i;
                return i < 0 ? Core.bundle.get("setting.warn-effect-frequency.never") :
                        i == 0 ? Core.bundle.get("setting.warn-effect-frequency.once") :
                                Core.bundle.format("setting.warn-effect-frequency.every", i);
            });

            t.sliderPref(Constants.varsSignificantDigits, 7, 3, 16, 1, i -> {
                VarsDialog.significantDigits = i;
                return i == 16 ? Core.bundle.get("setting.vars-significant-digits.full") : Integer.toString(i);
            });

            t.sliderPref(Constants.variableUpdateFrequency, 15, 5, 60, 5, i -> {
                VarsDialog.updateFrequency = i;
                return Core.bundle.format("setting.variable-update-frequency.every", i);
            });

            t.sliderPref(Constants.tripleTapSpeed, 500, 0, 5000, 50, i -> {
                LogicDialogAddon.tripleTapSpeed = i;
                return i == 0 ? Core.bundle.get("setting.triple-tap-speed.disabled") :
                        Core.bundle.format("setting.triple-tap-speed.delay", i);
            });

            steppedPref(t, Constants.snapshotLimit, 20, new int[]{ 0, 5, 10, 20, 50, 100, 200, 500, 1000 },
                    i -> i == 0 ? Core.bundle.get("setting.snapshot-limit.disabled") : Integer.toString(i),
                    i -> Snapshots.maxSnapshots = i);
        });

        Vars.ui.settings.hidden(() -> {
            Snapshots.updateLimit();
        });

        if (canSetInstructions()) {
            LExecutor.maxInstructions = Core.settings.getInt(Constants.maxInstructions);
        }
        Assertions.minWaitTimeUpdate = Core.settings.getInt(Constants.minWaitTimeUpdate);
        Assertions.processorUpdatesPerTick = updatesPerTick(Core.settings.getInt(Constants.processorUpdatesPerTick, 4));
        Assertions.warnEffectFrequency = Core.settings.getInt(Constants.warnEffectFrequency);
        VarsDialog.updateFrequency = Core.settings.getInt(Constants.variableUpdateFrequency);
        Snapshots.maxSnapshots = Core.settings.getInt(Constants.snapshotLimit);
    }

    private static SteppedSliderSetting steppedPref(SettingsTable t, String name, int def, int[] steps, StringProcessor s, Intc changed) {
        SteppedSliderSetting res = new SteppedSliderSetting(name, def, steps, s, changed);
        t.pref(res);
        settings.defaults(name, def);
        t.rebuild();
        return res;
    }

    public static class SteppedSliderSetting extends Setting {
        int def;
        int[] steps;
        StringProcessor sp;
        Intc changed;

        public SteppedSliderSetting(String name, int def, int[] steps, StringProcessor s, Intc changed){
            super(name);
            this.def = def;
            this.steps = steps;
            this.sp = s;
            this.changed = changed;
        }

        @Override
        public void add(SettingsTable table){
            Slider slider = new Slider(0, steps.length - 1, 1, false);

            int val = settings.getInt(name);
            int pos;
            for (pos = 0; pos < steps.length; pos++) {
                if (steps[pos] >= val) break;
            }
            slider.setValue(pos);

            Label value = new Label("", Styles.outlineLabel);
            Table content = new Table();
            content.add(title, Styles.outlineLabel).left().growX().wrap();
            content.add(value).padLeft(10f).right();
            content.margin(3f, 33f, 3f, 33f);
            content.touchable = Touchable.disabled;

            slider.changed(() -> {
                int v = steps[(int)slider.getValue()];
                settings.put(name, v);
                value.setText(sp.get(v));
                if(changed != null) changed.get(v);
            });

            slider.change();

            addDesc(table.stack(slider, content).width(Math.min(Core.graphics.getWidth() / 1.2f / Scl.scl(1f), 500f)).left().padTop(4f).get());
            table.row();
        }
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
