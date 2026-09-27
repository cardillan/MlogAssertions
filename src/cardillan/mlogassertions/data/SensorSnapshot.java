package cardillan.mlogassertions.data;

import arc.struct.Seq;
import arc.util.Time;
import cardillan.mlogassertions.logic.SnapshotType;
import mindustry.gen.Building;
import mindustry.logic.LAccess;
import mindustry.logic.LVar;
import mindustry.logic.Senseable;
import mindustry.world.blocks.logic.LogicBlock;

public class SensorSnapshot extends SensorVars implements Snapshot{
    public String name;
    public final SnapshotType type;
    public final int id;
    public final Seq<Snapshot> group;
    public final long timestamp = Time.millis();

    public final Object[] values = new Object[length];

    private float[] typeDistribution = null;

    public SensorSnapshot(Building build, SnapshotType type, int id, Seq<Snapshot> group, String name) {
        super(build);
        this.type = type;
        this.id = id;
        this.group = group;
        this.name = name;

        for (int i = 0; i < length; i++) {
            values[i] = super.isObj(i) ? super.obj(i) : super.num(i);
        }

        typeDistribution = computeTypeDistribution();
    }

    @Override
    public boolean isObj(int index) {
        return !(values[index] instanceof Double);
    }

    @Override
    public Object obj(int index) {
        return values[index] instanceof Double ? null : values[index];
    }

    @Override
    public double num(int index) {
        return values[index] instanceof Double d ? d : 0;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public SnapshotType type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public Seq<Snapshot> group() {
        return group;
    }



    @Override
    public boolean writeTo(VariableValues liveData) {
        return false;
    }

    @Override
    public float[] typeDistribution() {
        return typeDistribution;
    }
}
