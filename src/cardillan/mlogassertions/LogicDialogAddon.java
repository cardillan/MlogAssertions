package cardillan.mlogassertions;

import arc.Core;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.logic.LExecutor;
import mindustry.logic.LogicDialog;

import java.lang.reflect.Field;

public class LogicDialogAddon {

    public static void init() {
        Vars.ui.logic.shown(LogicDialogAddon::setupLogicDialog);
    }

    private static void setupLogicDialog() {
        LExecutor executor;
        try {
            Field executorField = LogicDialog.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executor = (LExecutor)executorField.get(Vars.ui.logic);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.err("Cannot access LogicDialog.executor", e);
            return;
        }

        LogicDialog logicDialog = Vars.ui.logic;
        logicDialog.buttons.button("@asserts.copyvariables", Icon.copy, () -> {
            StringBuilder sbr = new StringBuilder(500);
            sbr.append("Variable\tValue\n");
            for (var v : executor.vars) {
                sbr.append(v.name).append("\t").append(v.isobj ? LExecutor.PrintI.toString(v.objval) : v.numval + "").append("\n");
            }
            Core.app.setClipboardText(sbr.toString());
        });

        logicDialog.buttons.button("@asserts.copyprintbuffer", Icon.copy, () -> {
            String text = "Printbuffer contents:\n" + executor.textBuffer.toString();
            Core.app.setClipboardText(text);
        });
    }
}
