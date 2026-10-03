package cardillan.mlogassertions.logic;

import arc.func.Cons;
import arc.func.Func;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.gen.Icon;
import mindustry.gen.LogicIO;
import mindustry.logic.*;
import mindustry.logic.LStatements.JumpStatement;
import mindustry.ui.Styles;

public class LogicStatements {
    private static final LogicStatementWriter writer = new LogicStatementWriter();

    public static void register() {
        register(AssertStatement::new, AssertStatement.opcode, AssertStatement::read);
        register(AssertBoundsStatement::new, AssertBoundsStatement.opcode, AssertBoundsStatement::read);
        register(AssertEqualsStatement::new, AssertEqualsStatement.opcode, AssertEqualsStatement::read);
        register(AssertFlushStatement::new, AssertFlushStatement.opcode, AssertFlushStatement::read);
        register(AssertPrintsStatement::new, AssertPrintsStatement.opcode, AssertPrintsStatement::read);
        register(AssertTypeStatement::new, AssertTypeStatement.opcode, AssertTypeStatement::read);
        register(BreakpointStatement::new, BreakpointStatement.opcode, BreakpointStatement::read);
        register(ErrorStatement::new, ErrorStatement.opcode, ErrorStatement::read);
        register(LogStatement::new, LogStatement.opcode, LogStatement::read);
        register(ProfileStatement::new, ProfileStatement.opcode, ProfileStatement::read);
        register(RestartStatement::new, RestartStatement.opcode, RestartStatement::read);
        register(SnapshotStatement::new, SnapshotStatement.opcode, SnapshotStatement::read);
    }

    private static void register(Prov<LStatement> prov, String opcode, Func<String[], LStatement> parser) {
        if (LAssembler.customParsers.containsKey(opcode)) {
            Log.warn("Logic statement opcode already registered: " + opcode);
            return;
        }
        LogicIO.allStatements.add(prov);
        LAssembler.customParsers.put(opcode, parser);
    }

    public static abstract class AbstractAssertStatement extends LStatement {
        final String name;

        // This is the optional (expandable) message field. Child classes need to use it for optional messages.
        protected String message = "null";
        boolean expanded = false;

        public AbstractAssertStatement(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public LCategory category() {
            return AssertLogic.assertsCategory;
        }

        protected boolean expanded() {
            return !message.equals("null");
        }

        protected abstract void rebuild(Table t);

        @Override
        public final void build(Table table) {
            expanded = expanded();
            rebuild(table);
        }

        // Expandlable message field
        protected void message(Table t, String label, String defaultMessage) {
            if (!expanded) {
                t.button(b -> {
                    b.add("add " + label).color(Color.gray);
                    b.clicked(() -> {
                        expanded = true;
                        message = '"' + defaultMessage + '"';
                        rebuild(t);
                    });
                }, Styles.logict, () -> {
                }).size(180f, 40f).left().pad(4f).color(t.color);
            } else {
                t.add(label).padLeft(10);
                // Convert various representations of empty messages to null
                field(t, message, str -> message = str == null || str.isEmpty() || str.equals("\"\"") || str.equals("\"") ? "null" : str)
                        .width(LCanvas.getTargetWidth() - Scl.scl(20f)).padRight(3);
                if (false) {
                    // A delete button - could be activated inadvertently
                    t.button(b -> {
                        b.image(Icon.trashSmall, t.color);
                        b.clicked(() -> {
                            message = "null";
                            expanded = false;
                            rebuild(t);
                        });
                    }, Styles.logict, () -> {
                    }).size(40f).padLeft(-1).color(t.color);
                }
            }
        }

        // Select field with optional label
        protected <T extends Enum<?>> void select(Table table, String label, T[] values, T current, Cons<T> getter, int cols, float width) {
            Table sub = new Table();
            sub.setColor(table.color);
            table.add(sub);

            if (!label.isEmpty()) sub.add(label).padLeft(10);
            table.button(b -> {
                b.add(bundle(current));
                b.clicked(() -> showSelect(b, values, current, o -> { getter.get(o); rebuild(table); }, cols, c -> c.width(width)));
            }, Styles.logict, () -> {
            }).size(width, 40f).left().pad(4f).color(table.color);
        }
    }

    public abstract static class AbstractMessageStatement extends AbstractAssertStatement {
        private final String opcode;
        private final boolean hasLevel;
        public Log.LogLevel level = Log.LogLevel.info;
        public String[] params = new String[10];

        public AbstractMessageStatement(String opcode, String name, String message) {
            super(name);
            this.opcode = opcode;
            this.hasLevel = opcode.equals("log");
            params[0] = "\"" + message + " at #{@counter}.\"";
            for (int i = 1; i < params.length; i++) params[i] = "null";
        }

