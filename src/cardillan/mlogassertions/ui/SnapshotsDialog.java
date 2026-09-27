package cardillan.mlogassertions.ui;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.util.Log;
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
    VarsDialog vars;
    SnapshotList snapshotList;
    Snapshot expanded = null;
    boolean group;

    // Saves/restores scroll position when the content of the pane changes
    float scroll = 0f;
    float w;

    public SnapshotsDialog(VarsDialog vars, SnapshotList snapshotList) {
        super("Snapshots");
        this.vars = vars;
        this.snapshotList = snapshotList;
        this.group = snapshotList.group();

        onResize(() -> { if (w != w()) setup(); });
        setup();
    }

    private float w() {
        return Mathf.floor(Math.min(Core.graphics.getWidth() / Scl.scl(1.05f) - 60f, 600f) / 15f) * 15f;
    }

    public void setup() {
        float h = 110f;
        float w = w();
        float indent = 64f * w / 600f;

        // Skip live view (for now)
        int start = snapshotList.group() ? 0 : 1;

        if (snapshotList.size() > start) {
            cont.clear();
            cont.pane(p -> {
                p.table(list -> {
                    for (int i = start; i < snapshotList.size(); i++) {
                        Snapshot snapshot = snapshotList.snapshot(i);
                        int index = i;

                        list.table(t -> {
                            t.add(createSnapshotButton(snapshot, null, index, group, w))
                                    .padBottom(8f).height(h);
                        });
                        list.row();

                        if (snapshot == expanded) {
                            for (int i2 = 0; i2 < expanded.group().size; i2++) {
                                Snapshot inner = expanded.group().get(i2);
                                int index2 = i2;

                                list.table(t -> {
                                    t.add().pad(0f).width(indent);
                                    t.add(createSnapshotButton(inner, expanded, index2, true, w - indent))
                                            .padBottom(8f).height(h);
                                });
                                list.row();
                            }
                        }
                    }
                }).growY().top().marginRight(15f);
            }).scrollX(false).update(s -> scroll = s.getScrollY()).get().setScrollYForce(scroll);
        } else {
            cont.add("No snapshots found.").row();
        }

        buttons.clear();
        addCloseButton();
    }

    private Button createSnapshotButton(Snapshot snapshot, Snapshot parent, int index, boolean group, float width) {
        Button b = new Button(Styles.grayt);
        b.clearChildren();  // ? - from arc
        b.margin(12f);
        int groupSize = groupSize(snapshot);
        Building build = snapshot.building();

        b.table(t -> {
            if (group) {
                Image image = new Image(new TextureRegionDrawable(build.block.uiIcon),
                        Vars.mobile ? Color.white : Color.lightGray).setScaling(Scaling.fit);
                t.add(image).size(40f).right().top().pad(4f).padRight(14f);
            } else {
                t.image(snapshot.type().icon).color(Pal.accent).size(48f).right().top().padRight(10f);
            }

            // Snapshot properties
            t.table(item -> {
                item.left();
                item.table(text -> {
                    if (group) {
                        text.add(snapshot.buildingDesc()).growX().ellipsis(true).wrap(false).pad(0).top().left();
                    } else {
                        text.add("Snapshot #" + snapshot.id() + ": " + groupSize + (groupSize > 1 ? " blocks" : " block"))
                                .growX().ellipsis(true).wrap(false).pad(0).top().left();
                        text.row();
                        text.add(snapshot.name()).color(Pal.accent).ellipsis(true).growX().ellipsis(true).wrap(false).pad(0).top().left();
                        text.row();
                        text.add(snapshot.time()).color(Color.gray).growX().ellipsis(true).wrap(false).pad(0).top().left();
                    }
                }).pad(0).top().growX();
            }).growX().minWidth(0f);

            t.add().growX();

            // Buttons
            if (!group) {
                t.table(btn -> {
                    btn.button(expanded == snapshot ? Icon.upOpen : Icon.downOpen, Styles.clearNonei, () -> {
                        expanded = expanded == snapshot ? null : snapshot;
                        setup();
                    }).size(50f).disabled(groupSize <= 1);
                }).width(64f).right().top();
            }
        }).top().width(width);

        b.row();

        b.table(t -> {
            float[] d = snapshot.typeDistribution();
            int i = 0;
            for (ValueType type : ValueType.values()) {
                if (d[i] > 0) {
                    t.add(new Image(Tex.whiteui, type.shade)).size(width * d[i], 10f).pad(0f);
                }
                i++;
            }
        }).center().width(width).padTop(10f).margin(8f);

        b.clicked(() -> {
            if (parent == null) {
                vars.setup(snapshotList, index);
            } else {
                vars.setup(SnapshotList.list(parent.group()), index);
            }
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
