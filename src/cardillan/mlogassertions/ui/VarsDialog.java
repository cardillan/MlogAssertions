package cardillan.mlogassertions.ui;

import arc.Core;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Time;
import cardillan.mlogassertions.data.*;
import cardillan.mlogassertions.logic.InstrumentationEngine;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.logic.LCanvas;
import mindustry.logic.Senseable;
import mindustry.ui.FileChooser;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.logic.LogicBlock;

import java.util.Arrays;

import static mindustry.Vars.*;

public class VarsDialog extends BaseDialog {
    public static int updateFrequency = 15;
    public static int significantDigits = 7;

    private static final float reset = 1e10f;
    private static final int live = 0;

    public static boolean hex = false;
    public static boolean sorted = true;
    public static boolean filtered = false;
    public static boolean hideLinks = false;
    public static boolean fullPrecision = false;
    public static int alignment = Align.right;

    private static int lastSnapshotId = -1;

    private Senseable entity;

    // A list of snapshots that can be browsed through
    private Snapshots snapshots;

    private Object[] lastObject;
    private double[] lastMemory;
    private float[] counter;
    private boolean[] updated;
    private int length;

    boolean wasPortrait, compact, paused;
    int rows, cols;

    public VarsDialog(Building entity) {
        this(Snapshots.forBuild(entity));
    }

