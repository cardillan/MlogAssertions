package cardillan.mlogassertions.ui;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.geom.Rect;
import arc.scene.ui.layout.Scl;
import arc.struct.FloatSeq;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.pooling.Pools;
import cardillan.mlogassertions.Constants;
import cardillan.mlogassertions.Settings;
import cardillan.mlogassertions.logic.LogicInstructions;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.logic.LExecutor;
import mindustry.ui.Fonts;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

import static arc.Core.camera;
import static arc.Core.settings;
import static mindustry.Vars.tilesize;

public class Assertions {
    public static int minWaitTimeUpdate = 1000;
    public static int processorUpdatesPerTick = 50;
    public static int warnEffectFrequency = 0;
    public static float layer = Layer.darkness + 1;
    public static float waitLayer = Layer.turret + 1;
    public static float textWidth = 110f;

    // wait is a special case, recognized by comparison to this instance
    static final String WAIT = new String("W");

    // Color of the displayed text/warning effect
    static final Color color = Color.coral;

    // Camera bounds
    private static final Rect wideBounds = new Rect();
    private static final Rect narrowBounds = new Rect();
    private static final Rect hitbox = new Rect();

    // Active messages
    static final ObjectMap<LogicBuild, String> blocks = new ObjectMap<>();

    // All processors
    static final Seq<LogicBuild> allBlocks = new Seq<>();

    // Invalid blocks
    static final Seq<LogicBuild> invalidBlocks = new Seq<>();

    // The breakpoint context
    static LogicBuild breakpointProc;
    static String breakpointMessage;
    static final FloatSeq accumulators = new FloatSeq();

    // The next time the effect should be run (game time)
    static double nextWarnEffect = 0;

    private static void effect(LogicBuild block) {
        Fx.unitCapKill.at(block.getX(), block.getY(), 10f, color);
    }

    public static void setWait(LogicBuild block) {
        if (blocks.get(block) != WAIT) {
            blocks.put(block, WAIT);
        }
    }

    public static void setMessage(LogicBuild block, Prov<String> message) {
        String prev = blocks.get(block);
        if (prev == null || prev == WAIT) {
            String str = message.get();
            blocks.put(block, str == null ? "<error>" : str);

            // Just this once
            if (warnEffectFrequency == 0) effect(block);
        }
    }

    public static void breakpoint(LogicBuild processor, String message) {
        Vars.state.set(GameState.State.paused);

        if (Settings.detachCameraOnBreakpoint()) {
            settings.put(Constants.reattachCamera, !settings.getBool(Constants.detachCamera, false));
            settings.put(Constants.detachCamera, true);
        }
        Core.camera.position.set(processor.getX(), processor.getY());

        // Clear all accumulators
        accumulators.clear();
        allBlocks.forEach(b -> {
            accumulators.add(b.accumulator);
            b.accumulator = 0;
        });

        // Restore all accumulators right after the update has finished
        Core.app.post(() -> {
            for (int i = 0; i < accumulators.size; i++) {
                allBlocks.get(i).accumulator += accumulators.get(i);
            }
        });

        breakpointProc = processor;
        breakpointMessage = message;
        blocks.remove(processor);
    }

    public static void reset(LogicBuild block) {
        blocks.remove(block);
    }

