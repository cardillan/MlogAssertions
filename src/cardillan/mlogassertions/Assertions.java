package cardillan.mlogassertions;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.ui.layout.Scl;
import arc.struct.GridMap;
import arc.util.Align;
import arc.util.pooling.Pools;
import mindustry.game.EventType;
import mindustry.ui.Fonts;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

import static mindustry.Vars.tilesize;

public class Assertions {
    final static GridMap<LogicBuild> blocks = new GridMap<>();
    final static GridMap<String> messages = new GridMap<>();
    final static Color textColor = Color.brick; //.a(0.8f);

    static int removeX = -1;
    static int removeY = -1;

    public static void add(LogicBuild block, String message) {
        int x = (int) block.getX();
        int y = (int) block.getY();
        if (blocks.get(x, y) != block) {
            blocks.put(x, y, block);
            messages.put(x, y, message);
        }
    }

    public static void remove(LogicBuild block) {
        remove((int) block.getX(), (int) block.getY());
    }

    public static void remove(int x, int y) {
        blocks.remove(x, y);
        messages.remove(x, y);
    }

    public static void init() {
        Events.on(EventType.ConfigEvent.class, e -> remove((int) e.tile.x, (int) e.tile.y));

        Events.run(EventType.Trigger.drawOver, () -> {
            blocks.values().forEach(Assertions::draw);
            if (removeX != -1) {
                remove(removeX, removeY);
                removeX = -1;
                removeY = -1;
            }
        });
    }

    private static void draw(LogicBuild block) {
        if (block.tile.build != block) {
            removeX = (int) block.getX();
            removeY = (int) block.getY();
            return;
        }

        String message = messages.get((int) block.getX(), (int) block.getY());

        float x = block.getX();
        float y = block.getY() + block.block.size * tilesize / 2f + 1f;

        Font font = Fonts.outline;
        GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.getData().setScale(1 / 4f / Scl.scl(1f));
        font.setUseIntegerPositions(false);

        l.setText(font, message, textColor, 90f, Align.left, false);

//        Draw.color(0f, 0f, 0f, 0.2f);
//        Fill.rect(x, y+2f, l.width+4f, l.height+2f);

        Draw.color();
        font.setColor(textColor);
        font.draw(message, x - l.width/2f, y + l.height, 90f, Align.left, false);
        font.setUseIntegerPositions(ints);

        font.getData().setScale(1f);

        Pools.free(l);
    }
}
