package cardillan.mlogassertions.ui;

import arc.Core;
import arc.Events;
import arc.scene.style.Drawable;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.logic.GlobalVarsDialog;
import mindustry.logic.LExecutor;
import mindustry.logic.LogicDialog;
import mindustry.ui.dialogs.BaseDialog;

import java.lang.reflect.Field;

import static mindustry.Vars.net;
import static mindustry.Vars.state;

public class LogicDialogAddon {
    public static Field executorField;
    public static Field wasPausedField;
    public static GlobalVarsDialog globalsDialog;

    public static void init() {
        try {
            executorField = LogicDialog.class.getDeclaredField("executor");
            executorField.setAccessible(true);

            Field dialogField = LogicDialog.class.getDeclaredField("globalsDialog");
            dialogField.setAccessible(true);
            globalsDialog = (GlobalVarsDialog) dialogField.get(Vars.ui.logic);

            wasPausedField = BaseDialog.class.getDeclaredField("wasPaused");
            wasPausedField.setAccessible(true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.err("Cannot access LogicDialog fields", e);

            // No point modifying the Logic dialog if we can't fully operate it
            return;
        }

        Vars.ui.logic.shown(LogicDialogAddon::setupLogicDialog);

        Events.on(EventType.ResizeEvent.class, event -> {
            if (Vars.ui.logic.isShown() && Core.scene.getDialog() == Vars.ui.logic) {
                setupLogicDialog();
            }
        });
    }

    private static class HookTable extends Table {
        public Cell<TextButton> button(String text, Drawable image, Runnable clicked) {
            return super.button(text, image, "@variables".equals(text) ? () -> Vars.ui.showInfo("Hooray") : clicked);
        }
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
            Log.err("Cannot access LogicDialog fields", e);
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

                    VarsDialog dialog = new VarsDialog(new ProcessorVars(executor));

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
