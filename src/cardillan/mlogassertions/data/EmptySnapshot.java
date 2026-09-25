package cardillan.mlogassertions.data;

import arc.util.Time;
import mindustry.gen.Building;

public class EmptySnapshot implements Snapshot {
    public final Building build;
    public final long timestamp = Time.millis();

    public EmptySnapshot(Building build) {
        this.build = build;
    }

    @Override
    public Building building() {
        return build;
    }

    @Override
    public boolean processor() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String label(int index, boolean hex) {
        return "";
    }

    @Override
    public String formatted(int index, boolean hex) {
        return "";
    }

    @Override
    public String clipboard(int index, boolean hex) {
        return "";
    }

    @Override
    public ValueType type(int index) {
        return null;
    }

    @Override
    public boolean isObj(int index) {
        return false;
    }

    @Override
    public boolean isLink(int index) {
        return false;
    }

    @Override
    public Object obj(int index) {
        return null;
    }

    @Override
    public double num(int index) {
        return 0;
    }

    @Override
    public String textBuffer() {
        return "";
    }

    @Override
    public void clear() {
    }

    @Override
    public void setView(boolean sorted, boolean hideTemps, boolean hideLinks) {
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String name() {
        return "Invalid snapshot";
    }

    @Override
    public boolean writeTo(VariableValues liveData) {
        return false;
    }
}