        @Override
        protected boolean expanded() {
            for (int i = 1; i < params.length; i++) {
                if (!params[i].isEmpty() && !"null".equals(params[i])) {
                    return true;
                }
            }
            return false;
        }

        protected void rebuild(Table t) {
            t.clearChildren();
            t.left();

            if (hasLevel) {
                select(t, "level", levels, level, o -> level = o, 1, 80f);
            }

            field(t, params[0], str -> params[0] = str).width(LCanvas.getTargetWidth() - Scl.scl(20f)).padRight(3);

            if (expanded) {
                for (int i = 1; i < params.length; i++) {
                    final int index = i;
                    fields(t, "p" + i, false, params[index], v -> params[index] = v);
                }
            } else {
                t.button(b -> {
                    b.add("add parameters").color(Color.gray);
                    b.clicked(() -> { expanded = true; rebuild(t); });
                }, Styles.logict, () -> {}).size(180f, 40f).left().pad(4f).color(t.color);
            }
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            if (hasLevel) writer.write(level.name());
            for (String param : params) writer.write(param.isEmpty() ? "null" : param);
            writer.end();
        }

        protected LStatement readTokens(String[] tokens) {
            int i = 1;
            if (hasLevel && tokens.length > i) level = Log.LogLevel.valueOf(tokens[i++]);
            for (int j = 0; j < 10; j++) {
                if (tokens.length > i) params[j] = tokens[i++];
            }
            return this;
        }
    }

    public static class AssertStatement extends AbstractAssertStatement {
        public static final String opcode = "assert";

        public ConditionOp op = ConditionOp.equal;
        public String value = "x", compare = "false";

        public AssertStatement() {
            super("Assert");
        }

        protected void rebuild(Table t) {
            t.clearChildren();
            t.left();

            JumpStatement.addOp(this, t, op, o -> {
                op = o;
                rebuild(t);
            }, value, str -> value = str, compare, str -> compare = str);

            message(t, "message", "Assertion [gold]{1}[] {3} [gold]{2}[] failed.");
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.AssertI(op, builder.var(value), builder.var(compare), builder.var(message));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(op.name());
            writer.write(value);
            writer.write(compare);
            writer.write(message);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            AssertStatement stmt = new AssertStatement();
            int i = 1;
            if (tokens.length > i) stmt.op = ConditionOp.valueOf(tokens[i++]);
            if (tokens.length > i) stmt.value = tokens[i++];
            if (tokens.length > i) stmt.compare = tokens[i++];
            if (tokens.length > i) stmt.message = tokens[i++];
            return stmt;
        }
    }

    public static class AssertBoundsStatement extends AbstractAssertStatement {
        private static final ConditionOp[] ops = {ConditionOp.lessThan, ConditionOp.lessThanEq};
        public static final String opcode = "assertBounds";
        public AssertionType type = AssertionType.integer;
        public String multiple = "2";
        public String min = "0";
        public ConditionOp opMin = ConditionOp.lessThanEq;
        public String value = "index";
        public ConditionOp opMax = ConditionOp.lessThanEq;
        public String max = "10";

        public AssertBoundsStatement() {
            super("Assert Bounds");
        }

        @Override
        protected void rebuild(Table table) {
            table.clearChildren();
            table.left();

            field(table, value, str -> value = str);
            select(table, "is", AssertionType.all, type, o -> type = o, 2, 110f);
            if (type == AssertionType.multiple) {
                fields(table, "of", false, multiple, str -> multiple = str);
            }
            fields(table, "where", false, min, str -> min = str);
            select(table, "", ops, opMin, o -> opMin = o, 2, 64f);
            table.add("value").padLeft(10);
            select(table, "", ops, opMax, o -> opMax = o, 2, 64f);
            fields(table, min, str -> min = str);
            message(table, "message", "Index out of bounds: {1}{4}{2}{5}{3}.");
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.AssertBoundsI(type, builder.var(multiple),
                    builder.var(min), opMin, builder.var(value), opMax, builder.var(max),
                    builder.var(message));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(type.name());
            writer.write(multiple);
            writer.write(min);
            writer.write(opMin.name());
            writer.write(value);
            writer.write(opMax.name());
            writer.write(max);
            writer.write(message);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            AssertBoundsStatement stmt = new AssertBoundsStatement();
            int i = 1;
            if (tokens.length > i) stmt.type = AssertionType.valueOf(tokens[i++]);
            if (tokens.length > i) stmt.multiple = tokens[i++];
            if (tokens.length > i) stmt.min = tokens[i++];
            if (tokens.length > i) stmt.opMin = conditionOp(tokens[i++]);
            if (tokens.length > i) stmt.value = tokens[i++];
            if (tokens.length > i) stmt.opMax = conditionOp(tokens[i++]);
            if (tokens.length > i) stmt.max = tokens[i++];
            if (tokens.length > i) stmt.message = tokens[i++];
            return stmt;
        }

