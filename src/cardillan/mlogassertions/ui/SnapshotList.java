package cardillan.mlogassertions.ui;

import arc.Core;
import arc.struct.Queue;
import arc.struct.Seq;
import cardillan.mlogassertions.data.Snapshot;
import cardillan.mlogassertions.data.Snapshots;
import cardillan.mlogassertions.data.VariableValues;
import mindustry.gen.Building;

interface SnapshotList {
    boolean group();
    String title();
    int size();
    VariableValues view(int index);
    Snapshot snapshot(int index);
    void remove(int index);

    static SnapshotList list(final Building building) {
        return new SnapshotList() {
            final VariableValues data = Snapshots.liveView(building);
            final Queue<Snapshot> queue = Snapshots.get(building);

            @Override
            public boolean group() {
                return false;
            }

            @Override
            public String title() {
                return data.processor() ? "@variables" : "@varsdialog.memory";
            }

            @Override
            public int size() {
                return queue.size + 1;
            }

            @Override
            public VariableValues view(int index) {
                return index == 0 ? data : queue.get(index - 1);
            }

            @Override
            public Snapshot snapshot(int index) {
                return index == 0 ? null : queue.get(index - 1);
            }

            @Override
            public void remove(int index) {
                queue.removeIndex(index - 1);
            }
        };
    }

    static SnapshotList list(Seq<Snapshot> snapshots) {
        return new SnapshotList() {

            @Override
            public boolean group() {
                return true;
            }

            @Override
            public String title() {
                return "Snapshot #" + snapshots.get(0).id() + ": " + snapshots.get(0).type() + " snapshot";
            }

            @Override
            public int size() {
                return snapshots.size;
            }

            @Override
            public VariableValues view(int index) {
                return snapshots.get(index);
            }

            @Override
            public Snapshot snapshot(int index) {
                return snapshots.get(index);
            }

            @Override
            public void remove(int index) {
            }
        };
    }
}
