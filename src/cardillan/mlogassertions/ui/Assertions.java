package cardillan.mlogassertions.ui;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.ui.layout.Scl;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.pooling.Pools;
import cardillan.mlogassertions.logic.LogicInstructions;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.logic.LExecutor;
import mindustry.ui.Fonts;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

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

    // Color of the displayed text
    static final Color color = Color.coral;

    // Active messages
    static final ObjectMap<LogicBuild, String> blocks = new ObjectMap<>();

    // All blocks
    static final Seq<LogicBuild> allBlocks = new Seq<>();

    // Invalid blocks
    static final Seq<LogicBuild> invalidBlocks = new Seq<>();

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

            Vars.world.tiles.eachTile(tile -> {
                if (tile.build instanceof LogicBuild build && blocks.put(build, "") == null) {
                    allBlocks.add(build);
                }
            });

            blocks.clear(32);

            Log.info("WorldLoadEndEvent: found " + allBlocks.size + " processors on the map.");
        });

        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (e.tile.build instanceof LogicBuild build) {
                Log.info("BlockBuildEndEvent: new processor " + e.tile.build);
                allBlocks.add(build);
            }
        });

        Events.on(EventType.ConfigEvent.class, e -> {
            if (e.tile instanceof LogicBuild build) {
                reset(build);
                Log.info("ConfigEvent: configured processor " + e.tile);
            }
        });

        Events.run(EventType.Trigger.drawOver, () -> {
            checkBlocks();
            blocks.each(Assertions::draw);

            invalidBlocks.each(blocks::remove);
            allBlocks.removeAll(invalidBlocks);
            invalidBlocks.clear();
        });
    }

    static int checkIndex;
    static double totalUpdates = 0;
    static boolean runWarnEffect;

    private static void checkBlocks() {
        // Do not lose fractional values of updates at high FPS
        totalUpdates += Math.max(Core.graphics.getDeltaTime(), 1.5) * processorUpdatesPerTick;
        long updates = (long) totalUpdates;
        totalUpdates -= updates;

        if (allBlocks.size <= updates) {
            allBlocks.each(Assertions::check);
        } else {
            for (int i = 0; i < updates; i++) {
                check(allBlocks.get(checkIndex));
                checkIndex = (checkIndex + 1) % allBlocks.size;
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
            Log.info("Removed block " + block);
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
                setMessage(block, () -> "Stopped at #" + ix);
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
            Log.info("Removed block " + block);
            invalidBlocks.add(block);
            return;
        }

        // This is a wait indication
        boolean wait = message == WAIT;

        if (runWarnEffect && !wait) {
            effect(block);
        }

        float x = block.getX();
        float y = block.getY() + (wait ? 0 : block.block.size * tilesize/2f + 1.5f);

        Draw.z(wait ? waitLayer : layer);
        float z = Drawf.text();

        Font font = Fonts.outline;
        GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.getData().setScale(1 / 4f / Scl.scl(1f));
        font.setUseIntegerPositions(false);

        l.setText(font, message, color, textWidth, Align.left, true);

        Draw.color();
        font.setColor(color);
        font.draw(message, x - l.width/2f, y + (wait ? l.height/2f : l.height), textWidth, Align.left, true);
        font.setUseIntegerPositions(ints);
        font.getData().setScale(1f);
        Draw.z(z);

        Pools.free(l);
    }
}
