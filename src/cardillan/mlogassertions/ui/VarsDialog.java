package cardillan.mlogassertions.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.actions.Actions;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.MappableContent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.Arrays;

public class VarsDialog extends BaseDialog {
    public static final double COLOR_LIMIT = Color.white.toDoubleBits();
    public static final float RESET = 1e10f;

    static boolean hex = false;
    static boolean sorted = false;
    static boolean hideTemps = false;
    static boolean hideLinks = false;

    private final VariableValues data;
    private final boolean processor;

    private final Object[] lastObject;
    private final double[] lastMemory;
    private final float[] counter;
    private final boolean[] updated;
    private int length;

    boolean wasPortrait;
    int rows, cols;

    public VarsDialog(VariableValues data) {
        super(data.processor() ? "@variables" : "@varsdialog.memory");
        data.setView(false, false, false);

        this.data = data;
        this.processor = data.processor();
        this.length = data.size();
        this.counter = new float[length];
        this.lastObject = new Object[length];
        this.lastMemory = new double[length];
        this.updated = new boolean[length];

        data.setView(sorted, hideTemps, hideLinks);

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

    public void setup() {
        buttons.clear();
        cont.clear();
        Arrays.fill(counter, RESET);

        buttons.defaults().size(200f, 64f);

        cont.pane(p -> {
            p.margin(10f).marginRight(16f);
            p.table(Tex.button, t -> {
                t.defaults().fillX().height(45f);
                length = data.size();
                cols = cols();
                rows = (length + cols - 1) / cols;

                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        int index = col * rows + row;
                        if (index >= length) break;

                        Color varColor = Pal.gray;
                        float stub = 8f, mul = 0.5f, pad = 4;

                        t.add(new Image(Tex.whiteui, varColor.cpy().mul(mul))).width(stub);
                        t.stack(new Image(Tex.whiteui, varColor), new Label(() -> data.label(index, hex)) {{
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

                        Image halfShade = t.add(new Image(Tex.whiteui, halfColor(typeColor(index)))).width(stub).get();
                        Image fullShade = new Image(Tex.whiteui, typeColor(index));
                        Label typeLabel = new Label("");
                        typeLabel.setAlignment(Align.center);
                        typeLabel.setStyle(Styles.outlineLabel);
                        t.stack(fullShade, typeLabel).minWidth(120f).get();

                        valueLabel.update(() -> {
                            if ((counter[index] += Time.delta) >= 15f) {
                                Object objval = data.obj(index);
                                double numval = data.num(index);
                                if (counter[index] >= RESET || objval != lastObject[index] || numval != lastMemory[index]) {
                                    lastObject[index] = objval;
                                    lastMemory[index] = numval;

                                    String text = print(index);
                                    valueLabel.setText(text);
                                    typeLabel.setText(typeName(index));

                                    Color typeColor = typeColor(index);
                                    halfShade.setColor(halfColor(typeColor));
                                    fullShade.setColor(typeColor);

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
                        t.button("@varsdialog.dec", style, () -> {
                            hex = false;
                            Arrays.fill(counter, RESET);
                        }).name("dec").group(hexGroup).checked(!hex);
                        t.button("@varsdialog.hex", style, () -> {
                            hex = true;
                            Arrays.fill(counter, RESET);
                        }).name("hex").group(hexGroup).checked(hex);
                        t.row();

                        t.row();
                        ButtonGroup<TextButton> sortedGroup = new ButtonGroup<>();
                        t.button("@varsdialog.unsorted", style, () -> {
                            data.setView(sorted = false, hideTemps, hideLinks);
                            Arrays.fill(counter, RESET);
                        }).name("unsorted").group(sortedGroup).checked(!sorted);
                        t.button("@varsdialog.sorted", style, () -> {
                            data.setView(sorted = true, hideTemps, hideLinks);
                            Arrays.fill(counter, RESET);
                        }).name("sorted").group(sortedGroup).checked(sorted);

                        t.row();
                        ButtonGroup<TextButton> tempsGroup = new ButtonGroup<>();
                        t.button("@varsdialog.showall", style, () -> {
                            data.setView(sorted, hideTemps = false, hideLinks);
                            if (data.size() != length) {
                                setup();
                            } else {
                                Arrays.fill(counter, RESET);
                            }
                        }).name("showall").group(tempsGroup).checked(!hideTemps);
                        t.button("@varsdialog.hidetemps", style, () -> {
                            data.setView(sorted, hideTemps = true, hideLinks);
                            if (data.size() != length) {
                                setup();
                            } else {
                                Arrays.fill(counter, RESET);
                            }
                        }).name("hidetemps").group(tempsGroup).checked(hideTemps);

                        t.row();
                        ButtonGroup<TextButton> linksGroup = new ButtonGroup<>();
                        t.button("@varsdialog.showlinks", style, () -> {
                            data.setView(sorted, hideTemps, hideLinks = false);
                            if (data.size() != length) {
                                setup();
                            } else {
                                Arrays.fill(counter, RESET);
                            }
                        }).name("showlinks").group(linksGroup).checked(!hideLinks);
                        t.button("@varsdialog.hidelinks", style, () -> {
                            data.setView(sorted, hideTemps, hideLinks = true);
                            if (data.size() != length) {
                                setup();
                            } else {
                                Arrays.fill(counter, RESET);
                            }
                        }).name("hidelinks").group(linksGroup).checked(hideLinks);

                        t.row();
                        t.defaults().size(323f, 45f).padTop(25f).padBottom(15f);
                        t.button("@back", Icon.left, Styles.flatt, dialog::hide).colspan(2).name("back");
                    });
                });

                dialog.addCloseListener();
                dialog.show();
            }).name("options");
        } else {
            buttons.button("@varsdialog.hex", Styles.squareTogglet, () -> {
                hex = !hex;
                Arrays.fill(counter, -1f);
            }).name("hex").checked(hex);
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
                            data.clear();
                            Arrays.fill(counter, RESET / 2);
                            dialog.hide();
                        }).marginLeft(12f).row();
                    }

                    t.button("@varsdialog.copyvariables", Icon.copy, style, () -> {
                        StringBuilder sbr = new StringBuilder(500);
                        sbr.append("Slot\tType\tValue\n");
                        for (int i = 0; i < length; i++) {
                            sbr.append(data.label(i, false))
                                    .append("\t").append(typeName(i))
                                    .append("\t").append(data.isObj((i)) && data.obj(i) instanceof String str ? str : print(i))
                                    .append("\n");
                        }
                        Core.app.setClipboardText(sbr.toString());
                        dialog.hide();
                    }).marginLeft(12f).row();

                    if (processor) {
                        t.button("@varsdialog.copyprintbuffer", Icon.copy, style, () -> {
                            String text = "Printbuffer contents:\n" + data.textBuffer();
                            Core.app.setClipboardText(text);
                            dialog.hide();
                        }).marginLeft(12f).row();
                    }
                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).name("edit");

        wasPortrait = Core.graphics.isPortrait();
        addCloseListener();
    }

    private String print(int index) {
        if (data.isObj(index)) {
            Object obj = data.obj(index);
            if (obj instanceof String str) {
                return str.length() > 40 ? str.substring(0, 40).trim() + "[gold]..." : str;
            } else {
                return
                        obj == null ? "null" :
                        obj instanceof MappableContent content ? content.name :
                        obj instanceof Content ? "[content]" :
                        obj instanceof Building build ? build.block.name + pos(build.x(), build.y()) :
                        obj instanceof Unit unit ? unit.type.name + pos(unit.x(), unit.y()) :
                        obj instanceof Enum<?> e ? e.name() :
                        obj instanceof Team team ? team.name :
                        "[object]";
            }
        } else {
            double num = data.num(index);
            if (num <= COLOR_LIMIT && num > 0) {
                long color = Double.doubleToLongBits(num) & 0xFFFFFFFFL;
                String str = Integer.toHexString((int) color);
                if (str.length() < 8) str = "0".repeat(8 - str.length()) + str;
                return '%' + str + " [#" + str.substring(0, 6) + "]\ue86b";
            } else if ((long) num == num) {
                return hex ? "0x" + Long.toHexString((long) num).toUpperCase() : Long.toString((long) num);
            } else {
                return hex ? Double.toHexString(num).toLowerCase() : Double.toString(num).toLowerCase();
            }
        }
    }

    private String pos(float x, float y) {
        return String.format(" (%.1f,\u00a0%.1f)", x / Vars.tilesize, y / Vars.tilesize);
    }

    private static final Color col = new Color();
    private Color halfColor(Color color) {
        return col.set(color).mul(0.5f);
    }

    public Color typeColor(int index) {
        if (data.isLink(index)) {
            return Pal.tungstenShot;
        } else if (data.isObj(index)) {
            Object objval = data.obj(index);
            return  objval == null ? Color.darkGray :
                    objval instanceof String ? Pal.ammo :
                    objval instanceof Content ? Pal.logicOperations :
                    objval instanceof Building ? Pal.logicBlocks :
                    objval instanceof Unit ? Pal.logicUnits :
                    objval instanceof Team ? Pal.logicControl :
                    objval instanceof Enum<?> ? Pal.logicIo :
                    Color.white;
        } else {
            double numval = data.num(index);
            return  numval <= COLOR_LIMIT && numval > 0 ? Pal.berylShot :
                    (long) numval == numval ? Pal.logicWorld :
                    Pal.place;
        }
    }

    public String typeName(int index) {
        if (data.isLink(index)) {
            return " link ";
        } else if (data.isObj(index)) {
            Object objval = data.obj(index);
            return  objval == null ? " null " :
                    objval instanceof String ? " string " :
                    objval instanceof Content ? " content " :
                    objval instanceof Building ? " building " :
                    objval instanceof Team ? " team " :
                    objval instanceof Unit ? " unit " :
                    objval instanceof Enum<?> ? " enum " :
                    " unknown ";
        } else {
            double num = data.num(index);
            return  num <= COLOR_LIMIT && num > 0 ? " color " :
                    (long) num == num ? " integer " :
                    " number ";
        }
    }
}
