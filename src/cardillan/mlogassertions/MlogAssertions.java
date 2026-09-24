package cardillan.mlogassertions;

import arc.Core;
import arc.Events;
import cardillan.mlogassertions.logic.AssertLogic;
import cardillan.mlogassertions.ui.Assertions;
import cardillan.mlogassertions.ui.LogicDialogAddon;
import cardillan.mlogassertions.ui.MemoryConfiguration;
import cardillan.mlogassertions.ui.MemoryVars;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class MlogAssertions extends Mod {

    public MlogAssertions() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (Version.build >= 160) {
                Core.app.post(() -> Vars.ui.showErrorMessage(
                        """
                                This release of MlogAssertions doesn't support Mindustry 160 or higher.
                                Please update your mod to a compatible version using the Mod Browser.
                                The mod remains inactive to prevent crashes in the game."""));
                return;
            }

            Settings.init();

            LogicDialogAddon.init();
            MemoryConfiguration.init();
            MemoryVars.init();

            AssertLogic.init();
            Assertions.init();
        });
    }
}
