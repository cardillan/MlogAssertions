package cardillan.mlogassertions;

import arc.Events;
import cardillan.mlogassertions.logic.AssertLogic;
import cardillan.mlogassertions.ui.Assertions;
import cardillan.mlogassertions.ui.LogicDialogAddon;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class MlogAssertions extends Mod {

    public MlogAssertions() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            Settings.init();

            LogicDialogAddon.init();

            AssertLogic.init();
            Assertions.init();
        });
    }
}
