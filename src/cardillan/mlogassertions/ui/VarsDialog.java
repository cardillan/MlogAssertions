package cardillan.mlogassertions.ui;

import arc.Core;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Time;
import cardillan.mlogassertions.data.*;
import mindustry.core.GameState;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.logic.LCanvas;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.Arrays;

import static mindustry.Vars.*;

public class VarsDialog extends BaseDialog {
    public static int updateFrequency = 15;
    public static int significantDigits = 7;

    public static final float reset = 1e10f;
    private static final int live = 0;

    static boolean hex = false;
    static boolean sorted = false;
    static boolean filtered = false;
    static boolean hideLinks = false;
    static boolean fullPrecision = false;

    private Building building;

    // A list of snapshots that can be browsed through
    private SnapshotList snapshotList;
    private int index;

    // Cuurrently displayed snapshots
    private VariableValues liveData;
    private VariableValues view;
    private Object[] lastObject;
    private double[] lastMemory;
    private float[] counter;
    private boolean[] updated;
    private int length;

    boolean wasPortrait;
    int rows, cols;

    public VarsDialog(Building building) {
        this(SnapshotList.list(building));
    }

    private VarsDialog(SnapshotList snapshotList) {
        super(snapshotList.title());
        this.snapshotList = snapshotList;
        this.index = 0;

        onResize(() -> {
            if (cols != cols() || wasPortrait != Core.graphics.isPortrait()) {
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
                    case home -> setup(0);
                    case end -> setup(snapshotList.size() - 1);
                    default -> {
                        return false;
                    }
                }
                return true;
            }
        });

