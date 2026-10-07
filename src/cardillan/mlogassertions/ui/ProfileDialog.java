package cardillan.mlogassertions.ui;

import arc.Core;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import cardillan.mlogassertions.Constants;
import cardillan.mlogassertions.logic.Instrumentation;
import cardillan.mlogassertions.logic.InstrumentationEngine;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

import java.util.Arrays;

public class ProfileDialog extends BaseDialog {
    static boolean totals = true;
    static boolean percents = false;
    static boolean sorted = false;
    static boolean branches = false;
    static boolean colors = true;
    static boolean execTime = false;

    Instrumentation instrumentation = null;
    LogicBuild build;
    long lastCheck = 0;
    long lastSort = 0;

    // Saves/restores scroll position when the content of the pane changes
    float scroll = 0f;
    float w;

    public ProfileDialog(LogicBuild build) {
        super("Profiler", Styles.fullDialog);
        this.build = build;

        instrumentation = InstrumentationEngine.getInstrumentation(build);
        if (instrumentation == null && Core.settings.getBool(Constants.startProfilerImmediatelly)) {
            instrumentation = InstrumentationEngine.startProfiling(build);
        }

        onResize(() -> {
            if (w != w()) setup();
        });

        setup();
    }

    private float w() {
        return Math.max(180f, Math.min(Core.graphics.getWidth() / Scl.scl(1.05f) - 210f, 800f));
    }

    int indexes[];
    int prevIndex[];
    long sortArray[];

    private void sort() {
        int size = instrumentation.size;
        lastSort = System.currentTimeMillis();

        if (sorted) {
            for (int i = 0; i < size; i++) {
                sortArray[i] = ((execTime ? (long) instrumentation.time[i] : (long) instrumentation.steps[i]) << 32) | (long) (size - i);
            }

            Arrays.sort(sortArray);

            for (int i = 0; i < size; i++) {
                indexes[i] = size - (int) (sortArray[size - i - 1] & (long) Integer.MAX_VALUE);
            }
        } else {
            for (int i = 0; i < size; i++) indexes[i] = i;
        }
    }

    public void startStop() {
        instrumentation = InstrumentationEngine.getInstrumentation(build, true, instr -> instr.profiling = !instr.profiling);
        setup();
    }

    public void setup(Instrumentation instrumentation) {
        if (this.instrumentation != instrumentation) {
            this.instrumentation = instrumentation;
            setup();
        }
    }

    public void setup(boolean dummy) {
        setup();
    }