    public static void init() {
        Events.on(EventType.ResetEvent.class, e -> {
            blocks.clear();
            allBlocks.clear();
            invalidBlocks.clear();
            nextWarnEffect = 0;
        });

        Events.on(EventType.WorldLoadEndEvent.class, e -> {
            blocks.clear();
            allBlocks.clear();
            invalidBlocks.clear();
            nextWarnEffect = 0;

            Groups.build.forEach(b -> {
                if (b instanceof LogicBuild build && blocks.put(build, "") == null) {
                    allBlocks.add(build);
                }
            });

            blocks.clear(32);

            Log.info("WorldLoadEndEvent: found " + allBlocks.size + " processors on the map.");
        });

        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (e.tile.build instanceof LogicBuild build) {
                //Log.info("BlockBuildEndEvent: new processor " + e.tile.build);
                allBlocks.add(build);
            }
        });

        Events.on(EventType.ConfigEvent.class, e -> {
            if (e.tile instanceof LogicBuild build) {
                reset(build);
                //Log.info("ConfigEvent: configured processor " + e.tile);
            }
        });

        Events.on(EventType.StateChangeEvent.class, e -> {
            if (e.from == GameState.State.paused) {
                breakpointProc = null;
                reattachCamera();
            }
        });

        Events.run(EventType.Trigger.drawOver, () -> {
            checkBlocks();

            camera.bounds(narrowBounds);
            wideBounds.set(narrowBounds);
            narrowBounds.grow(tilesize * 2f);
            wideBounds.grow(tilesize * 10f);

            blocks.each(Assertions::draw);
            if (breakpointProc != null) {
                draw(breakpointProc, breakpointMessage);
            }

            invalidBlocks.each(blocks::remove);
            allBlocks.removeAll(invalidBlocks);
            invalidBlocks.clear();
        });

        // Reattach the camera if the game was closed while paused
        reattachCamera();
    }

    private static void reattachCamera() {
        if (Core.settings.getBool(Constants.detachCamera)) {
            Core.settings.put(Constants.detachCamera, false);
            Core.settings.put(Constants.reattachCamera, true);
        }
    }

    static int checkIndex;
    static double totalUpdates = 0;
    static boolean runWarnEffect;

    private static void checkBlocks() {
        // Do not lose fractional values of updates at high FPS
        // Throttle down on low FPS
        totalUpdates += Math.min(Core.graphics.getDeltaTime() * 60, 5) * processorUpdatesPerTick;
        long updates = (long) totalUpdates;
        totalUpdates -= updates;

        if (allBlocks.size <= updates) {
            allBlocks.each(Assertions::check);
        } else {
            for (int i = 0; i < updates; i++) {
                if (checkIndex >= allBlocks.size) checkIndex = 0;
                check(allBlocks.get(checkIndex++));
            }
        }

        // Should spawn warning effects during this update?
        if (warnEffectFrequency > 0 && nextWarnEffect < Vars.state.tick) {
            runWarnEffect = true;
            nextWarnEffect = Vars.state.tick + 60 * warnEffectFrequency;
        } else {
            runWarnEffect = false;
        }
    }

    private static void check(LogicBuild block) {
        if (block.tile.build != block) {
            //Log.info("Removed block " + block);
            invalidBlocks.add(block);
            return;
        } else if (block.executor == null || block.executor.counter == null) {
            // Not yet ready
            reset(block);
            return;
        }

        int ix = (int) block.executor.counter.numval;
        LExecutor.LInstruction[] instructions = block.executor.instructions;

        if (ix >= 0 && ix < instructions.length) {
            LExecutor.LInstruction instruction = instructions[ix];
            if (instruction instanceof LogicInstructions.AssertInstruction) {
                // These are handled in the instruction itself
                return;
            }
            if (instruction instanceof LExecutor.StopI) {
                setMessage(block, () -> Core.bundle.format("assertions.stoppedAt", ix));
                return;
            }
            if (minWaitTimeUpdate > 0 && instruction instanceof LExecutor.WaitI w && 1000 * w.value.num() >= minWaitTimeUpdate) {
                setWait(block);
                return;
            }
        }

        reset(block);
    }

    private static void draw(LogicBuild block, String message) {
        if (block.tile.build != block) {
            //Log.info("Removed block " + block);
            invalidBlocks.add(block);
            return;
        }

        // No processing when out of bounds
        block.hitbox(hitbox);
        if (!wideBounds.overlaps(hitbox)) return;

        // This is a wait indication
        if (message == WAIT) {
            if (narrowBounds.overlaps(hitbox)) {
                drawWait(block);
            }
            return;
        }

        if (runWarnEffect) {
            effect(block);
        }

        float x = block.getX();
        float y = block.getY() + (block.block.size * tilesize/2f + 1.5f);

        Draw.z(layer);
        float z = Drawf.text();

        Font font = Fonts.outline;
        GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.getData().setScale(1 / 4f / Scl.scl(1f));
        font.setUseIntegerPositions(false);

        l.setText(font, message, color, textWidth, Align.left, true);

        Draw.color();
        font.setColor(color);
        font.draw(message, x - l.width/2f, y + l.height, textWidth, Align.left, true);
        font.setUseIntegerPositions(ints);
        font.getData().setScale(1f);
        Draw.z(z);

        Pools.free(l);
    }

    private static void drawWait(LogicBuild block) {
        int sides = 60;
        int ix = (int) block.executor.counter.numval;
        LExecutor.LInstruction[] instructions = block.executor.instructions;
        if (ix >= 0 && ix < instructions.length && instructions[ix] instanceof LExecutor.WaitI w) {
            float total = (float) w.value.num();
            float current = w.curTime;
            float arc = current / total;

            float x = block.getX();
            float y = block.getY();

            Draw.z(waitLayer);
            float z = Drawf.text();
            int blockSize = block.tile.block().size;
            if (arc > 0.01) {
                Draw.color(Color.white);
                Fill.arc(x, y, Scl.scl(blockSize * 2.1f - 0.5f), arc, 90 - 360f * arc, sides);
            }
            Draw.color(Color.white);
            Lines.stroke(Scl.scl((blockSize + 1) * 0.25f));
            Lines.poly(x, y, sides, Scl.scl(block.tile.block().size * 2.5f));
            Draw.z(z);
        }
    }
}
