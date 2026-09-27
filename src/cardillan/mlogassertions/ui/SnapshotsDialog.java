package cardillan.mlogassertions.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.util.Scaling;
import cardillan.mlogassertions.data.*;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.MemoryBlock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SnapshotsDialog extends BaseDialog {
    static final DateFormat dateFormat = SimpleDateFormat.getTimeInstance();

    VarsDialog vars;
    SnapshotList snapshotList;
    Snapshot expanded = null;
    boolean group;

    // Saves/restores scroll position when the content of the pane changes
    float scroll = 0f;

    public SnapshotsDialog(VarsDialog vars, SnapshotList snapshotList) {
        super("Snapshots");
        this.vars = vars;
        this.snapshotList = snapshotList;
        this.group = snapshotList.group();
        setup();
    }

    public void setup() {
        float h = 120f;
        float w = Math.min(Core.graphics.getWidth() / Scl.scl(1.05f) - Scl.scl(28f), 520f);

        // Skip live view (for now)
        int start = snapshotList.group() ? 0 : 1;

        if (snapshotList.size() > start) {
            cont.clear();
            cont.pane(p -> {
                p.table(list -> {
                    for (int i = start; i < snapshotList.size(); i++) {
                        Snapshot snapshot = snapshotList.snapshot(i);
                        int index = i;

                        list.table(item -> {
                            item.add(createSnapshotButton(snapshot, index, group, w)).grow().pad(4f).padTop(8f);
                        }).size(w, h);
                        list.row();

                        if (snapshot == expanded) {
                            for (int i2 = 0; i2 < expanded.group().size; i2++) {
                                Snapshot inner = expanded.group().get(i2);
                                int index2 = i2;

                                list.table(item -> {
                                    item.add("").width(64f);
                                    item.add(createSnapshotButton(inner, index2, true, w - 96f)).grow().pad(4f).padTop(8f);
                                }).size(w, h);
                                list.row();
                            }
                        }
                    }
                });
            }).grow().pad(12f).scrollX(false).update(s -> scroll = s.getScrollY()).get().setScrollYForce(scroll);;
        } else {
            cont.add("No snapshots found.").row();
        }

        buttons.clear();
        addCloseButton();
    }

    private Button createSnapshotButton(Snapshot snapshot, int index, boolean group, float width) {
        Button b = new Button(Styles.grayt);
        b.clearChildren();  // ? - from arc
        b.margin(12f).left().top().defaults().left().top();
        int groupSize = groupSize(snapshot);
        Building build = snapshot.building();

        b.table(t -> {
            // A group snapshot is indented
            if (group) {
                Image image = new Image(new TextureRegionDrawable(build.block.uiIcon),
                        Vars.mobile ? Color.white : Color.lightGray).setScaling(Scaling.fit);
                t.add(image).size(Vars.iconXLarge).right().top().padRight(10f);
            } else {
                t.image(snapshot.type().icon).color(Pal.accent).size(Vars.iconXLarge).right().top().padRight(10f);
            }

            // Snapshot properties
            t.table(item -> {
                item.left();
                item.table(text -> {
                    if (group) {
                        text.add(build.block.name + BaseVariableValues.pos(build.x(), build.y())).growX().left();
                    } else {
                        text.add("Snapshot #" + snapshot.id() + ": " + groupSize + (groupSize > 1 ? " blocks" : " block")).growX().left();
                    }
                    text.row();
                    text.add(snapshot.name()).color(Pal.accent).ellipsis(true).growX().left();
                    text.row();
                    text.add(dateFormat.format(new Date(snapshot.timestamp()))).color(Color.gray).growX().left();
                }).top().growX();
                item.add().growX();
            }).growX().growY().left();

            // Buttons
            if (!group) {
                t.table(btn -> {
                    btn.button(expanded == snapshot ? Icon.upOpen : Icon.downOpen, Styles.clearNonei, () -> {
                        expanded = expanded == snapshot ? null : snapshot;
                        setup();
                    }).size(50f).disabled(groupSize <= 1);
                }).width(64f).right().top();
            }
        });

        b.row();

        b.table(t -> {
            float[] d = snapshot.typeDistribution();
            float w = width - 64f;
            int i = 0;
            for (ValueType type : ValueType.values()) {
                if (d[i] > 0) {
                    t.add(new Image(Tex.whiteui, type.shade)).width(w * d[i]).pad(0f).height(10f).padTop(8f);
                }
                i++;
            }
        });

        b.clicked(() -> {
            vars.setup(snapshotList, index);
            hide();
        });

        return b;
    }

    private int groupSize(Snapshot snapshot) {
        return snapshot.group() == null ? 1 : snapshot.group().size;
    }

    private VariableValues createLiveView(Building b) {
        if (b instanceof MemoryBlock.MemoryBuild build) return new MemoryVars(build);
        if (b instanceof LogicBlock.LogicBuild build) return new ProcessorVars(build);
        return null;
    }
}
