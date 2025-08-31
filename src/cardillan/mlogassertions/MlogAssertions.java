package cardillan.mlogassertions;

import arc.Core;
import arc.Events;
import cardillan.mlogassertions.logic.AssertLogic;
import mindustry.game.EventType;
import mindustry.logic.LExecutor;
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
