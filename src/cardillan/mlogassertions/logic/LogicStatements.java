package cardillan.mlogassertions.logic;

import arc.func.Cons;
import arc.func.Func;
import arc.func.Prov;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.gen.LogicIO;
import mindustry.logic.*;
import mindustry.ui.Styles;

public class LogicStatements {
    private static final LogicStatementWriter writer = new LogicStatementWriter();

    public static void register() {
        register(AssertBoundsStatement::new, AssertBoundsStatement.opcode, AssertBoundsStatement::read);
        register(AssertEqualsStatement::new, AssertEqualsStatement.opcode, AssertEqualsStatement::read);
        register(AssertTypeStatement::new, AssertTypeStatement.opcode, AssertTypeStatement::read);
        register(AssertFlushStatement::new, AssertFlushStatement.opcode, AssertFlushStatement::read);
        register(AssertPrintsStatement::new, AssertPrintsStatement.opcode, AssertPrintsStatement::read);
        register(ErrorStatement::new, ErrorStatement.opcode, ErrorStatement::read);
        register(LogStatement::new, LogStatement.opcode, LogStatement::read);
        register(BreakpointStatement::new, BreakpointStatement.opcode, BreakpointStatement::read);
    }

    private static void register(Prov<LStatement> prov, String opcode, Func<String[], LStatement> parser) {
        LogicIO.allStatements.add(prov);
        LAssembler.customParsers.put(opcode, parser);
    }

    public static abstract class AssertStatement extends LStatement {
        final String name;

        public AssertStatement(String name) {
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

        protected void stretchRow(Table table){
            if(LCanvas.useRows()){
                table.add("").growX().row();
            }
        }

        protected void message(Table table, String value, Cons<String> setter) {
            field(table, value, setter).width(LCanvas.useRows() ? 280f : 0f).growX().padRight(3);
        }

        protected void subtable(Table table, Cons<Table> builder) {
            table.table(t ->{
                t.left();
                t.color.set(category().color);
                builder.get(t);
            }).growX();
        };
    }

    public static class AssertBoundsStatement extends AssertStatement {
        public static final String opcode = "assertBounds";
        public AssertionType type = AssertionType.integer;
        public String multiple = "2";
        public String min = "0";
        public AssertOp opMin = AssertOp.lessThanEq;
        public String value = "index";
        public AssertOp opMax = AssertOp.lessThanEq;
        public String max = "10";
        public String message = "\"Index out of bounds (0 to 10).\"";

        public AssertBoundsStatement() {
            super("Assert Bounds");
        }

        @Override
        public void build(Table t) {
            t.defaults().left();
            t.clearChildren();
            t.left();
            t.add("Value type ").color(category().color).padLeft(4);
            row(t);
            subtable(t, table -> {
                table.add("value of ").padLeft(4);
                field(table, value, str -> value = str);
                table.add(" is ").padLeft(4);
                table.button(b -> {
                    b.label(() -> type.name());
                    b.clicked(() -> showSelect(b, AssertionType.all, type, o -> {
                        type = o;
                        build(t);
                    }, 2, cell -> cell.size(110, 50)));
                }, Styles.logict, () -> {}).size(108, 40).left().pad(4f).color(table.color);
                if (type == AssertionType.multiple) {
                    row(table);
                    table.add(" of ");
                    numField(table, multiple, str -> multiple = str);
                }
            });
            t.row();
            t.add("Bounds ").color(category().color).padLeft(4);
            row(t);
            subtable(t, table -> {
                numField(table, min, str -> min = str);
                opButton(t, table, opMin, o -> opMin = o);
                table.add(" value ");
                opButton(t, table, opMax, o -> opMax = o);
                numField(table, max, str -> max = str);
            });
            t.row();
            t.add("Message").color(category().color).padLeft(4);
            row(t);
            field(t, message, str -> message = str).width(0f).growX().padRight(3);
        }

        void numField(Table table, String value, Cons<String> setter) {
            field(table, value, setter).width(84f).left();
        }

        void opButton(Table parent, Table table, AssertOp op, Cons<AssertOp> getter) {
            table.button(b -> {
                b.label(() -> op.symbol);
                b.clicked(() -> showSelect(b, AssertOp.all, op, o -> {
                    getter.get(o);
                    build(parent);
                }));
            }, Styles.logict, () -> {
            }).size(64f, 40f).left().pad(4f).color(table.color);
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
            if (tokens.length > i) stmt.opMin = AssertOp.valueOf(tokens[i++]);
            if (tokens.length > i) stmt.value = tokens[i++];
            if (tokens.length > i) stmt.opMax = AssertOp.valueOf(tokens[i++]);
            if (tokens.length > i) stmt.max = tokens[i++];
            if (tokens.length > i) stmt.message = tokens[i++];
            return stmt;
        }
    }

    public static class AssertEqualsStatement extends AssertStatement {
        public static final String opcode = "assertequals";
        public String expected = "0";
        public String actual = "value";
        public String message = "\"value should be equal to 0\"";

        public AssertEqualsStatement() {
            super("Assert Equals");
        }