        private static ConditionOp conditionOp(String name) {
            ConditionOp op = ConditionOp.valueOf(name);
            if (op != ConditionOp.lessThan && op != ConditionOp.lessThanEq) {
                throw new IllegalArgumentException("Unsupported condition op: " + name);
            }
            return op;
        }
    }

    public static class AssertEqualsStatement extends AbstractAssertStatement {
        public static final String opcode = "assertequals";
        public String expected = "0";
        public String actual = "value";

        public AssertEqualsStatement() {
            super("Assert Equals");
        }


        @Override
        protected void rebuild(Table t) {
            t.clearChildren();
            t.left();
            fields(t, "expected", false, expected, v -> expected = v);
            fields(t, "actual", false, actual, v -> actual = v);
            message(t, "message", "Assertion failed: expected [gold]{1}[], got [gold]{2}[].");
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.AssertEqualsI(builder.var(expected),
                    builder.var(actual), builder.var(message));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(expected);
            writer.write(actual);
            writer.write(message);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            AssertEqualsStatement stmt = new AssertEqualsStatement();
            int i = 1;
            if (tokens.length > i) stmt.expected = tokens[i++];
            if (tokens.length > i) stmt.actual = tokens[i++];
            if (tokens.length > i) stmt.message = tokens[i++];
            return stmt;
        }
    }

    public static class AssertFlushStatement extends AbstractAssertStatement {
        public static final String opcode = "assertflush";
        public String position = "position";

        public AssertFlushStatement() {
            super("Assert Flush");
        }

        @Override
        protected void rebuild(Table t) {
            t.clearChildren();
            t.left();
            fields(t, "position", false, position, v -> position = v);
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.AssertFlushI(builder.var(position));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(position);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            AssertFlushStatement stmt = new AssertFlushStatement();
            int i = 1;
            if (tokens.length > i) stmt.position = tokens[i++];
            return stmt;
        }
    }

    public static class AssertPrintsStatement extends AbstractAssertStatement {
        public static final String opcode = "assertprints";
        public String position = "position";
        public String expected = "\"frog\"";

        public AssertPrintsStatement() {
            super("Assert Prints");
        }

        @Override
        protected void rebuild(Table t) {
            t.clearChildren();
            t.left();
            fields(t, "position", false, position, v -> position = v);
            fields(t, "expected", false, expected, v -> expected = v);
            message(t, "message", "Assertion failed: expected [gold]{1}[], got [gold]{2}[].");
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.AssertPrintsI(builder.var(position),
                    builder.var(expected), builder.var(message));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(position);
            writer.write(expected);
            writer.write(message);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            AssertPrintsStatement stmt = new AssertPrintsStatement();
            int i = 1;
            if (tokens.length > i) stmt.position = tokens[i++];
            if (tokens.length > i) stmt.expected = tokens[i++];
            if (tokens.length > i) stmt.message = tokens[i++];
            return stmt;
        }
    }

    public static class AssertTypeStatement extends AbstractAssertStatement {
        public static final String opcode = "asserttype";
        public AssertionDataType expectedType = AssertionDataType.unit;
        public String actualValue = "@unit";

        public AssertTypeStatement() {
            super("Assert Type");
        }

        @Override
        protected void rebuild(Table t) {
            t.clearChildren();
            t.left();
            fields(t, actualValue, v -> actualValue = v);
            select(t, "is", AssertionDataType.all, expectedType, o -> expectedType = o, 2, 160f);
            message(t, "message", "Assertion failed: expected [gold]{1}[], got [gold]{2}[].");
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.AssertTypeI(expectedType, builder.var(actualValue), builder.var(message));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(expectedType.name());
            writer.write(actualValue);
            writer.write(message);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            AssertTypeStatement stmt = new AssertTypeStatement();
            int i = 1;
            if (tokens.length > i) stmt.expectedType = AssertionDataType.valueOf(tokens[i++]);
            if (tokens.length > i) stmt.actualValue = tokens[i++];
            if (tokens.length > i) stmt.message = tokens[i++];
            return stmt;
        }
    }

    public static class BreakpointStatement extends AbstractAssertStatement {
        public static final String opcode = "breakpoint";

        public ConditionOp op = ConditionOp.always;
        public String value = "x", compare = "false";

        public BreakpointStatement() {
            super("Breakpoint");
        }

