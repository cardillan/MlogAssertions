package cardillan.mlogassertions.data;

import mindustry.gen.Building;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public interface Snapshot extends VariableValues {
    long timestamp();
    String name();

    boolean writeTo(VariableValues liveData);
}
