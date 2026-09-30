package cardillan.mlogassertions;

import arc.graphics.Color;

public class Constants {
    public static final double COLOR_LIMIT = Color.white.toDoubleBits();

    public static final String assertionsCategory = "asserts";

    // Camera state
    public static final String detachCamera = "detach-camera";
    public static final String reattachCamera = "reattach-camera";

    // Mod settings
    public static final String disableBreakpoints = "disable-breakpoints";
    public static final String assertsAreBreakpoints = "asserts-are-breakpoints";
    public static final String freeCameraOnBreakpoint = "free-camera-on-breakpoint";
    public static final String maxInstructions = "max-instructions";
    public static final String minWaitTimeUpdate = "min-wait-time-update";
    public static final String processorUpdatesPerTick = "processor-updates-per-tick";
    public static final String warnEffectFrequency = "warn-effect-frequency";
    public static final String tripleTapSpeed = "triple-tap-speed";
    public static final String snapshotLimit = "snapshot-limit";

    public static final String snapshotOnBreakpoint = "snapshot-on-breakpoint";
    public static final String snapshotOnAssertion = "snapshot-on-assertion";

    public static final String variableUpdateFrequency = "variable-update-frequency";
    public static final String varsSignificantDigits = "vars-significant-digits";
    public static final String varsAlignment = "vars-alignment";
}