        @Override
        public void build(Table table) {
            table.defaults().left();

            table.add(" expected ").self(this::param);
            field(table, expected, v -> expected = v);
            stretchRow(table);
            table.add(" actual ").self(this::param);
            field(table, actual, v -> actual = v);
            stretchRow(table);
            table.add(" message ").self(this::param);
            message(table, message, str -> message = str);
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

    public static class AssertFlushStatement extends AssertStatement {
        public static final String opcode = "assertflush";
        public String position = "position";

        public AssertFlushStatement() {
            super("Assert Flush");
        }

        @Override
        public void build(Table table) {
            table.add(" position ").self(this::param);
            field(table, position, v -> position = v);
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

    public static class AssertPrintsStatement extends AssertStatement {
        public static final String opcode = "assertprints";
        public String position = "position";
        public String expected = "\"frog\"";
        public String message = "\"text output should be equal to 'frog'\"";

        public AssertPrintsStatement() {
            super("Assert Prints");
        }

        @Override
        public void build(Table table) {
            table.defaults().left();
            table.add(" position ").self(this::param);
            field(table, position, v -> position = v);
            stretchRow(table);
            table.add(" expected ").self(this::param);
            field(table, expected, v -> expected = v);
            stretchRow(table);
            table.add(" message ").self(this::param);
            message(table, message, str -> message = str);
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

    public static class AssertTypeStatement extends AssertStatement {
        public static final String opcode = "asserttype";
        public String value = "@unit";
        public AssertDataType type = AssertDataType.unit;
        public String message = "\"@unit should be a unit\"";

        public AssertTypeStatement() {
            super("Assert Type");
        }

        @Override
        public void build(Table table) {
            table.defaults().left();
            table.clearChildren();
            table.left();

            if(LCanvas.useRows()) {
                subtable(table, subtable -> createValues(table, subtable));
                row(table);
                subtable(table, this::createMessage);
            } else {
                createValues(table, table);
                createMessage(table);
            }
        }

        private void createValues(Table root, Table table) {
            field(table, value, v -> value = v);
            table.add(" is ").self(this::param);
            table.button(b -> {
                b.label(() -> type.name());
                b.clicked(() -> showSelect(b, AssertDataType.all, type, o -> {
                    type = o;
                    build(root);
                }, 2, cell -> cell.size(110, 50)));
            }, Styles.logict, () -> {
            }).size(108, 40).left().pad(4f).color(table.color);
        }

        private void createMessage(Table table) {
            table.add(" message ").self(this::param);
            message(table, message, str -> message = str);
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            return new LogicInstructions.AssertTypeI(builder.var(value), type, builder.var(message));
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            writer.write(value);
            writer.write(type.name());
            writer.write(message);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            AssertTypeStatement stmt = new AssertTypeStatement();
            int i = 1;
            if (tokens.length > i) stmt.value = tokens[i++];
            if (tokens.length > i) stmt.type = AssertDataType.valueOf(tokens[i++]);
            if (tokens.length > i) stmt.message = tokens[i++];
            return stmt;
        }
    }

    public abstract static class MessageStatement extends AssertStatement {
        private final String opcode;
        private final boolean hasLevel;
        public Log.LogLevel level = Log.LogLevel.info;
        public String[] params = new String[10];

        public MessageStatement(String opcode, String name, String message) {
            super(name);
            this.opcode = opcode;
            this.hasLevel = opcode.equals("log");
            params[0] = "\"" + message + " at #[[1]\"";
            params[1] = "@counter";
            for (int i = 2; i < params.length; i++) params[i] = "null";
        }

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            table.defaults().left();
            Table t1 = table.table().growX().left().get();
            if (hasLevel) {
                t1.button(b -> {
                    b.label(() -> level.name());
                    b.clicked(() -> showSelect(b, levels, level, o -> {
                        level = o;
                        rebuild(table);
                    }, 1, cell -> cell.width(80)));
                }, Styles.logict, () -> {
                }).size(80f, 40f).left().pad(4f).color(table.color);
            }
            t1.setColor(category().color);
            t1.add(" message ").self(this::param).left();
            message(t1, params[0], str -> params[0] = str);
            table.row();
            Table t2 = table.table().growX().left().get().left();
            t2.setColor(category().color);

            for (int i = 1; i < params.length; i++) {
                final int index = i;
                t2.add(" p" + i + " ").self(this::param);
                field(t2, params[index], v -> params[index] = v).width(LCanvas.useRows() ? 150f : 220f);
                if (LCanvas.useRows()) {
                    if (i % 2 == 0) row(t2);
                } else {
                    if (i % 3 == 0) t2.row();
                }
            }
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            if (hasLevel) writer.write(level.name());
            for (String param : params) writer.write(param);
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

    public static class BreakpointStatement extends AssertStatement {
        public static final String opcode = "breakpoint";

        public ConditionOp op = ConditionOp.always;
        public String value = "x", compare = "false";

        public BreakpointStatement() {
            super("Breakpoint");
        }

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            if (op == ConditionOp.always) {
                table.add("trigger ").padLeft(10).left();
            } else {
                table.add("trigger when ").padLeft(10).left();
                row(table);
            }

            addOp(table, op, o -> {
                op = o;
                rebuild(table);
            }, value, str -> value = str, compare, str -> compare = str);
        }

        public void addOp(Table t, ConditionOp op, Cons<ConditionOp> getter, String comp0, Cons<String> set0, String comp1, Cons<String> set2){
            if(op != ConditionOp.always) field(t, comp0, set0);

            t.button(b -> {
                b.add(op.symbol);
                b.clicked(() -> showSelect(b, ConditionOp.all, op, getter));
            }, Styles.logict, () -> {
            }).size(op == ConditionOp.always ? 80f : 48f, 40f).pad(4f).color(t.color);

            if(op != ConditionOp.always) field(t, comp1, set2);
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder){
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

    public static class ErrorStatement extends MessageStatement {
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

    public static class LogStatement extends MessageStatement {
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
}
