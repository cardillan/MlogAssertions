package cardillan.mlogassertions.data;

import arc.struct.Seq;
import arc.util.Time;
import cardillan.mlogassertions.logic.SnapshotType;
import mindustry.Vars;
import mindustry.logic.GlobalVars;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.MemoryBlock.MemoryBuild;

import java.util.Arrays;
import java.util.Date;
import java.util.ResourceBundle;

public class MemorySnapshot extends MemoryVars implements Snapshot {
    public String name;
    public final SnapshotType type;
    public final int id;
    public final Seq<Snapshot> group;

    public static MemorySnapshot create(MemoryBuild build, SnapshotType type, int id, Seq<Snapshot> group, String name) {
        return new MemorySnapshot(build, type, id, group, name);
    }

    private MemorySnapshot(MemoryBuild build, SnapshotType type, int id, Seq<Snapshot> group, String name) {
        super(build, false);
        this.type = type;
        this.id = id;
        this.group = group;
        this.name = name;
    }

    @Override
    public SnapshotType type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    public int id() {
        return id;
    }

    @Override
    public Seq<Snapshot> group() {
        return group;
    }

    @Override
    public Seq<Snapshot> recording() {
        return null;
    }

    @Override
    public boolean writeTo(VariableValues liveData) {
        if (liveData instanceof MemoryVars memory) {
            if (memory.length != length) return false;
            System.arraycopy(objectMemory, 0, memory.objectMemory, 0, length);
            System.arraycopy(numberMemory, 0, memory.numberMemory, 0, length);
            return true;
        } else {
            return false;
        }
    }

    private float[] typeDistribution = null;

    @Override
    public float[] typeDistribution() {
        if (typeDistribution == null) typeDistribution = computeTypeDistribution();
        return typeDistribution;
    }

    @Override
    public void setDefaultFilter(LVar[] vars) {
        // Do nothing
    }
}