        setup();
    }

    private int cols() {
        return Math.max(1, (int) (Core.graphics.getWidth() / Scl.scl(view.maxColWidth())));
    }

    public void setup(SnapshotList snapshotList, int index) {
        this.snapshotList = snapshotList;
        this.index = index;
        setup();
    }

    private Table titleTable;

    public void setup(int index) {
        this.index = Math.min(index, snapshotList.size() - 1);
        view = snapshotList.view(index);
        view.setView(sorted, filtered, hideLinks);

        if (snapshotList.group()) cont.top();
        else cont.center();

        if (view.building() != building || view.size() != length) {
            setup();
        } else {
            rebuildTitle(titleTable);
            Arrays.fill(counter, reset);
        }
    }

    private void prev() {
        if (index > 0) setup(index - 1);
    }

    private void next() {
        if (index < snapshotList.size() - 1) setup(index + 1);
    }

    private void rebuildTitle(Table titleTable) {
        if (Snapshots.maxSnapshots == 0) return;
        boolean compact = LCanvas.isCompact();

        this.titleTable = titleTable;

        // Snapshot is null for live view
        Snapshot snapshot = snapshotList.snapshot(index);
        if (snapshotList.group()) cont.top();
        else cont.center();

        // Snapshot navigation
        titleTable.clear();
        titleTable.table(t -> {
            // Previous
            if (!compact) {
                t.button(Icon.leftOpen, Styles.defaulti, this::prev).size(48f, 64f).pad(5f).disabled(index == 0);
            }

            if (snapshotList.group()) {
                t.image(view.building().block.uiIcon).size(64f).pad(5f);

                t.table(left -> {
                    left.add(view.buildingDescMulti()).growX().ellipsis(true).wrap(false).top().left();
                }).minWidth(0f).pad(5f).padLeft(10f).top().growX();

                t.table(right -> {
                    right.add((index + 1) + "/" + snapshotList.size()).color(Pal.accent).top().right().growX().get().setAlignment(Align.right);
                    right.row();
                    right.add(snapshot == null ? "" : snapshot.time()).color(Color.gray).top().right().growX().get().setAlignment(Align.right);
                }).right().minWidth(0f).pad(5f).padLeft(10f).top().growX();
            } else {
                //float w = 64+5+300+5+150+10;
                t.table(title -> {
                    title.table(tBlock -> {
                        tBlock.image(view.building().block.uiIcon).size(iconLarge).padRight(5f);
                        tBlock.table(text -> {
                            text.add(view.buildingDesc()).color(Color.white).growX().ellipsis(true).wrap(false).get().setAlignment(Align.left);
                            text.row();
                            text.table(tProperties -> {
                                tProperties.add(view.buildingPos()).color(Color.gray).growX().ellipsis(true).wrap(false).get().setAlignment(Align.left);
                                if (index > 0) {
                                    tProperties.add(snapshot.time()).color(Color.gray).growX().get().setAlignment(Align.right);
                                }
                            }).top().growX();
                        }).top().growX();
                    }).top().growX();
                    title.row();

                    title.table(tSnapshot -> {
                        if (index == 0) {
                            tSnapshot.add("Live").color(Pal.accent).top().growX().get().setAlignment(Align.left);
                        } else {
                            tSnapshot.add("#" + snapshot.id() + ": " + snapshot.name()).color(Pal.accent).growX().ellipsis(true).wrap(false).get().setAlignment(Align.left);

                            Label l = tSnapshot.add(index + "/" + (snapshotList.size() - 1)).color(Pal.accent).growX().padLeft(10f).get();
                            l.setAlignment(Align.right);
                            l.update(() -> {
                                if (index < snapshotList.size() && snapshotList.snapshot(index) == snapshot) {
                                    l.setText(index + "/" + (snapshotList.size() - 1));
                                } else {
                                    // Try to find our snapshot
                                    for (int i = 0; i < snapshotList.size(); i++) {
                                        if (snapshotList.snapshot(i) == snapshot) {
                                            index = i;
                                            l.setText(i + "/" + (snapshotList.size() - 1));
                                            return;
                                        }
                                    }
                                    // Not found
                                    l.setText("--/" + (snapshotList.size() - 1));
                                }
                            });
                        }
                    }).top().growX();
                    title.row();
                }).growX().minWidth(0f).pad(5f);
            }

            // Next snapshot
            if (!compact) {
                t.button(Icon.rightOpen, Styles.defaulti, this::next).size(48f, 64f).pad(5f).get().setDisabled(() -> index >= snapshotList.size() - 1);
            }
        }).growX().fillX().row();

        // View/snapshot commands
        titleTable.table(t -> {
            t.defaults().size(40f).pad(5f);

            ImageButton.ImageButtonStyle style = Styles.cleari;

            if (compact) {
                t.button(Icon.leftOpen, style, this::prev).padRight(15f);
            }

            t.button(Icon.filters, style, this::viewOptions);
            t.button(Icon.edit, style, this::editCommands);

            // Play/pause the game
            Image icon = new Image(state.isPlaying() ? Icon.pause : Icon.play);
            var b = new Button();
            b.add(icon).size(64f);
            b.setStyle(style);
            b.clicked(() -> {
                if (state.isPlaying()) {
                    state.set(GameState.State.paused);
                    icon.setDrawable(Icon.play);
                } else {
                    state.set(GameState.State.playing);
                    icon.setDrawable(Icon.pause);
                }
            });
            t.add(b);

            t.image().growY().width(4f).pad(6f).color(Pal.gray);


            // Select a snapshot from the current block's list of snapshots
            t.button(Icon.folderOpen, style,
                            () -> new SnapshotsDialog(VarsDialog.this, snapshotList.group() ? SnapshotList.list(building) : snapshotList).show())
                    .get().setDisabled(() -> snapshotList.size() <= 1);

            // Select a snapshot from a group snapshot
            t.button(Icon.logic, style,
                            () -> new SnapshotsDialog(VarsDialog.this, snapshotList.group() ? snapshotList : SnapshotList.list(snapshot.group())).show())
                    .disabled(snapshot == null || snapshot.group() == null);

            // Restore a snapshot
            // TODO Group restore
            t.button(Icon.download, style, () -> {
                if (snapshot.writeTo(liveData)) {
                    ui.showInfo("The processor's state has been restored from the snapshot.");
                    setup(live);
                } else {
                    ui.showErrorMessage("Cannot restore this snapshot: either the snapshot is invalid, or the processor's code has been recompiled.");
                }
            }).disabled(snapshot == null || snapshot.dataType() == BlockDataType.properties);

            // Create a snapshot
            t.button(Icon.box, style, () -> {
                Snapshots.create(view.building(), "User snapshot");
                rebuildTitle(titleTable);
            }).disabled(snapshot != null);

            // Can't remove snapshots from snapshot groups
            t.button(Icon.trash, style, () -> {
                snapshotList.remove(index);
                if (index >= snapshotList.size()) index--;
                rebuildTitle(titleTable);
            }).disabled(snapshotList.group() || index == 0);

            if (compact) {
                t.button(Icon.rightOpen, style, this::next).padLeft(15f);
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
        title.setText(snapshotList.title());

        // Current view
        view = snapshotList.view(index);
        view.setView(false, false, false);

        length = view.size();
        counter = new float[length];
        lastObject = new Object[length];
        lastMemory = new double[length];
        updated = new boolean[length];

        view.setView(sorted, filtered, hideLinks);
        Arrays.fill(counter, reset);

        // Always obtain independent live data
        if (view.building() != building) {
            building = view.building();
            liveData = Snapshots.liveView(building);
        }

        buttons.clear();
        cont.clear();

        if (Snapshots.maxSnapshots > 0) {
            cont.table(this::rebuildTitle).width(Math.min(600f, LCanvas.getTargetWidth())).fillX();
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
                        Cell<Table> val = t.table(Tex.pane, out -> {
                            Label label = out.add("").style(Styles.outlineLabel).padLeft(4).padRight(4).width(LCanvas.isCompact() ? 140f : 220f).wrap().get();
                            label.setAlignment(Align.right);
                            label.act(1f);
                            label.setEllipsis(true);
                        }).padRight(pad);
                        Label valueLabel = (Label) val.get().getCells().first().get();

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
                                    valueLabel.setText(text);
                                    typeLabel.setText(type.paddedTitle);

                                    halfShade.setColor(type.darkShade);
                                    fullShade.setColor(type.shade);

                                    updated[index] = counter[index] < reset;
                                    valueLabel.setColor(updated[index] ? Pal.accent : Color.white);
                                } else {
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

        if (Snapshots.maxSnapshots == 0) {
            // No snapshots: no commands above the list
            if (liveData.dataType() == BlockDataType.processor) {
                buttons.button("@back", Icon.left, this::hide).name("back");
                buttons.button("@logic.globals", Icon.list, () -> LogicDialogAddon.globalsDialog.show());
                if (Core.graphics.isPortrait()) buttons.row();
                buttons.button("@varsdialog.options", Icon.filters, this::viewOptions).name("options");
                buttons.button("@edit", Icon.edit, this::editCommands).name("edit");
            } else {
                buttons.button("@back", Icon.left, this::hide).name("back");
                buttons.button("@edit", Icon.edit, this::editCommands).name("edit");
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
            if (liveData.dataType() == BlockDataType.processor) {
                buttons.button("@logic.globals", Icon.list, () -> LogicDialogAddon.globalsDialog.show());
            }
        }

        wasPortrait = Core.graphics.isPortrait();
        addCloseListener();
    }

    private void viewOptions() {
        BaseDialog dialog = new BaseDialog("@varsdialog.options");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButton.TextButtonStyle style = Styles.squareTogglet;
                t.defaults().size(160f, 45f).pad(3f).left();

                ButtonGroup<TextButton> hexGroup = new ButtonGroup<>();
                t.button("@varsdialog.dec", style, () -> refreshView(hex = false)).name("dec").group(hexGroup).checked(!hex);
                t.button("@varsdialog.hex", style, () -> refreshView(hex = true)).name("hex").group(hexGroup).checked(hex);
                t.row();
                ButtonGroup<TextButton> sortedGroup = new ButtonGroup<>();
                t.button("@varsdialog.unsorted", style, () -> updateView(sorted = false)).name("unsorted").group(sortedGroup).checked(!sorted);
                t.button("@varsdialog.sorted", style, () -> updateView(sorted = true)).name("sorted").group(sortedGroup).checked(sorted);
                t.row();
                ButtonGroup<TextButton> tempsGroup = new ButtonGroup<>();
                t.button("@varsdialog.showall", style, () -> updateView(filtered = false)).name("showall").group(tempsGroup).checked(!filtered);
                t.button("@varsdialog.hidetemps", style, () -> updateView(filtered = true)).name("hidetemps").group(tempsGroup).checked(filtered);
                t.row();
                ButtonGroup<TextButton> linksGroup = new ButtonGroup<>();
                t.button("@varsdialog.showlinks", style, () -> updateView(hideLinks = false)).name("showlinks").group(linksGroup).checked(!hideLinks);
                t.button("@varsdialog.hidelinks", style, () -> updateView(hideLinks = true)).name("hidelinks").group(linksGroup).checked(hideLinks);
                t.row();

                t.defaults().size(323f, 60f).padTop(25f).padBottom(15f);
                t.button("@back", Icon.left, Styles.flatt, dialog::hide).colspan(2).name("back");
            });
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

                if (liveData.dataType() == BlockDataType.memory) {
                    t.button("@varsdialog.clearmemory", Icon.cancel, style, () -> {
                        view.clear();
                        Arrays.fill(counter, reset / 2);
                        dialog.hide();
                    }).marginLeft(12f).row();
                }

                t.button("@varsdialog.copyvariables", Icon.copy, style, () -> {
                    Core.app.setClipboardText(MemoryText.write(view, hex));
                    dialog.hide();
                }).marginLeft(12f).row();

                if (liveData.dataType() == BlockDataType.processor) {
                    t.button("@varsdialog.copyprintbuffer", Icon.copy, style, () -> {
                        String text = "Printbuffer contents:\n" + view.textBuffer();
                        Core.app.setClipboardText(text);
                        dialog.hide();
                    }).marginLeft(12f).row();
                }

                if (liveData.dataType() == BlockDataType.memory) {
                    t.button("@varsdialog.importvariables", Icon.download, style, () -> {
                        String text = Core.app.getClipboardText();
                        String error = MemoryText.validate(text, length);
                        if (error == null) error = MemoryText.read(text, length, view);

                        if (error != null) {
                            ui.showInfoFade(Core.bundle.format("varsdialog.importfailed", error));
                            return;
                        }

                        Arrays.fill(counter, reset / 2);
                        dialog.hide();
                    }).marginLeft(12f).row();
                }

                t.button("Delete all snapshots", Icon.trash, style, () -> {
                    Snapshots.deleteBuilding(view.building());
                    dialog.hide();
                    setup(live);
                }).marginLeft(12f).row();

                t.button("@back", Icon.left, style, dialog::hide).padTop(10f).name("back");
            });
        });

        dialog.addCloseListener();
        dialog.show();
    }

    private void refreshView(boolean update) {
        Arrays.fill(counter, reset);
    }

    private void updateView(boolean update) {
        view.setView(sorted, filtered, hideLinks);
        if (view.size() != length) {
            setup();
        } else {
            Arrays.fill(counter, reset);
        }
    }
}
