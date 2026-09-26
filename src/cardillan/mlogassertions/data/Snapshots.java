package cardillan.mlogassertions.data;

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.Queue;
import arc.struct.Seq;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.logic.MemoryBlock;
import mindustry.world.blocks.logic.MemoryBlock.MemoryBuild;

public class Snapshots {
    static int maxSnapshots = 50;

    // Snapshots
    public static final ObjectMap<Building, Queue<Snapshot>> snapshots = new ObjectMap<>();

    public static void init() {
        Events.on(EventType.ResetEvent.class, e -> {
            snapshots.clear();
        });

        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (e.breaking && (e.tile.build instanceof LogicBuild || e.tile.build instanceof MemoryBuild)) {
                if (e.breaking) {
                    snapshots.remove(e.tile.build);
                }
            }
        });
    }

    public static VariableValues liveView(Building building) {
        if (building instanceof MemoryBlock.MemoryBuild build) return new MemoryVars(build);
        if (building instanceof LogicBlock.LogicBuild build) return new ProcessorVars(build);
        return new EmptySnapshot(building);
    }

    public static Snapshot create(Building building, String name) {
        if (building instanceof MemoryBlock.MemoryBuild build) return new MemorySnapshot(build, name);
        if (building instanceof LogicBlock.LogicBuild build) return new ProcessorSnapshot(build, name);
        return new EmptySnapshot(building);
    }

    public static Queue<Snapshot> get(Building building) {
        snapshots.putMissing(building, new Queue<>());
        return snapshots.get(building);
    }

    public static void add(Building building, String name) {
        Queue<Snapshot> queue = get(building);
        queue.addFirst(create(building, name));
        if (queue.size > maxSnapshots) queue.removeLast();
    }
}
