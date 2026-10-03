package cardillan.mlogassertions.data;

import arc.Events;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.logic.MemoryBlock.MemoryBuild;

public class MapIndex {
    // Keeps a current list of all processors and memory blocks on the map
    public static final Seq<LogicBuild> processors = new Seq<>();
    public static final Seq<MemoryBuild> memories = new Seq<>();

    public static void init() {
        Events.on(EventType.ResetEvent.class, e -> {
            processors.clear();
            memories.clear();
        });

        Events.on(EventType.WorldLoadEndEvent.class, e -> {
            processors.clear();
            memories.clear();

            Groups.build.each(b -> {
                if (b instanceof LogicBuild processor) processors.add(processor);
            });

            ObjectSet<MemoryBuild> seen = new ObjectSet<>();
            Vars.world.tiles.eachTile(tile -> {
                if (tile.build instanceof MemoryBuild memory && seen.add(memory)) memories.add(memory);
            });

            Log.info("[Mlog Dev Tools] found " + processors.size + " processors and " + memories.size + " memory blocks on the map.");
        });

        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (e.tile.build instanceof LogicBuild build) {
                if (e.breaking) processors.remove(build); else processors.add(build);
            }
            if (e.tile.build instanceof MemoryBuild build) {
                if (e.breaking) memories.remove(build); else memories.add(build);
            }
        });
    }
}
