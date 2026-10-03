package cardillan.mlogassertions.data;

import arc.struct.Seq;
import cardillan.mlogassertions.logic.SnapshotType;
import mindustry.logic.LVar;

public interface Snapshot extends VariableValues {
    SnapshotType type();
    String name();
    int id();

    // List of all snapshots in the group
    // Retuns null - not an empty Seq! - when the snapshot is isolated.
    Seq<Snapshot> group();

    // List of snaphots within a recording snapshott, null if not a recording snapshot
    Seq<Snapshot> recording();

    // Returns the porportion of each type in the snapshot
    float[] typeDistribution();

    boolean writeTo(VariableValues liveData);

    void setDefaultFilter(LVar[] vars);
}
