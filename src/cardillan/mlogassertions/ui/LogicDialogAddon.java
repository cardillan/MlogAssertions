package cardillan.mlogassertions.ui;

import arc.Core;
import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.logic.LExecutor;
import mindustry.logic.LogicDialog;

import java.lang.reflect.Field;

public class LogicDialogAddon {

    public static void init() {
        Vars.ui.logic.shown(LogicDialogAddon::setupLogicDialog);

        Events.on(EventType.ResizeEvent.class, event -> {
            if(Vars.ui.logic.isShown() && Core.scene.getDialog() == Vars.ui.logic){
                setupLogicDialog();
            }
        });
    }

    private static boolean alreadyModified;

    private static void setupLogicDialog() {
        LogicDialog logicDialog = Vars.ui.logic;

        // TODO is there a better way...?
        alreadyModified = false;
        logicDialog.buttons.forEach(button -> {
            if ("@asserts.copyvariables".equals(button.name)) alreadyModified = true;
        });

        if (alreadyModified) return;

        LExecutor executor;
        try {
            Field executorField = LogicDialog.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executor = (LExecutor) executorField.get(Vars.ui.logic);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.err("Cannot access LogicDialog.executor", e);
            return;
        }

        if (Core.graphics.isPortrait()) logicDialog.buttons.row();

        logicDialog.buttons.button("@asserts.copyvariables", Icon.copy, () -> {
            StringBuilder sbr = new StringBuilder(500);
            sbr.append("Variable\tValue\n");
            for (var v : executor.vars) {
                sbr.append(v.name).append("\t").append(v.isobj ? LExecutor.PrintI.toString(v.objval) : v.numval + "").append("\n");
            }
            Core.app.setClipboardText(sbr.toString());
        }).name("@asserts.copyvariables");

        logicDialog.buttons.button("@asserts.copyprintbuffer", Icon.copy, () -> {
            String text = "Printbuffer contents:\n" + executor.textBuffer.toString();
            Core.app.setClipboardText(text);
        });
    }
}
