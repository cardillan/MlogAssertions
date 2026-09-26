package cardillan.mlogassertions.data;

import arc.struct.Seq;
import arc.util.Time;
import cardillan.mlogassertions.logic.SnapshotType;
import mindustry.gen.Building;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

public class ProcessorSnapshot extends ProcessorVars implements Snapshot {
    public String name;
    public final SnapshotType type;
    public final int id;
    public final Seq<Snapshot> group;
    public final long timestamp = Time.millis();

    public final String textBuffer;

    public ProcessorSnapshot(LogicBuild build, SnapshotType type, int id, Seq<Snapshot> group, String name) {
        super(build);
        this.type = type;
        this.id = id;
        this.group = group;
        this.textBuffer = executor.textBuffer.toString();
        this.name = name;
    }

    @Override
    protected LVar get(LVar var) {
        LVar copy = new LVar(var.name);
        copy.id = var.id;
        copy.isobj = var.isobj;
        copy.constant = var.constant;
        copy.objval = var.objval;
        copy.numval = var.numval;
        return copy;
    }

    public LogicBuild building() {
        return build;
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
        if (liveData instanceof ProcessorVars processor) {
            if (processor.data.length != data.length) return false;
            for (int i = 0; i < data.length; i++) {
                if (!data[i].name.equals(processor.data[i].name) || data[i].constant != processor.data[i].constant || data[i].id != processor.data[i].id) return false;
            }
            for (int i = 0; i < data.length; i++) {
                processor.data[i].objval = data[i].objval;
                processor.data[i].numval = data[i].numval;
            }
            return true;
        } else {
            return false;
        }
    }
}
