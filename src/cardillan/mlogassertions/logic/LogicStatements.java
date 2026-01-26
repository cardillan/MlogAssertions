package cardillan.mlogassertions.logic;

import arc.func.Cons;
import arc.func.Func;
import arc.func.Prov;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import mindustry.gen.LogicIO;
import mindustry.logic.LAssembler;
import mindustry.logic.LCategory;
import mindustry.logic.LExecutor;
import mindustry.logic.LStatement;
import mindustry.logic.LVar;
import mindustry.ui.Styles;

public class LogicStatements {
    private static final LogicStatementWriter writer = new LogicStatementWriter();

    public static void register() {
        register(AssertBoundsStatement::new, AssertBoundsStatement.opcode, AssertBoundsStatement::read);
        register(AssertEqualsStatement::new, AssertEqualsStatement.opcode, AssertEqualsStatement::read);
        register(AssertFlushStatement::new, AssertFlushStatement.opcode, AssertFlushStatement::read);
        register(AssertPrintsStatement::new, AssertPrintsStatement.opcode, AssertPrintsStatement::read);
        register(ErrorStatement::new, ErrorStatement.opcode, ErrorStatement::read);
    }

    private static void register(Prov<LStatement> prov, String opcode, Func<String[], LStatement> parser) {
        LogicIO.allStatements.add(prov);
        LAssembler.customParsers.put(opcode, parser);
    }

    private static abstract class AssertStatement extends LStatement {
        final String name;

        public AssertStatement(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        protected Cell<TextField> numField(Table table, String num, Cons<String> getter) {
            return field(table, num, getter).width(120).left();
        }
    }

    private static class AssertBoundsStatement extends AssertStatement {
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
        public LCategory category() {
            return AssertLogic.assertsCategory;
        }

        @Override
        public void build(Table t) {
            t.defaults().left();
            t.clearChildren();
            t.left();
            t.add("Value type ").color(category().color).padLeft(4);
            t.table(table -> {
                table.left();
                table.color.set(category().color);
                table.add("value of ").padLeft(4);
                numField(table, value, str -> value = str);
                table.add(" is ").padLeft(4);
                table.button(b -> {
                    b.label(() -> type.name());
                    b.clicked(() -> showSelect(b, AssertionType.all, type, o -> {
                        type = o;
                        build(t);
                    }, 2, cell -> cell.size(110, 50)));
                }, Styles.logict, () -> {}).size(110, 40).left().pad(4f).color(table.color);
                if (type == AssertionType.multiple) {
                    table.add(" of ");
                    numField(table, multiple, str -> multiple = str);
                }
                table.add("").growX();
            });
            t.row();
            t.add("Bounds ").color(category().color).padLeft(4);
            t.table(table -> {
                table.left();
                table.color.set(category().color);
                numField(table, min, str -> min = str);
                opButton(t, table, opMin, o -> opMin = o);
                table.add(" value ");
                opButton(t, table, opMax, o -> opMax = o);
                numField(table, max, str -> max = str);
                table.add("").growX();
            });
            t.row();
            t.add("Message").color(category().color).padLeft(4);
            field(t, message, str -> message = str).width(0f).maxTextLength(64).growX().padRight(3);
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

    private static class AssertEqualsStatement extends AssertStatement {
        public static final String opcode = "assertequals";
        public String expected = "0";
        public String actual = "value";
        public String message = "\"value should be equal to 0\"";

        public AssertEqualsStatement() {
            super("Assert Equals");
        }

        @Override
        public LCategory category() {
            return AssertLogic.assertsCategory;
        }

        @Override
        public void build(Table table) {
            table.defaults().left();

            table.add(" expected ").self(this::param);
            numField(table, expected, v -> expected = v);
            table.add(" actual ").self(this::param);
            numField(table, actual, v -> actual = v);
            table.add(" message ").self(this::param);
            field(table, message, str -> message = str).width(0f).growX().padRight(3);
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

    private static class AssertFlushStatement extends AssertStatement {
        public static final String opcode = "assertflush";
        public String position = "position";

        public AssertFlushStatement() {
            super("Assert Flush");
        }

        @Override
        public LCategory category() {
            return AssertLogic.assertsCategory;
        }

        @Override
        public void build(Table table) {
            table.add(" position ").self(this::param);
            numField(table, position, v -> position = v);
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

    private static class AssertPrintsStatement extends AssertStatement {
        public static final String opcode = "assertprints";
        public String position = "position";
        public String expected = "\"frog\"";
        public String message = "\"text output should be equal to 'frog'\"";

        public AssertPrintsStatement() {
            super("Assert Prints");
        }

        @Override
        public LCategory category() {
            return AssertLogic.assertsCategory;
        }

        @Override
        public void build(Table table) {
            table.defaults().left();
            table.add(" position ").self(this::param);
            numField(table, position, v -> position = v);
            table.add(" expected ").self(this::param);
            field(table, expected, v -> expected = v);
            table.add(" message ").self(this::param);
            field(table, message, str -> message = str).width(0f).growX().padRight(3);
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

    private static class ErrorStatement extends AssertStatement {
        public static final String opcode = "error";
        public String[] params = new String[10];

        public ErrorStatement() {
            super("Error");
            params[0] = "\"Runtime error at #[[1]\"";
            params[1] = "@counter";
            for (int i = 2; i < params.length; i++) params[i] = "null";
        }

        @Override
        public LCategory category() {
            return AssertLogic.assertsCategory;
        }

        @Override
        public void build(Table table) {
            table.defaults().left();
            Cell<Table> t1 = table.table().growX().left();
            t1.get().add(" message ").self(this::param).left();
            field(t1.get(), params[0], str -> params[0] = str).width(0f).growX().padRight(3);
            table.row();
            Cell<Table> t2 = table.table().growX().left();
            t2.get().left();
            for (int i = 1; i < params.length; i++) {
                final int index = i;
                t2.get().add(" p" + i + " ").self(this::param);
                field(t2.get(), params[index], v -> params[index] = v);
                if (i % 3 == 0) t2.get().row();
            }
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder) {
            LVar[] vars = new LVar[10];
            for (int i = 0; i < params.length; i++) vars[i] = builder.var(params[i]);
            return new LogicInstructions.ErrorI(vars);
        }

        @Override
        public void write(StringBuilder builder) {
            writer.start(builder);
            writer.write(opcode);
            for (String param : params) writer.write(param);
            writer.end();
        }

        public static LStatement read(String[] tokens) {
            ErrorStatement stmt = new ErrorStatement();
            int i = 1;
            for (int j = 0; j < 10; j++) {
                if (tokens.length > i) stmt.params[i - 1] = tokens[i++];
            }
            return stmt;
        }
    }
}