    private VarsDialog(Snapshots snapshots) {
        super(snapshots.title());
        this.snapshots = snapshots;

        onResize(() -> {
            if (cols != cols() || wasPortrait != Core.graphics.isPortrait() || compact != LCanvas.isCompact()) {
                setup();
            } else {
                rebuildTitle(titleTable);
            }
        });

        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode) {
                switch (keycode) {
                    case pageUp, left -> prev();
                    case pageDown, right -> next();
                    case home -> first();
                    case end -> last();
                    default -> {
                        return false;
                    }
                }
                return true;
            }
        });

        setup();
    }

    public static String escape(String s) {
        return s.indexOf('[') < 0 ? s : s.replace("[", "[[");
    }

    public void setup(Snapshots snapshots) {
        this.snapshots = snapshots;
        setup();
    }

    public void setup(boolean updated) {
        if (updated) {
            setup();
        }
    }

    private int cols() {
        return Math.max(1, (int) (Core.graphics.getWidth() / Scl.scl(snapshots.view().dataType().maxColWidth)));
    }

    private void prev() {
        setup(snapshots.prev());
    }

    private void next() {
        setup(snapshots.next());
    }

    private void first() {
        setup(snapshots.first());
    }

    private void last() {
        setup(snapshots.last());
    }

    private void createSnapshot() {
        SnapshotManager.create(snapshots.view().entity(), "User snapshot");
        rebuildTitle(titleTable);
    }

    private void restoreSnapshot() {
        // TODO Group restore
        if (snapshots.view() instanceof Snapshot snapshot) {
            if (snapshot.writeTo(snapshots.liveData())) {
                ui.showInfo("The processor's state has been restored from the snapshot.");
                setup(Snapshots.forBuild(snapshot.entity()));
                return;
            }
        }
        ui.showErrorMessage("Cannot restore this snapshot: either the snapshot is invalid, or the processor's code has been recompiled.");
    }

    private void removeSnapshot() {
        setup(snapshots.remove());
    }

    private Table titleTable;
    private void rebuildTitle(Table titleTable) {
        if (SnapshotManager.maxSnapshots == 0) return;
        compact = LCanvas.isCompact();

        VariableValues view = snapshots.view();

        this.titleTable = titleTable;

        // Snapshot is null for live view
        Snapshot snapshot = view instanceof Snapshot s ? s : null;
        cont.align(snapshots.group() ? Align.top : Align.center);

        // Snapshot navigation
        titleTable.clear();
        titleTable.table(t -> {
            // Previous
            if (!compact) {
                t.button(Icon.left, Styles.defaulti, this::prev).size(48f, 64f).pad(5f).disabled(!snapshots.hasPrev());
            }

            if (snapshots.group() && !snapshots.recording()) {
                t.image(view.icon()).size(64f).pad(5f);

                t.table(left -> {
                    left.add(view.buildingDescMulti()).growX().ellipsis(true).wrap(false).top().left();
                }).minWidth(0f).pad(5f).padLeft(10f).top().growX();

                t.table(right -> {
                    right.add(snapshots.pos()).color(Pal.accent).top().right().growX().get().setAlignment(Align.right);
                    right.row();
                    right.add(snapshot.time()).color(Color.gray).top().right().growX().get().setAlignment(Align.right);
                }).right().minWidth(0f).pad(5f).padLeft(10f).top().growX();
            } else {
                t.table(title -> {
                    title.table(tBlock -> {
                        tBlock.image(view.icon()).size(iconLarge).padRight(5f);
                        tBlock.table(text -> {
                            text.add(view.entityDesc()).color(Color.white).growX().ellipsis(true).wrap(false).get().setAlignment(Align.left);
                            text.row();
                            text.table(tProperties -> {
                                tProperties.add(view.entityPos()).color(Color.gray).growX().ellipsis(true).wrap(false).get().setAlignment(Align.left);
                                if (snapshot != null) {
                                    tProperties.add(snapshot.time()).color(Color.gray).growX().get().setAlignment(Align.right);
                                }
                            }).top().growX();
                        }).top().growX();
                    }).top().growX();
                    title.row();

                    title.table(tSnapshot -> {
                        if (view.live()) {
                            tSnapshot.add("Live").color(Pal.accent).top().growX().get().setAlignment(Align.left);
                        } else {
                            String name = snapshots.recording() ? snapshot.name()
                                    : "#" + snapshot.id() + ": " + snapshot.type().charIcon + " " + snapshot.name();
                            tSnapshot.add(name).color(Pal.accent).growX().ellipsis(true).wrap(false).get().setAlignment(Align.left);

                            Label l = tSnapshot.add(snapshots.pos()).color(Pal.accent).growX().padLeft(10f).get();
                            l.setAlignment(Align.right);
                            l.update(() -> l.setText(snapshots.pos()));
                        }
                    }).top().growX();
                    title.row();
                }).growX().minWidth(0f).pad(5f);
            }

            // Next snapshot
            if (!compact) {
                t.button(Icon.right, Styles.defaulti, this::next).size(48f, 64f).pad(5f).get().setDisabled(() -> !snapshots.hasNext());
            }
        }).growX().fillX().row();

        // View/snapshot commands
        titleTable.table(t -> {
            t.defaults().size(40f).pad(5f);

            ImageButton.ImageButtonStyle style = Styles.cleari;

            if (compact) {
                t.button(Icon.left, style, this::prev).padRight(15f).disabled(!snapshots.hasPrev());
            }

            t.button(Icon.filters, style, this::viewOptions);
            t.button(Icon.edit, style, this::editCommands);
            t.button(Icon.chartBar, style, () -> {
                if (entity instanceof LogicBlock.LogicBuild build) new ProfileDialog(build).show();
            }).get().setDisabled(() -> view.dataType() != EntityDataType.processor);

            // Play/pause the game
            Image icon = new Image(state.isPlaying() ? Icon.pause : Icon.play);
            var b = new Button();
            b.add(icon).size(64f);
            b.setStyle(style);
            b.clicked(() -> {
                if (state.isPlaying()) {
                    paused = true;
                    state.set(GameState.State.paused);
                    icon.setDrawable(Icon.play);
                } else {
                    paused = false;
                    state.set(GameState.State.playing);
                    icon.setDrawable(Icon.pause);
                }
            });
            t.add(b);

            t.image().growY().width(4f).pad(6f).color(Pal.gray);


            // Select a snapshot from the current block's list of snapshots
            t.button(Icon.folderOpen, style,
                            () -> new SnapshotsDialog(VarsDialog.this, snapshots.group() ? Snapshots.forBuild(entity) : snapshots).show())
                    .get().setDisabled(() -> !SnapshotManager.hasSnapshots(entity));

            // Select a snapshot from a group snapshot
            t.button(Icon.logic, style,
                            () -> new SnapshotsDialog(VarsDialog.this, snapshots.group() ? snapshots : Snapshots.list(snapshot.group())).show())
                    .disabled(snapshot == null || snapshot.group() == null);

            // Create a snapshot
            t.button(Icon.box, style, this::createSnapshot).disabled(snapshot != null);
            t.button(Icon.infoCircle, style, this::help);

            if (compact) {
                t.button(Icon.right, style, this::next).padLeft(15f).get().setDisabled(() -> !snapshots.hasNext());
            }
        }).growX().fillX();

        titleTable.addListener(new InputListener() {
            private float startX;
            private float startY;
            private boolean dragging;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                startX = x;
                startY = y;
                dragging = true;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (!dragging) return;
                dragging = false;

                float dx = x - startX;
                float dy = y - startY;

                // Only treat predominantly horizontal movement as a swipe.
                if (Math.abs(dx) < 80f || Math.abs(dx) < 1.5f * Math.abs(dy)) return;

                if (dx < 0) {
                    next();
                } else {
                    prev();
                }
            }
        });
    }

    private void setup() {
        title.setText(snapshots.title());

        // Current view
        VariableValues view = snapshots.view();
        view.setView(false, false, false);
        lastSnapshotId = view instanceof Snapshot s ? s.id() : -1;
        entity = view.entity();

        length = view.size();
        counter = new float[length];
        lastObject = new Object[length];
        lastMemory = new double[length];
        updated = new boolean[length];

        view.setView(sorted, filtered, hideLinks);
        Arrays.fill(counter, reset);

        buttons.clear();
        cont.clear();

        if (SnapshotManager.maxSnapshots > 0) {
            cont.table(this::rebuildTitle).width(Math.min(700f, LCanvas.getTargetWidth())).fillX();
            cont.row();
        }

        cont.pane(p -> {
            p.margin(10f).marginTop(0f);
            p.table(Tex.button, t -> {
                t.defaults().fillX().height(45f);
                length = view.size();
                cols = cols();
                rows = (length + cols - 1) / cols;

                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        int index = col * rows + row;
                        if (index >= length) break;

                        Color varColor = Pal.gray;
                        float stub = 8f, mul = 0.5f, pad = 4;

                        t.add(new Image(Tex.whiteui, varColor.cpy().mul(mul))).width(stub);
                        t.stack(new Image(Tex.whiteui, varColor), new Label(() -> view.label(index, hex)) {{
                            setAlignment(Align.center);
                            setColor(Pal.accent);
                            setStyle(Styles.outlineLabel);
                        }}).padRight(pad);

                        t.add(new Image(Tex.whiteui, Pal.gray.cpy().mul(mul))).width(stub);
                        EllipsisLabel valueLabel = new EllipsisLabel("");
                        valueLabel.act(1f);
                        valueLabel.maxLines(2);
                        Cell<Table> val = t.table(Tex.pane, out -> out.add(valueLabel).style(Styles.outlineLabel)
                                    .padLeft(4).padRight(4).width(compact ? 140f : 220f)
                        ).padRight(pad);

                        ValueType valueType = view.type(index);
                        Image halfShade = t.add(new Image(Tex.whiteui, valueType.darkShade)).width(stub).get();
                        Image fullShade = new Image(Tex.whiteui, valueType.shade);
                        Label typeLabel = new Label("");
                        typeLabel.setAlignment(Align.center);
                        typeLabel.setStyle(Styles.outlineLabel);
                        t.stack(fullShade, typeLabel).minWidth(120f).get();

                        valueLabel.update(() -> {
                            if ((counter[index] += Time.delta) >= updateFrequency) {
                                Object objval = view.obj(index);
                                double numval = view.num(index);
                                if (counter[index] >= reset || objval != lastObject[index] || numval != lastMemory[index]) {
                                    lastObject[index] = objval;
                                    lastMemory[index] = numval;

                                    String text = view.formatted(index, hex, fullPrecision ? 16 : significantDigits);
                                    ValueType type = view.type(index);
                                    valueLabel.setText(type == ValueType.string ? escape(text) : text);
                                    valueLabel.setAlignment(alignment);
                                    typeLabel.setText(type.paddedTitle);

                                    halfShade.setColor(type.darkShade);
                                    fullShade.setColor(type.shade);

                                    updated[index] = counter[index] < reset;
                                    valueLabel.setColor(updated[index] ? Pal.accent : Color.white);
                                } else if (!paused) {
                                    updated[index] = false;
                                    valueLabel.setColor(Color.white);
                                }
                                counter[index] = 0f;
                            }
                        });
                    }
                    t.row();
                    t.add().growX().colspan(6).height(4).row();
                }
            });
        });

        // Dialog buttons
        buttons.defaults().size(200f, 64f);

        if (SnapshotManager.maxSnapshots == 0) {
            // No snapshots: no commands above the list
            if (snapshots.view().dataType() == EntityDataType.processor) {
                buttons.button("@back", Icon.left, this::hide).name("back");
                buttons.button("@logic.globals", Icon.list, () -> LogicDialogAddon.globalsDialog.show());
                if (Core.graphics.isPortrait()) buttons.row();
                buttons.button("@varsdialog.options", Icon.filters, this::viewOptions).name("options");
                buttons.button("@edit", Icon.edit, () -> editCommands()).name("edit");
            } else {
                buttons.button("@back", Icon.left, this::hide).name("back");
                buttons.button("@edit", Icon.edit, () -> editCommands()).name("edit");
                if (Core.graphics.isPortrait()) buttons.row();
                buttons.button("@varsdialog.hex", Styles.squareTogglet, () -> refreshView(hex = !hex)).name("hex").checked(hex);
                if (significantDigits < 16) {
                    buttons.button("@varsdialog.fullprecision", Styles.squareTogglet, () -> refreshView(fullPrecision = !fullPrecision))
                            .name("fullprecision").checked(fullPrecision);
                }
            }
        } else {
            // Snapshots are enabled: most commands are displayed above the list
            buttons.button("@back", Icon.left, this::hide).name("back");
            if (snapshots.view().dataType() == EntityDataType.processor) {
                buttons.button("@logic.globals", Icon.list, () -> LogicDialogAddon.globalsDialog.show());
            }
        }

        wasPortrait = Core.graphics.isPortrait();
        addCloseListener();
    }

    private void help(Table t, TextureRegionDrawable icon, String text) {
        help(t, icon, null, text);
    }

    private void help(Table t, TextureRegionDrawable icon1, TextureRegionDrawable icon2, String text) {
        if (icon2 == null) {
            t.image(icon1).colspan(2).color(Color.lightGray);
        } else {
            t.image(icon1).color(Color.lightGray).padRight(4f);
            t.image(icon2).color(Color.lightGray).padLeft(4f);
        }

        t.add(text).color(Color.lightGray).width(350f).minWidth(0f).wrap().row();
    }

    private void help() {
        BaseDialog dialog = new BaseDialog("Help");
        dialog.titleTable.visible(() -> false).setHeight(0f);
        dialog.cont.pane(p -> {
            p.table(Tex.button, t -> {
                TextButton.TextButtonStyle style = Styles.squareTogglet;
                t.defaults().fillX().pad(6f, 15f, 6f, 15f).left();

                t.add("Available commands").colspan(3).color(Pal.accent).center().padBottom(10f).get().setAlignment(Align.center);
                t.row();

                help(t, Icon.left, Icon.right, mobile
                        ? "Navigate to the previous/next snapshot (or swipe the header left/right)."
                        : "Navigate to the previous/next snapshot (also the PgUp/PgDn and Home/End keys).");
                help(t, Icon.filters, "Customize the view (for the duration of the session).");
                help(t, Icon.edit, "Export, import or modify the data of this block.");
                help(t, Icon.chartBar, "Profile the current processor's execution.");
                help(t, Icon.pause, Icon.play, "Pause/resume the game.");
                help(t, Icon.folderOpen, "Show a list of this block's snapshots.");
                help(t, Icon.logic, "Navigate to a different block contained in this snapshot.");
                help(t, Icon.box, "Create a new snapshot of this block and all connected blocks.");
                help(t, Icon.infoCircle, "Show this help.");

                t.add("Edit commands").colspan(3).color(Pal.accent).center().padBottom(15F).get().setAlignment(Align.center);
                t.row();

                help(t, Icon.cancel, "Reset memory block to all zeroes.");
                help(t, Icon.copy, "Copy values to the clipboard.");
                help(t, Icon.upload, "Export values to a file.");
                help(t, Icon.paste, "Import memory block values from Clipboard.");
                help(t, Icon.download, "Import memory block values from a file.");
                help(t, Icon.undo, "Restore the current processor or memory block's state from the selected snapshot.");
                help(t, Icon.trash, "Delete the current snapshot.");
                help(t, Icon.trash, "Delete all snapshots of this block (they may still be accessible as part of connected or global snapshots).");

                t.defaults().size(180f, 60f).growX().colspan(3).pad(15f);
                t.button("@back", Icon.left, Styles.defaultt, dialog::hide).center().marginLeft(12f).name("back");
            }).pad(10f).padRight(30f);
        });

        dialog.addCloseListener();
        dialog.show();
    }

    private void viewOptions() {
        BaseDialog dialog = new BaseDialog("@varsdialog.options");
        dialog.titleTable.visible(() -> false);
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButton.TextButtonStyle style = Styles.squareTogglet;
                t.defaults().height(45f).growX().fillX().uniformX().colspan(3).pad(3f).left();

                t.add("View customization").colspan(6).color(Pal.accent).center().padBottom(10f).get().setAlignment(Align.center);
                t.row();

                ButtonGroup<TextButton> hexGroup = new ButtonGroup<>();
                t.button("@varsdialog.dec", style, () -> refreshView(hex = false)).name("dec").group(hexGroup).checked(!hex);
                t.button("@varsdialog.hex", style, () -> refreshView(hex = true)).name("hex").group(hexGroup).checked(hex);
                t.row();
                ButtonGroup<TextButton> sortedGroup = new ButtonGroup<>();
                t.button("@varsdialog.sorted", style, () -> updateView(sorted = true)).name("sorted").group(sortedGroup).checked(sorted);
                t.button("@varsdialog.unsorted", style, () -> updateView(sorted = false)).name("unsorted").group(sortedGroup).checked(!sorted);
                t.row();
                ButtonGroup<TextButton> tempsGroup = new ButtonGroup<>();
                t.button("@varsdialog.showall", style, () -> updateView(filtered = false)).name("showall").group(tempsGroup).checked(!filtered);
                t.button("@varsdialog.hidetemps", style, () -> updateView(filtered = true)).name("hidetemps").group(tempsGroup).checked(filtered);
                t.row();
                ButtonGroup<TextButton> linksGroup = new ButtonGroup<>();
                t.button("@varsdialog.showlinks", style, () -> updateView(hideLinks = false)).name("showlinks").group(linksGroup).checked(!hideLinks);
                t.button("@varsdialog.hidelinks", style, () -> updateView(hideLinks = true)).name("hidelinks").group(linksGroup).checked(hideLinks);
                t.row();
                if (significantDigits < 16) {
                    ButtonGroup<TextButton> precisionGroup = new ButtonGroup<>();
                    t.button(Core.bundle.format("varsdialog.limitedprecission", significantDigits), style,
                            () -> updateView(fullPrecision = false)).name("limitedprecission").group(precisionGroup).checked(!fullPrecision);
                    t.button("@varsdialog.fullprecision", style, () -> updateView(fullPrecision = true)).name("hidelinks").group(precisionGroup).checked(fullPrecision);
                    t.row();
                }
                ButtonGroup<TextButton> alignmentGroup = new ButtonGroup<>();
                t.defaults().height(45f).growX().fillX().uniformX().colspan(2).pad(3f).left();
                t.button("Left", style, () -> updateView(alignment = Align.left)).group(alignmentGroup).checked(alignment == Align.left);
                t.button("Center", style, () -> updateView(alignment = Align.center)).group(alignmentGroup).checked(alignment == Align.center);
                t.button("Right", style, () -> updateView(alignment = Align.right)).group(alignmentGroup).checked(alignment == Align.right);
                t.row();

                t.defaults().height(60f).growX().fillX().uniformX().colspan(6).pad(15f);
                t.button("@back", Icon.left, Styles.flatt, dialog::hide).marginLeft(12f).name("back");
            }).width(350f).growX();
        });

        dialog.addCloseListener();
        dialog.show();
    }

    private void editCommands() {
        BaseDialog dialog = new BaseDialog("@edit");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButton.TextButtonStyle style = Styles.flatt;
                t.defaults().size(360f, 60f).left();

                if (snapshots.view().dataType() == EntityDataType.memory && snapshots.view().live()) {
                    t.button("@varsdialog.clearmemory", Icon.cancel, style, () -> {
                        snapshots.view().clear();
                        Arrays.fill(counter, reset / 2);  // Animate change
                        dialog.hide();
                    }).marginLeft(12f).row();
                }

                t.button("@varsdialog.copyvariables", Icon.copy, style, () -> {
                    Core.app.setClipboardText(MemoryText.write(snapshots.view(), hex));
                    dialog.hide();
                }).marginLeft(12f).row();

                t.button("@varsdialog.exportvariablesfile", Icon.upload, style, () -> {
                    FileChooser.save("txt").name("memory_export.txt").submit(file -> {
                        try {
                            file.writeString(MemoryText.write(snapshots.view(), hex));
                        } catch (Exception e) {
                            Log.err("[Mlog Dev Tools] Error writing file: ", e);
                            ui.showErrorMessage("Error writing file " + file.absolutePath());
                        }
                    });
                    dialog.hide();
                }).marginLeft(12f).row();

                if (snapshots.view().dataType() == EntityDataType.memory && snapshots.view().live()) {
                    t.button("@varsdialog.importvariables", Icon.paste, style, () -> {
                        String text = Core.app.getClipboardText();
                        if (text == null || text.length() == 0) {
                            ui.showErrorMessage("The clipboard is empty.");
                        } else {
                            importData(text);
                        }
                        dialog.hide();
                    }).marginLeft(12f).row();

                    t.button("@varsdialog.importvariablesfile", Icon.download, style, () -> {
                        FileChooser.open("txt").submit(file -> {
                            try {
                                String text = file.readString();

                                if (text == null) {
                                    ui.showErrorMessage("Error reading file " + file.absolutePath());
                                } else {
                                    importData(text);
                                }
                            } catch (Exception e) {
                                Log.err("[Mlog Dev Tools] Error reading file: ", e);
                                ui.showErrorMessage("Error reading file " + file.absolutePath());
                            }
                        });
                        dialog.hide();
                    }).marginLeft(12f).row();
                }

                if (snapshots.view().dataType() != EntityDataType.entity && snapshots.view() instanceof Snapshot snapshot) {
                    t.button("Restore current snapshot", Icon.undo, style, this::restoreSnapshot).marginLeft(12f).row();
                }

                if (snapshots.canRemove()) {
                    // Can't remove snapshots from snapshot groups
                    t.button("Delete current snapshot", Icon.trash, style, this::removeSnapshot).marginLeft(12f).row();
                }

                t.button("Delete all snapshots of this block", Icon.trash, style, () -> {
                    SnapshotManager.deleteEntity(snapshots.view().entity());
                    dialog.hide();
                    first();
                }).marginLeft(12f).row();

                t.button("@back", Icon.left, style, dialog::hide).padTop(10f).marginLeft(12f).name("back");
            });
        });

        dialog.addCloseListener();
        dialog.show();
    }

    private void importData(String text) {
        String error = MemoryText.validate(text, length);
        if (error == null) {
            error = MemoryText.read(text, length, snapshots.view());
        }

        if (error != null) {
            ui.showErrorMessage(Core.bundle.format("varsdialog.importfailed", error));
        } else {
            Arrays.fill(counter, reset / 2);
        }
    }

    private void refreshView(boolean update) {
        Arrays.fill(counter, reset);
    }

    private void updateView(int update) {
        updateView(true);
    }

    private void updateView(boolean update) {
        snapshots.view().setView(sorted, filtered, hideLinks);
        if (snapshots.view().size() != length) {
            setup();
        } else {
            Arrays.fill(counter, reset);
        }
    }
}
