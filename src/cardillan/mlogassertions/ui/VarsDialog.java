package cardillan.mlogassertions.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Time;
import cardillan.mlogassertions.data.*;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.Arrays;
import java.util.Date;

public class VarsDialog extends BaseDialog {
    public static final float RESET = 1e10f;

    static boolean hex = false;
    static boolean sorted = false;
    static boolean hideTemps = false;
    static boolean hideLinks = false;

    private final boolean processor;

    private Queue<Snapshot> snapshots;
    private int index;

    private VariableValues data;
    private VariableValues view;
    private Object[] lastObject;
    private double[] lastMemory;
    private float[] counter;
    private boolean[] updated;
    private int length;

    boolean wasPortrait;
    int rows, cols;

    public VarsDialog(VariableValues data) {
        this(data, Snapshots.get(data.building()), -1);
    }

    public VarsDialog(VariableValues data, Queue<Snapshot> snapshots, int index) {
        super(data.processor() ? "@variables" : "@varsdialog.memory");
        this.processor = data.processor();
        this.snapshots = snapshots;
        this.index = index;
        this.data = data;

        onResize(() -> {
            if (cols != cols() || wasPortrait != Core.graphics.isPortrait()) {
                setup();
            }
        });

        setup();
    }

    private int cols() {
        return processor ? 1 : Math.max(1, (int) (Core.graphics.getWidth() / Scl.scl(550f)));
    }

    public void setup(int index) {
        this.index = index;
        setup();
    }

