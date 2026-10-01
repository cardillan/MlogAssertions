package cardillan.mlogassertions.data;

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Log;
import cardillan.mlogassertions.logic.SnapshotType;
import mindustry.ai.types.LogicAI;
import mindustry.entities.units.UnitController;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Entityc;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.logic.Senseable;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.logic.MemoryBlock;
import mindustry.world.blocks.logic.MemoryBlock.MemoryBuild;

public class Snapshots {
    public static int maxSnapshots = 20;
    private static int id = 0;

    // Snapshots
    private static final ObjectMap<Senseable, Queue<Snapshot>> snapshots = new ObjectMap<>();

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

    public static void updateLimit() {
        if (maxSnapshots == 0) {
            deleteAll();
        } else {
            snapshots.each((b, q) -> {
                while (q.size > maxSnapshots) q.removeLast();
            });
        }
    }

    public static VariableValues liveView(Senseable entity) {
        return entity instanceof MemoryBuild build ? new MemoryVars(build) :
                entity instanceof LogicBuild build ? new ProcessorVars(build) :
                        new SensorVars(entity);
    }

    public static boolean hasSnapshots(Senseable entity) {
        Queue<Snapshot> queue = snapshots.get(entity);
        return queue != null && queue.size > 0;
    }

    public static Queue<Snapshot> get(Senseable entity) {
        snapshots.putMissing(entity, new Queue<>());
        return snapshots.get(entity);
    }


    public static void create(Senseable entity, String name) {
        create(entity, SnapshotType.connected, name);
    }

    public static void create(Senseable entity, SnapshotType type, String name) {
        if (maxSnapshots == 0) return;

        VariableValues content = liveView(entity);
        if (!content.valid()) return;

        id++;
        Seq<Senseable> entities = new Seq<>();
        switch (type) {
            case isolated -> entities.add(entity);
            case connected -> {
                entities.add(entity);
                ObjectSet<Object> set = new ObjectSet<>();
                content.eachObject(o -> {
                    if (o instanceof Building build && set.add(o)) entities.add(build);
                    if (o instanceof Unit unit && set.add(o)) entities.add(unit);
                });
                if (entity instanceof LogicBuild) {
                    Groups.unit.each(u -> {
                        if (u.controller() instanceof LogicAI ai && ai.controller == entity && set.add(u)) {
                            entities.add(u);
                        }
                    });
                }
            }
            case global -> {
                entities.addAll(MapIndex.processors);
                entities.addAll(MapIndex.memories);
            }
        }

        Seq<Snapshot> group = type == SnapshotType.isolated ? null : new Seq<>();

        for (Senseable e : entities) {
            Queue<Snapshot> queue = get(e);
            Snapshot snapshot = create(e, type, group, name);
            if (snapshot != null) {
                queue.addFirst(snapshot);
                if (queue.size > maxSnapshots) queue.removeLast();
            }
        }
    }

    public static void deleteEntity(Senseable entity) {
        snapshots.get(entity).clear();
    }

    public static void deleteAll() {
        snapshots.each((b, q) -> q.clear());
        id = 0;
    }

    private static Snapshot create(Senseable entity, SnapshotType type, Seq<Snapshot> group, String name) {
        Snapshot snapshot =
                entity instanceof MemoryBlock.MemoryBuild build ? MemorySnapshot.create(build, type, id, group, name) :
                        entity instanceof LogicBuild build ? ProcessorSnapshot.create(build, type, id, group, name) :
                                SensorSnapshot.create(entity, type, id, group, name);

        if (group != null && snapshot != null) group.add(snapshot);
        return snapshot;
    }
}
