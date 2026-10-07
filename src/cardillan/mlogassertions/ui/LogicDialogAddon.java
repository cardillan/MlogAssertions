package cardillan.mlogassertions.ui;

import arc.Core;
import arc.Events;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import cardillan.mlogassertions.Accessor;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.logic.GlobalVarsDialog;
import mindustry.logic.LExecutor;
import mindustry.logic.LogicDialog;
import mindustry.ui.Displayable;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.logic.LogicBlock;

import java.lang.reflect.Field;

import static mindustry.Vars.net;
import static mindustry.Vars.state;

public class LogicDialogAddon {
    public static int tripleTapSpeed = 500;

    public static Field executorField;
    public static Field wasPausedField;
    public static GlobalVarsDialog globalsDialog;

    static int tapCount = 0;
    static long timeTapped1, timeTapped2;
    static Building lastTappedBuild;

    public static void init() {
        try {
            globalsDialog = Accessor.from(Vars.ui.logic).access("globalsDialog").get(GlobalVarsDialog.class);

            executorField = LogicDialog.class.getDeclaredField("executor");
            executorField.setAccessible(true);

            wasPausedField = BaseDialog.class.getDeclaredField("wasPaused");
            wasPausedField.setAccessible(true);
        } catch (Accessor.AccessException | NoSuchFieldException e) {
            Log.err("[Mlog Dev Tools] Cannot access LogicDialog fields", e);
            return;
        }

        Events.on(EventType.TapEvent.class, e -> {
            if (tripleTapSpeed <= 0 || e.tile.build != null  && !e.tile.build.displayable()) return;

            if (lastTappedBuild != e.tile.build ||  e.tile.build == null) {
                lastTappedBuild = e.tile.build;
                timeTapped1 = timeTapped2 = System.currentTimeMillis();
                tapCount = 1;
            } else if (timeTapped1 >= System.currentTimeMillis() - tripleTapSpeed) {
                if (tapCount >= 2) {
                    Building build = lastTappedBuild;
                    Core.app.post(() -> new VarsDialog(build).show());
                    lastTappedBuild = null;
                } else {
                    tapCount++;
                    timeTapped1 = timeTapped2;
                    timeTapped2 = System.currentTimeMillis();
                }
            } else if (timeTapped2 >= System.currentTimeMillis() - 1000) {
                // The previous tap is still valid
                timeTapped1 = timeTapped2;
                timeTapped2 = System.currentTimeMillis();
                tapCount = 2;
            } else {
                timeTapped1 = timeTapped2 = System.currentTimeMillis();
                tapCount = 1;
            }
        });


        Vars.ui.logic.shown(LogicDialogAddon::setupLogicDialog);

        Events.on(EventType.ResizeEvent.class, event -> {
            if (Vars.ui.logic.isShown() && Core.scene.getDialog() == Vars.ui.logic) {
                setupLogicDialog();
            }
        });
    }

    private static void setupLogicDialog() {
        LogicDialog logicDialog = Vars.ui.logic;
        Table buttons = logicDialog.buttons;

        LExecutor executor;
        boolean wasPaused;
        try {
            executor = (LExecutor) executorField.get(Vars.ui.logic);
            wasPaused = (boolean) wasPausedField.get(Vars.ui.logic);
        } catch (IllegalAccessException e) {
            Log.err("[Mlog Dev Tools] Cannot access LogicDialog fields", e);
            return;
        }

        TextButton[] old = new TextButton[buttons.getCells().size];
        for (int i = 0; i < old.length; i++) {
            old[i] = (TextButton) buttons.getCells().get(i).get();
        }
        for (TextButton button : old) buttons.removeChild(button);
        buttons.clearChildren();

        for (int i = 0; i < old.length; i++) {
            if (i == 2 && Core.graphics.isPortrait()) buttons.row();

            if ("variables".equals(old[i].name)) {

                buttons.button("@variables", Icon.menu, () -> {
                    //in the editor, it should display the global variables only (the button text is different)
                    if (!logicDialog.shouldShowVariables()) {
                        globalsDialog.show();
                        return;
                    }

                    VarsDialog dialog = new VarsDialog(executor.build);

                    dialog.hidden(() -> {
                        if (!wasPaused && !net.active() && !state.isMenu()) {
                            state.set(GameState.State.paused);
                        }
                    });

                    dialog.shown(() -> {
                        if (!wasPaused && !net.active() && !state.isMenu()) {
                            state.set(GameState.State.playing);
                        }
                    });

                    dialog.show();
                }).name("variables").update(b -> {
                    if (logicDialog.shouldShowVariables()) {
                        b.setText("@variables");
                    } else {
                        b.setText("@logic.globals");
                    }
                });
            } else {
                buttons.add(old[i]);
            }
        }
    }
}
