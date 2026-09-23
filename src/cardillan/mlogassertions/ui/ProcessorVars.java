package cardillan.mlogassertions.ui;

import mindustry.logic.LExecutor;
import mindustry.logic.LVar;

import java.util.Arrays;
import java.util.Comparator;

public class ProcessorVars implements VariableValues {
    private final LExecutor executor;
    private final LVar[] vars;
    private int length;

    public ProcessorVars(LExecutor executor) {
        this.executor = executor;
        this.vars = new LVar[executor.vars.length + 2 + (executor.privileged ? 1 : 0)];
        this.length = 0;
    }

    @Override
    public boolean processor() {
        return true;
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public String label(int index, boolean hex) {
        return vars[index].name;
    }

    @Override
    public boolean isObj(int index) {
        return vars[index].isobj;
    }

    @Override
    public Object obj(int index) {
        return vars[index].objval;
    }

    @Override
    public double num(int index) {
        return vars[index].numval;
    }

    @Override
    public String textBuffer() {
        return executor.textBuffer.toString();
    }

    @Override
    public void clear() {
        // Do nothing
    }

    @Override
    public void setView(boolean sorted, boolean hideTemps) {
        length = 0;
        vars[length++] = executor.counter;
        vars[length++] = executor.unit;
        vars[length++] = executor.ipt;
        if (executor.privileged) {
            vars[length++] = executor.queryResult;
        }

        int start = length;

        for (int i = 1; i < executor.vars.length; i++) {
            if (hideTemps && isTemp(executor.vars[i].name)) continue;
            vars[length++] = executor.vars[i];
        }

        if (sorted) {
            Arrays.sort(vars, start, length, Comparator.comparing(a -> a.name, mindcodeOrder));
        }
    }

    private enum VariableClass {
        builtin, uncategorized, global, main, local, compiler, temporary;
    }

    private static VariableClass variableClass(String name) {
        if (name.isEmpty()) return VariableClass.uncategorized;
        return switch (name.charAt(0)) {
            case '@' -> VariableClass.builtin;
            case '.' -> VariableClass.global;
            case ':' -> name.indexOf(':', 1) < 0 ? VariableClass.main : VariableClass.local;
            case '*' -> isTemp(name) ? VariableClass.temporary : VariableClass.compiler;
            case '_' -> VariableClass.temporary;
            default -> VariableClass.uncategorized;
        };
    }

    private static boolean isTemp(String name) {
        return name.startsWith("*tmp");
    }

    private static final Comparator<String> mindcodeOrder = (a, b) -> {
        if (a.isEmpty() || b.isEmpty()) return Integer.compare(a.length(), b.length());

        VariableClass va = variableClass(a);
        VariableClass vb = variableClass(b);
        if (va != vb) return va.compareTo(vb);

        int ia = 0;
        int ib = 0;

        while (ia < a.length() && ib < b.length()) {
            char ca = a.charAt(ia);
            char cb = b.charAt(ib);

            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                while (ia < a.length() && a.charAt(ia) == '0') ia++;
                while (ib < b.length() && b.charAt(ib) == '0') ib++;

                int sa = ia, na = ia;
                int sb = ib, nb = ib;

                while (ia < a.length() && Character.isDigit(a.charAt(ia))) ia++;
                while (ib < b.length() && Character.isDigit(b.charAt(ib))) ib++;

                if (ia - na != ib - nb) {
                    return Integer.compare(ia - na, ib - nb);
                }

                while (na < ia) {
                    int comparison = Character.compare(a.charAt(na), b.charAt(nb));
                    if (comparison != 0) {
                        return comparison;
                    }
                    na++;
                    nb++;
                }

                if (sa - na != sb - nb) {
                    return Integer.compare(sa - na, sb - nb);
                }
            } else {
                int comparison = Character.compare(ca, cb);
                if (comparison != 0) {
                    return comparison;
                }
                ia++;
                ib++;
            }
        }

        return Integer.compare(a.length(), b.length());
    };
}
