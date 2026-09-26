package cardillan.mlogassertions.data;

import arc.struct.Seq;
import cardillan.mlogassertions.logic.SnapshotType;
import mindustry.gen.Building;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public interface Snapshot extends VariableValues {
    long timestamp();
    SnapshotType type();
    String name();
    int id();

    // List of all snapshots in the group
    // Retuns null - not an empty Seq! - when the snapshot is isolated.
    Seq<Snapshot> group();

    boolean writeTo(VariableValues liveData);
}