    public void setup() {
        buttons.clear();
        cont.clear();

        Color basicColor = Color.slate.cpy().mul(0.8f);
        Color barColor = basicColor.cpy().mul(0.66f);
        Color fillColor = basicColor.cpy().mul(0.33f);
        Color emptyColor = basicColor.cpy().mul(0.15f);

        w = w();
        float labelPad = 8f;
        float iWidth = branches ? w - 114f : w;

        if (instrumentation != null && instrumentation.size > 0) {
            indexes = new int[instrumentation.size];
            prevIndex = new int[instrumentation.size];
            sortArray = new long[instrumentation.size];
            sort();

            cont.table(t -> {
                ImageButton.ImageButtonStyle style = Styles.cleari;
                t.defaults().size(40f).pad(5f);
                //t.button(Icon.left, style, this::hide);
                ImageButton active = t.button(Icon.chartBar, Styles.clearTogglei, this::startStop).get();
                active.update(() -> active.setChecked(instrumentation != null && instrumentation.profiling));

                t.button(Icon.edit, style, this::editCommands);
                t.image().growY().width(4f).pad(6f).color(Pal.gray);
                t.button(Icon2.time, Styles.clearTogglei, () -> setup(execTime = !execTime)).checked(execTime);
                t.button(Icon2.sortDesc, Styles.clearTogglei, () -> setup(sorted = !sorted)).checked(sorted);
                t.button(Icon2.percent, Styles.clearTogglei, () -> percents = !percents).checked(percents);
                t.button(Icon2.branching, Styles.clearTogglei, () -> setup(branches = !branches)).checked(branches);
                t.button(Icon2.sum, Styles.clearTogglei, () -> setup(totals = !totals)).checked(totals);
                t.button(Icon.tag, Styles.clearTogglei, () -> setup(colors = !colors)).checked(colors);
                t.button(Icon.infoCircle, style, this::help);
            }).top().growX().fillX().row();

            cont.table(main -> {
                main.pane(p -> {
                    p.table(t -> {
                        t.defaults().fillX().height(35f).padRight(4f).padTop(4f);

                        for (int i = 0; i < instrumentation.size; i++) {
                            int ind = i;

                            Image numImage = new Image(Tex.whiteui, colors ? instrumentation.colors[indexes[ind]] : basicColor);
                            Label numLabel = new Label(String.valueOf(indexes[ind]));
                            t.stack(numImage, new Table(l -> {
                                l.add(numLabel).color(Color.white).padLeft(labelPad).padRight(labelPad);
                            })).minWidth(65f).width(65f);

                            ProgressBackground ratio = new ProgressBackground(barColor, fillColor);
                            Label source = new Label(VarsDialog.escape(instrumentation.source[indexes[ind]]));
                            t.stack(ratio, new Table(l -> {
                                l.add(source).color(Color.lightGray)
                                        .minWidth(0f).left().growX().fillX().padLeft(10f).padRight(10f)
                                        .wrap(false).ellipsis(true).get().setAlignment(Align.left);
                            })).minWidth(iWidth).width(iWidth).growX().fillX();

                            Label branchLabel = new Label("");
                            if (branches) {
                                t.stack(new Image(Tex.whiteui, basicColor), new Table(l -> {
                                    l.add(branchLabel).color(Pal.accent).padLeft(labelPad).padRight(labelPad);
                                })).minWidth(110f).width(110f);
                            }

                            Label countLabel = new Label("");
                            countLabel.update(() -> {
                                int index = indexes[ind];

                                if (prevIndex[ind] != index) {
                                    prevIndex[ind] = index;
                                    numImage.setColor(colors ? instrumentation.colors[index] : basicColor);
                                    numLabel.setText(String.valueOf(index));
                                    source.setText(VarsDialog.escape(instrumentation.source[index]));
                                }

                                boolean covered = instrumentation.steps[index] > 0 || instrumentation.covered.get(index);
                                source.setColor(covered ? Color.white : Color.lightGray);

                                if (branches) {
                                    branchLabel.setText(instrumentation.branching[index] < 0 ? "" : percents
                                            ? formatPercent(instrumentation.branching[index], instrumentation.steps[index], "%.1f%%")
                                            : formatNumber(instrumentation.branching[index]));
                                }

                                if (execTime) {
                                    countLabel.setText(percents
                                            ? formatPercent(instrumentation.time[index], instrumentation.totalTime, "%.2f%%")
                                            : formatNumber(instrumentation.time[index]));
                                    ratio.progress = (float) (instrumentation.time[index] / instrumentation.maxTime);
                                } else {
                                    countLabel.setText(percents
                                            ? formatPercent(instrumentation.steps[index], instrumentation.totalSteps, "%.2f%%")
                                            : formatNumber(instrumentation.steps[index]));
                                    ratio.progress = instrumentation.steps[index] / (float) instrumentation.maxSteps;
                                }
                                ratio.fillColor = covered ? fillColor : emptyColor;
                            });
                            t.stack(new Image(Tex.whiteui, basicColor), new Table(l -> {
                                l.add(countLabel).color(Pal.accent).padLeft(labelPad).padRight(labelPad);
                            })).minWidth(110f).width(110f).padRight(0);

                            t.row();
                        }
                    }).growY().top().marginRight(15f);
                }).scrollX(false).update(s -> scroll = s.getScrollY()).get().setScrollYForce(scroll);

                if (totals) {
                    main.row();
                    main.table(t -> {
                        String[] titles = {
                                execTime ? "Total execution quota spent" : "Total instructions executed",
                                "Execution quota lost to yields",
                                "Code coverage (" + instrumentation.size + " instructions in total)"
                        };
                        Prov[] values = {
                                execTime ? () -> formatNumber(instrumentation.totalTime) : () -> formatNumber(instrumentation.totalSteps),
                                () -> percents ? formatPercent(instrumentation.lostQuota, instrumentation.totalTime, "%.1f%%"): formatNumber(instrumentation.lostQuota),
                                () -> percents ? formatPercent(instrumentation.coverage, instrumentation.size, "%.1f%%") : String.valueOf(instrumentation.coverage)
                        };

                        t.defaults().fillX().height(35f).padRight(4f).padTop(4f);

                        for (int i = 0; i < titles.length; i++) {
                            int index = i;
                            t.image(Tex.whiteui, basicColor).minWidth(65f).width(65f);

                            t.stack(new Image(Tex.whiteui, basicColor), new Table(l -> {
                                l.add(titles[index]).color(Pal.accent)
                                        .minWidth(0f).left().growX().fillX().padLeft(10f).padRight(10f)
                                        .wrap(false).ellipsis(true).get().setAlignment(Align.left);
                            })).minWidth(w).width(w).growX().fillX();

                            Label countLabel = new Label(values[index]);
                            t.stack(new Image(Tex.whiteui, basicColor), new Table(l -> {
                                l.add(countLabel).color(Pal.accent).padLeft(labelPad).padRight(labelPad);
                            })).minWidth(110f).width(110f).padRight(0);
                            t.row();
                        }
                    }).left();
                }
            });

            cont.update(() -> {
                if (sorted) {
                    if (execTime) {
                        if (lastCheck < System.currentTimeMillis() - 500) {
                            lastCheck = System.currentTimeMillis();
                            int diff = System.currentTimeMillis() - lastSort > 1500 ? 1 : 5;
                            for (int i = 1; i < indexes.length; i++) {
                                if (instrumentation.time[indexes[i]] - instrumentation.time[indexes[i - 1]] > diff) {
                                    Core.app.post(() -> sort());
                                    break;
                                }
                            }
                        }
                    } else {
                        if (lastCheck < System.currentTimeMillis() - 500) {
                            lastCheck = System.currentTimeMillis();
                            int diff = System.currentTimeMillis() - lastSort > 1500 ? 1 : 5;
                            for (int i = 1; i < indexes.length; i++) {
                                if (instrumentation.steps[indexes[i]] - instrumentation.steps[indexes[i - 1]] > diff) {
                                    Core.app.post(() -> sort());
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        } else {
            cont.table(t -> {
                t.add("Profiler records the number of times each instruction executes. Once activated, it remains active even after leaving this screen, until stopped.\n\n" +
                        "If the processor's code gets updated, the profiler remains active, but the data gathered so far are cleared.")
                        .color(Color.lightGray).width(410f).minWidth(410f).wrap(true).padBottom(30f);
                t.row();
                t.check("Do not show again", b -> Core.settings.put(Constants.startProfilerImmediatelly, b))
                        .padBottom(30f).growX().fillX();
                t.row();
                t.button("Start profiling", Icon.chartBar, Styles.flatBordert, () -> setup(InstrumentationEngine.startProfiling(build)))
                        .height(64f).growX().fillX();
            }).left();
        }
        addCloseButton();
    }

    private String formatNumber(int number) {
        return number == 0 ? "-" : String.valueOf(number);
    }

    private String formatPercent(int part, int total, String format) {
        if (total <= 0 || part <= 0) return "-";
        if (part >= total) return "100%";
        String result = String.format(format, 100d * part / total);
        return result.startsWith("100.") ? result.substring(1, result.length()).replace('0', '9') : result;
    }

    private String formatNumber(double number) {
        return formatNumber((int) number);
    }

    private String formatPercent(double part, double total, String format) {
        if (total <= 0 || part <= 0) return "-";
        if (part >= total) return "100%";
        String result = String.format(format, 100 * part / total);
        return result.startsWith("100.") ? result.substring(1, result.length()).replace('0', '9') : result;
    }

    private void restart() {
        build.updateCode(build.code);
        InstrumentationEngine.clearProfilingData(build);
        instrumentation = InstrumentationEngine.startProfiling(build);
        setup();
    }

    private void copyToClipboard() {
        StringBuilder sb = new StringBuilder(200 * instrumentation.size);
        sb.append("#")
                .append("\t").append("Instruction")
                .append("\t").append("Execution steps")
                .append("\t").append("Execution quota")
                .append("\n");

        for (int i = 0; i < instrumentation.size; i++) {
            sb.append(i)
                    .append("\t").append(instrumentation.source[i])
                    .append("\t").append(instrumentation.steps[i])
                    .append("\t").append(instrumentation.time[i])
                    .append("\n");
        }
        Core.app.setClipboardText(sb.toString());
    }

    private void help(Table t, TextureRegionDrawable icon, String text) {
        t.image(icon).color(Color.lightGray).size(32f, 32f);
        t.add(text).color(Color.lightGray).width(350f).minWidth(0f).wrap().row();
    }

    private void editCommands() {
        BaseDialog dialog = new BaseDialog("@edit");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButton.TextButtonStyle style = Styles.flatt;
                t.defaults().size(360f, 60f).left();

                t.button("Reset data", Icon.cancel, style, () -> {
                    setup(InstrumentationEngine.clearProfilingData(build));
                    dialog.hide();
                }).marginLeft(12f).row();

                t.button("Reset data and restart processor", Icon.refresh, style, () -> {
                    restart();
                    dialog.hide();
                }).marginLeft(12f).row();

                t.button("Copy profiling data\nto clipboard", Icon.copy, style, () -> {
                    copyToClipboard();
                    dialog.hide();
                }).marginLeft(12f).row();

                t.button("@back", Icon.left, style, dialog::hide).padTop(10f).marginLeft(12f).name("back");
            });
        });

        dialog.addCloseListener();
        dialog.show();
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

                help(t, Icon.chartBar, "Start or stop profiling the current processor.");
                help(t, Icon.edit, "Reset, restart or copy profiling data.");
                help(t, Icon2.time, "Display execution quota spent by instructions instead of execution steps (the [accent]wait[] instruction may spend lots of execution quota waiting).");
                help(t, Icon2.sortDesc, "Sort the instructions by execution steps/quota.");
                help(t, Icon2.percent, "Display percentages instead of raw values.");
                help(t, Icon2.branching, "Show the number of jumps made by jump instructions.");
                help(t, Icon2.sum, "Show profiling totals.");
                help(t, Icon.tag, "Use the instruction's category color in the list.");
                help(t, Icon.infoCircle, "Show this help.");

                t.add("Edit commands").colspan(3).color(Pal.accent).center().padBottom(15F).get().setAlignment(Align.center);
                t.row();

                help(t, Icon.cancel, "Clear the current processor's profiling data.");
                help(t, Icon.refresh, "Restart the current processor and activate profiling from the beginning (existing profiling data are cleared).");
                help(t, Icon.copy, "Copy the profiling data into the clipboard in a tab-separated format (instruction #, instruction text, execution steps and quota).");

                t.defaults().size(180f, 60f).growX().colspan(3).pad(15f);
                t.button("@back", Icon.left, Styles.defaultt, dialog::hide).center().marginLeft(12f).name("back");
            }).pad(10f).padRight(30f);
        });

        dialog.addCloseListener();
        dialog.show();
    }

    private static class ProgressBackground extends Element {
        public float progress = 0f;
        public Color barColor;
        public Color fillColor;

        public ProgressBackground(Color barColor, Color fillColor) {
            touchable = Touchable.disabled;
            this.barColor = barColor;
            this.fillColor = fillColor;
        }

        @Override
        public void draw() {
            float barWidth = getWidth() * Mathf.clamp(progress);

            Draw.color(fillColor);
            Fill.rect(x + width / 2f, y + height / 2f, width, height);

            Draw.color(barColor);
            Fill.rect(x + barWidth / 2f, y + height / 2f, barWidth, height);

            Draw.reset();
        }
    }
}
