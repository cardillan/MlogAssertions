package cardillan.mlogassertions.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Scl;
import arc.struct.Queue;
import arc.struct.Seq;
import cardillan.mlogassertions.data.MemoryVars;
import cardillan.mlogassertions.data.ProcessorVars;
import cardillan.mlogassertions.data.Snapshot;
import cardillan.mlogassertions.data.VariableValues;
import mindustry.gen.Building;
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
    Queue<Snapshot> snapshots;

    public SnapshotsDialog(VarsDialog vars, Queue<Snapshot> snapshots) {
        super("Snapshots");
        this.vars = vars;
        this.snapshots = snapshots;
        setup(false);
    }

    public void setup(boolean update) {
        if (update && snapshots.isEmpty()) {
            hide();
            return;
        }

        float h = 80f;
        float w = Math.min(Core.graphics.getWidth() / Scl.scl(1.05f) - Scl.scl(28f), 520f);

        if (snapshots != null && snapshots.size > 0) {
            cont.clear();
            cont.pane(p -> {
                p.table(t -> {
                    for (int i = 0; i < snapshots.size; i++) {
                        Snapshot snapshot = snapshots.get(i);
                        int index = i;
                        t.button(b -> {
                            b.top().left();
                            b.margin(12f);
                            b.defaults().left().top();
                            b.table(item -> {
                                item.left();
                                item.table(text -> {
                                    text.add(snapshot.name()).color(Pal.accent).ellipsis(true).top().width(300f).growX().left();
                                    text.row();
                                    text.add(dateFormat.format(new Date(snapshot.timestamp()))).color(Color.gray).width(300f).growX().left();
                                }).top().growX();
                                item.add().growX();
                            }).growX().growY().left();

                        }, Styles.grayt, () -> {
                            vars.setup(index);
                            hide();
                        }).size(w, h).growX().pad(4f).padTop(8f).row();
                    }
                });
            }).scrollX(false).grow().pad(12f);
        } else {
            cont.add("No snapshots found.").row();
        }

        buttons.clear();
        addCloseButton();
    }

    private VariableValues createLiveView(Building b) {
        if (b instanceof MemoryBlock.MemoryBuild build) return new MemoryVars(build);
        if (b instanceof LogicBlock.LogicBuild build) return new ProcessorVars(build);
        return null;
    }
}