        protected void rebuild(Table table) {
            table.clearChildren();
            table.left();

            if (op != ConditionOp.always) table.add("when").padLeft(10);
            JumpStatement.addOp(this, table, op, o -> {
                op = o;
                rebuild(table);
            }, value, str -> value = str, compare, str -> compare = str);
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.BreakpointI(op, builder.var(value), builder.var(compare));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(op.name());
            writer.write(value);
            writer.write(compare);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            BreakpointStatement stmt = new BreakpointStatement();
            int i = 1;
            if (tokens.length > i) stmt.op = ConditionOp.valueOf(tokens[i++]);
            if (tokens.length > i) stmt.value = tokens[i++];
            if (tokens.length > i) stmt.compare = tokens[i++];
            return stmt;
        }
    }

    public static class ErrorStatement extends AbstractMessageStatement {
        public static final String opcode = "error";

        public ErrorStatement() {
            super(opcode, "Error", "Runtime error");
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            LVar[] vars = new LVar[10];
            for (int i = 0; i < params.length; i++) vars[i] = builder.var(params[i]);
            return new LogicInstructions.ErrorI(vars);
        }

        public static LStatement read(String[] tokens) {
            return new ErrorStatement().readTokens(tokens);
        }
    }

    private static final Log.LogLevel[] levels = {
            Log.LogLevel.err,
            Log.LogLevel.warn,
            Log.LogLevel.info,
            Log.LogLevel.debug,
    };

    public static class LogStatement extends AbstractMessageStatement {
        public static final String opcode = "log";

        public LogStatement() {
            super(opcode, "Log", "Logging a message");
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            LVar[] vars = new LVar[10];
            for (int i = 0; i < params.length; i++) vars[i] = builder.var(params[i]);
            return new LogicInstructions.LogI(level, vars);
        }

        public static LStatement read(String[] tokens) {
            return new LogStatement().readTokens(tokens);
        }
    }

    public static class ProfileStatement extends AbstractAssertStatement {
        public static final String opcode = "profile";

        public ProfilingCommand command = ProfilingCommand.start;
        public String block = "@this";

        public ProfileStatement() {
            super("Profile");
        }

        protected void rebuild(Table t) {
            t.clearChildren();
            t.left();

            select(t, "", ProfilingCommand.all, command, o -> command = o, 1, 70f);
            fields(t, "profiling of", false, block, str -> block = str);
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.ProfileI(command, builder.var(block));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(command.name());
            writer.write(block);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            ProfileStatement stmt = new ProfileStatement();
            int i = 1;
            if (tokens.length > i) stmt.command = ProfilingCommand.valueOf(tokens[i++]);
            if (tokens.length > i) stmt.block = tokens[i++];
            return stmt;
        }
    }

    public static class RestartStatement extends AbstractAssertStatement {
        public static final String opcode = "restart";

        public String block = "@this";

        public RestartStatement() {
            super("Restart");
        }

        protected void rebuild(Table t) {
            t.clearChildren();
            t.left();

            fields(t, block, str -> block = str);
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.RestartI(builder.var(block));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(block);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            RestartStatement stmt = new RestartStatement();
            int i = 1;
            if (tokens.length > i) stmt.block = tokens[i++];
            return stmt;
        }
    }

    public static class SnapshotStatement extends AbstractAssertStatement {
        public static final String opcode = "snapshot";

        public SnapshotType type = SnapshotType.isolated;
        public String block = "@this";
        public String steps = "20";

        public SnapshotStatement() {
            super("Snapshot");
        }

        protected void rebuild(Table t) {
            t.clearChildren();
            t.left();

            select(t, "create", SnapshotType.all, type, o -> type = o, 2, 130f);
            if (type == SnapshotType.recording) {
                fields(t, "snapshot of the next", false, steps, str -> steps = str).width(75f);
                fields(t, "steps in", false, block, str -> block = str);
            } else {
                if (type == SnapshotType.global) {
                    t.add("snapshot");
                } else {
                    fields(t, "snapshot of", false, block, str -> block = str);
                }

                message(t, "name", "Snapshot created at #{@counter}.");
            }
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.SnapshotI(type, builder.var(block), builder.var(steps), builder.var(message));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(type.name());
            writer.write(block);
            writer.write(steps);
            writer.write(message);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            SnapshotStatement stmt = new SnapshotStatement();
            int i = 1;
            if (tokens.length > i) stmt.type = SnapshotType.valueOf(tokens[i++]);
            if (tokens.length > i) stmt.block = tokens[i++];
            if (tokens.length > i) stmt.steps = tokens[i++];
            if (tokens.length > i) stmt.message = tokens[i++];
            return stmt;
        }
    }
}
