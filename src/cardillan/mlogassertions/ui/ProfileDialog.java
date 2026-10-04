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

        Instrumentation instrumentation = InstrumentationEngine.getInstrumentation(build);
        if (instrumentation == null && Core.settings.getBool(Constants.startProfilerImmediatelly)) {
            instrumentation = InstrumentationEngine.startProfiling(build);
        }

        onResize(() -> {
            if (w != w()) setup();
        });
        setup(instrumentation);
    }

    private float w() {
        return Math.max(180f, Math.min(Core.graphics.getWidth() / Scl.scl(1.05f) - 210f, 800f));
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
        Color barColor2 = barColor.cpy().lerp(Color.lightGray, 0.5f);

        w = w();
        float labelPad = 8f;
        float iWidth = branches ? w - 90f : w;

        if (instrumentation != null && instrumentation.size > 0) {
            int size = instrumentation.size;
            int indexes[] = new int[size];

            if (sorted) {
                lastSort = System.currentTimeMillis();
                if (execTime) {
                    Integer[] ind = new Integer[size];
                    for (int i = 0; i < size; i++) ind[i] = i;
                    Arrays.sort(ind, (i1, i2) -> (int) (instrumentation.time[i2] - instrumentation.time[i1]));
                    for (int i = 0; i < size; i++) indexes[i] = ind[i];
                } else {
                    for (int i = 0; i < size; i++) indexes[i] = -(instrumentation.steps[i] * size + (size - i - 1));
                    Arrays.sort(indexes);
                    for (int i = 0; i < size; i++) indexes[i] = size - -indexes[i] % size - 1;
                }
            } else {
                for (int i = 0; i < size; i++) indexes[i] = i;
            }

            cont.table(t -> {
                ImageButton.ImageButtonStyle style = Styles.cleari;
                t.defaults().size(40f).pad(5f);
                //t.button(Icon.left, style, this::hide);
                t.button(Icon.chartBar, Styles.clearTogglei, this::startStop).checked(instrumentation != null && instrumentation.profiling);
                t.button(Icon.refresh, style, this::restart);
                t.button(Icon.copy, style, this::copyToClipboard);
                t.button(Icon.cancel, style, () -> setup(InstrumentationEngine.clearProfilingData(build)));
                t.image().growY().width(4f).pad(6f).color(Pal.gray);
                t.button(Icon2.time, Styles.clearTogglei, () -> setup(execTime = !execTime)).checked(execTime);
                t.button(Icon2.sortDesc, Styles.clearTogglei, () -> { scroll = 0; setup(sorted = !sorted); }).checked(sorted);
                t.button(Icon2.percent, Styles.clearTogglei, () -> setup(percents = !percents)).checked(percents);
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
                            int index = indexes[i];
                            Color color = instrumentation.colors[index].cpy().mul(0.8f);

                            t.stack(new Image(Tex.whiteui, colors ? color : basicColor), new Table(l -> {
                                l.add(String.valueOf(index)).color(Color.white).padLeft(labelPad).padRight(labelPad);
                            })).minWidth(65f).width(65f);

                            ProgressBackground ratio = new ProgressBackground(barColor, barColor2, fillColor);
                            Label source = new Label(VarsDialog.escape(instrumentation.source[index]));
                            t.stack(ratio, new Table(l -> {
                                l.add(source).color(Color.lightGray)
                                        .minWidth(0f).left().growX().fillX().padLeft(10f).padRight(10f)
                                        .wrap(false).ellipsis(true).get().setAlignment(Align.left);
                            })).minWidth(iWidth).width(iWidth).growX().fillX();

                            if (branches) {
                                Label branchLabel = instrumentation.branching[index] < 0 ? new Label("") :
                                        new Label(() -> formatPercent(instrumentation.branching[index], instrumentation.steps[index], "%.1f%%"));
                                t.stack(new Image(Tex.whiteui, basicColor), new Table(l -> {
                                    l.add(branchLabel).color(Pal.accent).padLeft(labelPad).padRight(labelPad);
                                })).minWidth(86f).width(86f);

                            }

                            Label countLabel = new Label("");
                            if (execTime) {
                                countLabel.update(() -> {
                                    if (instrumentation.steps[index] > 0) source.setColor(Color.white);
                                    countLabel.setText(percents
                                            ? formatPercent(instrumentation.time[index], instrumentation.totalTime, "%.2f%%")
                                            : formatNumber(instrumentation.time[index]));
                                    ratio.progress = instrumentation.time[index] / instrumentation.maxTime;
                                });
                            } else {
                                countLabel.update(() -> {
                                    if (instrumentation.steps[index] > 0) source.setColor(Color.white);
                                    countLabel.setText(percents
                                            ? formatPercent(instrumentation.steps[index], instrumentation.totalSteps, "%.2f%%")
                                            : formatNumber(instrumentation.steps[index]));
                                    ratio.progress = instrumentation.steps[index] / (float) instrumentation.maxSteps;
                                });
                            }
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
                                "Code coverage"
                        };
                        Prov[] values = {
                                execTime ? () -> formatNumber(instrumentation.totalTime) : () -> formatNumber(instrumentation.totalSteps),
                                () -> formatNumber((int) instrumentation.lostQuota),
                                () -> formatPercent(instrumentation.coverage, instrumentation.size, "%.1f%%")
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

            if (sorted) {
                if (execTime) {
                    cont.update(() -> {
                        if (lastCheck < System.currentTimeMillis() - 500) {
                            lastCheck = System.currentTimeMillis();
                            int diff = System.currentTimeMillis() - lastSort > 1500 ? 1 : 5;
                            for (int i = 1; i < size; i++) {
                                if (instrumentation.time[indexes[i]] - instrumentation.time[indexes[i - 1]] > diff) {
                                    Core.app.post(this::setup);
                                    break;
                                }
                            }
                        }
                    });
                } else {
                    cont.update(() -> {
                        if (lastCheck < System.currentTimeMillis() - 500) {
                            lastCheck = System.currentTimeMillis();
                            int diff = System.currentTimeMillis() - lastSort > 1500 ? 1 : 5;
                            for (int i = 1; i < size; i++) {
                                if (instrumentation.steps[indexes[i]] - instrumentation.steps[indexes[i - 1]] > diff) {
                                    Core.app.post(this::setup);
                                    break;
                                }
                            }
                        }
                    });
                }
            } else {
                cont.update(() -> {});
            }
        } else {
            cont.table(t -> {
                t.add("Profiler records the number of times each instruction executes. Once activated, it remains active even after leaving this screen, until stopped.\n\n" +
                        "If the processor's code gets updated, the profiler remains active, but the data gathered so far are cleared.")
                        .color(Color.lightGray).width(410f).minWidth(410f).wrap(true).padBottom(30f);
                t.row();
                t.check("Do not show again", b -> Core.settings.put(Constants.startProfilerImmediatelly, b))
                        .padBottom(30f).growX().fillX();
                t.row();
                t.button("Start profiling", Icon.play, Styles.flatBordert, () -> setup(InstrumentationEngine.startProfiling(build)))
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

    private String formatNumber(float number) {
        return formatNumber((int) number);
    }

    private String formatPercent(float part, float total, String format) {
        if (total <= 0 || part <= 0) return "-";
        if (part >= total) return "100%";
        return String.format(format, 100 * part / total);
        //String result = String.format(format, 100 * part / total);
        //return result.startsWith("100.") ? result.substring(1, result.length()).replace('0', '9') : result;
    }

    private void restart() {
        build.updateCode(build.code);
        InstrumentationEngine.clearProfilingData(build);
        instrumentation = InstrumentationEngine.startProfiling(build);
        setup();
    }

    private void copyToClipboard() {
        StringBuilder sb = new StringBuilder(200 * instrumentation.size);
        for (int i = 0; i < instrumentation.size; i++) {
            sb.append(i)
                    .append("\t").append(instrumentation.source[i])
                    .append("\t").append(instrumentation.steps[i])
                    .append("\n");
        }
        Core.app.setClipboardText(sb.toString());
    }

    private void help(Table t, TextureRegionDrawable icon, String text) {
        t.image(icon).color(Color.lightGray).size(32f, 32f);
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

                help(t, Icon.chartBar, "Start or stop profiling the current processor.");
                help(t, Icon.refresh, "Restart the current processor and activate profiling from the beginning (existing profiling data are cleared).");
                help(t, Icon.copy, "Copy the profiling data into the clipboard in a tab-separated format (instruction #, instruction text, execution count).");
                help(t, Icon.cancel, "Clear the current processor's profiling data.");
                help(t, Icon2.time, "Display execution quota spent by instructions instead of execution steps (the [accent]wait[] instruction may spend lots of execution quota waiting).");
                help(t, Icon2.sortDesc, "Sort the instructions by execution steps/quota.");
                help(t, Icon2.percent, "Displays the percentage share of each instruction's execution count relative to the total number of executions.");
                help(t, Icon2.branching, "Show the percentage of jumps made by a jump instruction relative to the total number of executions of that instruction.");
                help(t, Icon2.sum, "Show profiling totals.");
                help(t, Icon.tag, "Use the instruction's category color in the list.");
                help(t, Icon.infoCircle, "Show this help.");

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
        public Color barColor2;
        public Color fillColor;

        public ProgressBackground(Color barColor, Color barColor2, Color fillColor) {
            touchable = Touchable.disabled;
            this.barColor = barColor;
            this.barColor2 = barColor2;
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
