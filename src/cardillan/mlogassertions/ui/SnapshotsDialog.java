package cardillan.mlogassertions.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Scaling;
import cardillan.mlogassertions.data.*;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.MemoryBlock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static mindustry.Vars.mods;

public class SnapshotsDialog extends BaseDialog {
    static final DateFormat dateFormat = SimpleDateFormat.getTimeInstance();

    VarsDialog vars;
    Seq<Snapshot> snapshots;
    Snapshot expanded = null;
    boolean group;

    public SnapshotsDialog(VarsDialog vars, Seq<Snapshot> snapshots) {
        super("Snapshots");
        this.vars = vars;
        this.snapshots = snapshots;
        this.group = true;
        setup();
    }

    public SnapshotsDialog(VarsDialog vars, Queue<Snapshot> snapshots) {
        super("Snapshots");
        this.vars = vars;
        this.snapshots = new Seq<>();
        snapshots.each(s -> this.snapshots.add(s));
        this.group = false;
        setup();
    }

    public void setup() {
        float h = 90f;
        float w = Math.min(Core.graphics.getWidth() / Scl.scl(1.05f) - Scl.scl(28f), 520f);

        if (snapshots != null && snapshots.size > 0) {
            cont.clear();
            cont.pane(p -> {
                p.table(list -> {
                    for (int i = 0; i < snapshots.size; i++) {
                        Snapshot snapshot = snapshots.get(i);
                        int index = i;

                        list.table(item -> {
                            item.add(createSnapshotButton(snapshot, index, group)).grow().pad(4f).padTop(8f);
                        }).size(w, h);
                        list.row();

                        if (snapshot == expanded) {
                            for (int i2 = 0; i2 < expanded.group().size; i2++) {
                                Snapshot inner = expanded.group().get(i2);

                                list.table(item -> {
                                    item.add("").width(64f);
                                    item.add(createSnapshotButton(inner, index, true)).grow().pad(4f).padTop(8f);
                                }).size(w, h);
                                list.row();
                            }
                        }
                    }
                });
            }).scrollX(false).grow().pad(12f);
        } else {
            cont.add("No snapshots found.").row();
        }

        buttons.clear();
        addCloseButton();
    }

    private Button createSnapshotButton(Snapshot snapshot, int index, boolean group) {
        Button b = new Button(Styles.grayt);
        b.clearChildren();  // ? - from arc
        b.margin(12f).left().top().defaults().left().top();
        int groupSize = groupSize(snapshot);
        Building build = snapshot.building();

        // A group snapshot is indented
        if (group) {
            Image image = new Image(new TextureRegionDrawable(build.block.uiIcon),
                    Vars.mobile ? Color.white : Color.lightGray).setScaling(Scaling.fit);
            b.add(image).size(Vars.iconXLarge).right().top().padRight(10f);
        } else {
            b.image(snapshot.type().icon).color(Pal.accent).size(Vars.iconXLarge).right().top().padRight(10f);
        }

        // Snapshot properties
        b.table(item -> {
            item.left();
            item.table(text -> {
                if (group) {
                    text.add(build.block.name + LogicVariableValues.pos(build.x(), build.y())).growX().left();
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
            b.table(btn -> {
                btn.button(expanded == snapshot ? Icon.upOpen : Icon.downOpen, Styles.clearNonei, () -> {
                    expanded = expanded == snapshot ? null : snapshot;
                    setup();
                }).size(50f).disabled(groupSize <= 1);
            }).width(64f).right().top();
        }

        b.clicked(() -> {
            vars.setup(index);
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
