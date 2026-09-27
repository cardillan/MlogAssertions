package cardillan.mlogassertions.data;

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Log;
import cardillan.mlogassertions.logic.SnapshotType;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.logic.MemoryBlock;
import mindustry.world.blocks.logic.MemoryBlock.MemoryBuild;

public class Snapshots {
    static int maxSnapshots = 1000;
    static int id = 0;

    // Snapshots
    private static final ObjectMap<Building, Queue<Snapshot>> snapshots = new ObjectMap<>();

    public static void init() {
        Events.on(EventType.ResetEvent.class, e -> {
            snapshots.clear();
            id = 0;
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
        return building instanceof MemoryBlock.MemoryBuild build ? new MemoryVars(build) :
                building instanceof LogicBlock.LogicBuild build ? new ProcessorVars(build) :
                new SensorVars(building);
    }

    public static Queue<Snapshot> get(Building building) {
        snapshots.putMissing(building, new Queue<>());
        return snapshots.get(building);
    }

    public static void create(Building building, String name) {
        create(building, SnapshotType.isolated, name);
    }

    public static void create(Building building, SnapshotType type, String name) {
        VariableValues content = liveView(building);
        if (!content.valid()) return;

        id++;
        Seq<Building> buildings = new Seq<>();
        switch (type) {
            case isolated -> buildings.add(building);
            case connected -> {
                buildings.add(building);
                ObjectSet<Object> set = new ObjectSet<>();
                content.eachObject(b -> {
                    if (b instanceof Building build && set.add(b)) buildings.add(build);
                });
            }
            case global -> {
                buildings.addAll(MapIndex.processors);
                buildings.addAll(MapIndex.memories);
            }
        }

        Seq<Snapshot> group = type == SnapshotType.isolated ? null : new Seq<>();

        for (Building b : buildings) {
            Queue<Snapshot> queue = get(b);
            queue.addFirst(create(b, type, group, name));
            if (queue.size > maxSnapshots) queue.removeLast();
        }
    }

    public static void deleteBuilding(Building building) {
        snapshots.get(building).clear();
    }

    public static void deleteAll() {
        snapshots.each((b, q) -> q.clear());
        id = 0;
    }

    private static Snapshot create(Building building, SnapshotType type, Seq<Snapshot> group, String name) {
        Snapshot snapshot =
                building instanceof MemoryBlock.MemoryBuild build ? new MemorySnapshot(build, type, id, group, name) :
                building instanceof LogicBlock.LogicBuild build ? new ProcessorSnapshot(build, type, id, group, name) :
                new SensorSnapshot(building, type, id, group, name);

        if (group != null) group.add(snapshot);
        return snapshot;
    }
}