    public void setup() {
        view = index < 0 ? data : snapshots.get(index);
        view.setView(false, false, false);

        length = view.size();
        counter = new float[length];
        lastObject = new Object[length];
        lastMemory = new double[length];
        updated = new boolean[length];

        view.setView(sorted, hideTemps, hideLinks);

        buttons.clear();
        cont.clear();
        Arrays.fill(counter, RESET);

        buttons.defaults().size(200f, 64f);

        Snapshot snapshot = index < 0 ? null : snapshots.get(index);
        cont.table(t -> {
            t.table(text -> {
                text.add(snapshot == null ? "Live view" : snapshot.name())
                        .color(Pal.accent).ellipsis(true).top().width(300f).growX().left();
                if (snapshot != null) {
                    text.add(snapshot == null ? "" : SnapshotsDialog.dateFormat.format(new Date(snapshot.timestamp())))
                            .color(Color.gray).width(60f).left();
                }
            }).width(300f).top().growX().row();
        }).pad(10f).row();

        cont.table(t -> {
            ImageButton.ImageButtonStyle style = Styles.defaulti;
            t.defaults().size(64f).pad(5f);
            t.button(Icon.leftOpen, style, () -> setup(index - 1)).disabled(index < 0);
            t.button(Icon.download, style, () -> {
                if (snapshot.writeTo(data)) {
                    Vars.ui.showInfo("The processor's state has been restored from the snapshot.");
                    setup(snapshots.size);
                } else {
                    Vars.ui.showErrorMessage("Cannot restore this snapshot: either the snapshot is invalid, or the processor's code has been recompiled.");
                }
            }).disabled(index < 0);
            t.button(Icon.folderOpen, style, () -> new SnapshotsDialog(VarsDialog.this, snapshots).show()).get().setDisabled(() -> snapshots.isEmpty());
            if (snapshot == null) {
                t.button(Icon.box, style, () -> {
                    Snapshots.add(data.building(), "User snapshot");
                });
            } else {
                t.button(Icon.trash, style, () -> {
                    snapshots.removeIndex(index);
                    if (index >= snapshots.size) index--;
                    setup();
                });
            }
            t.button(Icon.rightOpen, style, () -> setup(index + 1)).get().setDisabled(() -> index >= snapshots.size - 1);
        }).pad(10f).row();

        cont.pane(p -> {
            p.margin(10f).marginRight(16f);
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
                            Label label = out.add("").style(Styles.outlineLabel).padLeft(4).padRight(4).width(220f).wrap().get();
                            label.setAlignment(Align.right);
                            label.act(1f);
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
                            if ((counter[index] += Time.delta) >= 15f) {
                                Object objval = view.obj(index);
                                double numval = view.num(index);
                                if (counter[index] >= RESET || objval != lastObject[index] || numval != lastMemory[index]) {
                                    lastObject[index] = objval;
                                    lastMemory[index] = numval;

                                    String text = view.formatted(index, hex);
                                    ValueType type = view.type(index);
                                    valueLabel.setText(text);
                                    typeLabel.setText(type.paddedTitle);

                                    halfShade.setColor(type.darkShade);
                                    fullShade.setColor(type.shade);

                                    updated[index] = counter[index] < RESET;
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

        buttons.button("@back", Icon.left, this::hide).name("back");

        if (processor) {
            buttons.button("@varsdialog.options", Icon.filters, () -> {
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
                        t.button("@varsdialog.showall", style, () -> updateView(hideTemps = false)).name("showall").group(tempsGroup).checked(!hideTemps);
                        t.button("@varsdialog.hidetemps", style, () -> updateView(hideTemps = true)).name("hidetemps").group(tempsGroup).checked(hideTemps);
                        t.row();
                        ButtonGroup<TextButton> linksGroup = new ButtonGroup<>();
                        t.button("@varsdialog.showlinks", style, () -> updateView(hideLinks = false)).name("showlinks").group(linksGroup).checked(!hideLinks);
                        t.button("@varsdialog.hidelinks", style, () -> updateView(hideLinks = true)).name("hidelinks").group(linksGroup).checked(hideLinks);
                        t.row();
                        t.defaults().size(323f, 45f).padTop(25f).padBottom(15f);
                        t.button("@back", Icon.left, Styles.flatt, dialog::hide).colspan(2).name("back");
                    });
                });

                dialog.addCloseListener();
                dialog.show();
            }).name("options");
        } else {
            buttons.button("@varsdialog.hex", Styles.squareTogglet, () -> refreshView(hex = !hex)).name("hex").checked(hex);
        }

        if (Core.graphics.isPortrait()) buttons.row();

        if (processor) {
            buttons.button("@logic.globals", Icon.list, () -> LogicDialogAddon.globalsDialog.show());
        }

        buttons.button("@edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@edit");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, t -> {
                    TextButton.TextButtonStyle style = Styles.flatt;
                    t.defaults().size(360f, 60f).left();

                    if (!processor) {
                        t.button("@varsdialog.clearmemory", Icon.cancel, style, () -> {
                            view.clear();
                            Arrays.fill(counter, RESET / 2);
                            dialog.hide();
                        }).marginLeft(12f).row();
                    }

                    t.button("@varsdialog.copyvariables", Icon.copy, style, () -> {
                        Core.app.setClipboardText(MemoryText.write(view, hex));
                        dialog.hide();
                    }).marginLeft(12f).row();

                    if (processor) {
                        t.button("@varsdialog.copyprintbuffer", Icon.copy, style, () -> {
                            String text = "Printbuffer contents:\n" + view.textBuffer();
                            Core.app.setClipboardText(text);
                            dialog.hide();
                        }).marginLeft(12f).row();
                    }

                    if (!processor) {
                        t.button("@varsdialog.importvariables", Icon.download, style, () -> {
                            String text = Core.app.getClipboardText();
                            String error = MemoryText.validate(text, length);
                            if (error == null) error = MemoryText.read(text, length, view);

                            if (error != null) {
                                Vars.ui.showInfoFade(Core.bundle.format("varsdialog.importfailed", error));
                                return;
                            }

                            Arrays.fill(counter, RESET / 2);
                            dialog.hide();
                        }).marginLeft(12f).row();
                    }

                    /*
                    t.button("Create snapshot", Icon.box, style, () -> {
                        Snapshots.create(view.building(), "User snapshot");
                        dialog.hide();
                    }).marginLeft(12f).row();

                    Seq<Snapshot> snapshots = Snapshots.snapshots.get(view.building());
                    if (snapshots != null) {
                        t.button("View snapshots", Icon.zoom, style, () -> {
                            var list = new SnapshotsDialog(VarsDialog.this, snapshots);
                            list.hidden(() -> dialog.hide());
                            list.show();
                        }).marginLeft(12f).row();
                    }
                     */
                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).name("edit");

        wasPortrait = Core.graphics.isPortrait();
        addCloseListener();
    }

    private void refreshView(boolean update) {
        Arrays.fill(counter, RESET);
    }

    private void updateView(boolean update) {
        view.setView(sorted, hideTemps, hideLinks);
        if (view.size() != length) {
            setup();
        } else {
            Arrays.fill(counter, RESET);
        }
    }
}
