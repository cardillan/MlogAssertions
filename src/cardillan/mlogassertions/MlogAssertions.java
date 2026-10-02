package cardillan.mlogassertions;

import arc.Events;
import cardillan.mlogassertions.data.MapIndex;
import cardillan.mlogassertions.data.SnapshotManager;
import cardillan.mlogassertions.logic.AssertLogic;
import cardillan.mlogassertions.logic.InstrumentationEngine;
import cardillan.mlogassertions.ui.Assertions;
import cardillan.mlogassertions.ui.LogicDialogAddon;
import cardillan.mlogassertions.ui.BuildConfiguration;
import cardillan.mlogassertions.data.MemoryVars;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class MlogAssertions extends Mod {

    public MlogAssertions() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            Settings.init();

            // Logic
            AssertLogic.init();
            InstrumentationEngine.init();

            // Map
            MapIndex.init();
            SnapshotManager.init();

            // UI
            Assertions.init();
            LogicDialogAddon.init();
            BuildConfiguration.init();
            MemoryVars.init();
        });
    }
}
