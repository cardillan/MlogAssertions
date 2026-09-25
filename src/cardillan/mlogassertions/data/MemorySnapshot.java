package cardillan.mlogassertions.data;

import arc.util.Time;
import mindustry.Vars;
import mindustry.world.blocks.logic.MemoryBlock.MemoryBuild;

import java.util.Arrays;
import java.util.Date;
import java.util.ResourceBundle;

public class MemorySnapshot extends MemoryVars implements Snapshot {
    public String name;
    public final long timestamp = Time.millis();

    public MemorySnapshot(MemoryBuild build, String name) {
        super(build, false);
        this.name = name;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String name() {
        return name;
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
}
