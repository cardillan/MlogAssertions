package cardillan.mlogassertions.ui;

import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Log;
import cardillan.mlogassertions.data.*;
import mindustry.gen.Building;

interface SnapshotList {
    boolean group();
    BlockDataType dataType();
    String title();
    VariableValues liveData();

    String pos();

    VariableValues view();
    boolean next();
    boolean prev();
    boolean first();
    boolean last();
    boolean hasNext();
    boolean hasPrev();
    SnapshotList select(VariableValues var);

    boolean canRemove();
    boolean remove();

    Seq<Snapshot> list();

    static SnapshotList forBuild(final Building building) {
        return new SnapshotList() {
            static final int OBSOLETE = -2;
            static final int LIVE = -1;

            final VariableValues live = Snapshots.liveView(building);
            final Queue<Snapshot> queue = Snapshots.get(building);
            VariableValues view = live;
            int index = LIVE;

            @Override
            public boolean group() {
                return false;
            }

            @Override
            public BlockDataType dataType() {
                return live.dataType();
            }

            @Override
            public String title() {
                return live.dataType().name;
            }

            @Override
            public VariableValues liveData() {
                return live;
            }

            @Override
            public String pos() {
                updateIndex();
                return index == LIVE ? "live" : index < 0 ? "--/" + queue.size : (index + 1) + "/" + queue.size ;
            }

            @Override
            public VariableValues view() {
                return view;
            }

            @Override
            public boolean next() {
                updateIndex();
                if (index >= LIVE && index < queue.size - 1) {
                    index++;
                    view = queue.get(index);
                    return true;
                }
                return false;
            }

            @Override
            public boolean prev() {
                updateIndex();
                if (index == LIVE) return false;

                if (index >= 0) {
                    index--;
                    view = index == LIVE ? live : queue.get(index);
                } else if (index == OBSOLETE) {
                    view = queue.get(index = queue.size - 1);
                }
                return true;
            }

            @Override
            public boolean first() {
                if (index == LIVE) return false;

                index = LIVE;
                view = live;
                return true;
            }

            @Override
            public boolean last() {
                if (index == OBSOLETE || index >= queue.size - 1) return false;
                view = queue.get(index = queue.size - 1);
                return true;
            }

            @Override
            public boolean hasNext() {
                return index >= LIVE && index < queue.size - 1;
            }

            @Override
            public boolean hasPrev() {
                return index != LIVE;
            }

            @Override
            public SnapshotList select(VariableValues var) {
                if (var instanceof Snapshot snapshot) {
                    for (int i = 0; i < queue.size; i++) {
                        if (queue.get(i) == snapshot) {
                            index = i;
                            view = snapshot;
                            return this;
                        }
                    }
                    index = OBSOLETE;
                    view = snapshot;
                } else {
                    // Live view
                    first();
                }
                return this;
            }

            @Override
            public boolean remove() {
                if (updateIndex() < 0) return false;

                queue.removeIndex(index);
                if (index > queue.size) index--;
                return true;
            }

            @Override
            public boolean canRemove() {
                return (updateIndex() >= 0);
            }

            @Override
            public Seq<Snapshot> list() {
                Seq<Snapshot> seq = new Seq<>();
                queue.each(seq::add);
                return seq;
            }

            private int updateIndex() {
                Log.info("updateIndex: current = " + index);
                // Just to be sure
                if (!(view instanceof Snapshot)) return index = LIVE;

                if (index > LIVE) {
                    for (int i = index; i < queue.size; i++) {
                        if (queue.get(i) == view) {
                            Log.info("updateIndex: found " + i);
                            return index = i;
                        }
                    }
                    Log.info("updateIndex: obsolete");

                    // Obsolete
                    return index = -2;
                }

                Log.info("updateIndex: fixed index " + index);
                // Live and obsolete indexes can't change
                return index;
            }
        };
    }

    static SnapshotList list(Seq<Snapshot> snapshots) {
        return new SnapshotList() {
            int index = 0;

            @Override
            public boolean group() {
                return true;
            }

            @Override
            public BlockDataType dataType() {
                return snapshots.first().dataType();
            }

            @Override
            public String title() {
                return "Snapshot #" + snapshots.get(0).id() + ": " + snapshots.first().name();
            }

            @Override
            public VariableValues liveData() {
                return null;
            }

            @Override
            public String pos() {
                return (index + 1) + "/" + snapshots.size ;
            }

            @Override
            public Snapshot view() {
                return snapshots.get(index);
            }

            @Override
            public boolean next() {
                if (index >= snapshots.size - 1) return false;
                index++;
                return true;
            }

            @Override
            public boolean prev() {
                if (index == 0) return false;
                index--;
                return true;
            }

            @Override
            public boolean first() {
                if (index == 0) return false;
                index = 0;
                return true;
            }

            @Override
            public boolean last() {
                if (index >= snapshots.size - 1) return false;
                index = snapshots.size - 1;
                return true;
            }

            @Override
            public boolean hasNext() {
                return index < snapshots.size - 1;
            }

            @Override
            public boolean hasPrev() {
                return index > 0;
            }

            @Override
            public SnapshotList select(VariableValues var) {
                index = 0;
                for (int i = 0; i < snapshots.size; i++) {
                    if (snapshots.get(i) == var) {
                        index = i;
                        break;
                    }
                }
                return this;
            }

            @Override
            public boolean remove() {
                return false;
            }

            @Override
            public boolean canRemove() {
                return false;
            }

            @Override
            public Seq<Snapshot> list() {
                return snapshots;
            }
        };
    }
}
