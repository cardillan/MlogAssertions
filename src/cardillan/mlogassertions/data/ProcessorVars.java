package cardillan.mlogassertions.data;

import arc.func.Cons;
import mindustry.gen.Building;
import mindustry.logic.LExecutor;
import mindustry.logic.LVar;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

import java.util.Arrays;
import java.util.Comparator;

public class ProcessorVars extends LogicVariableValues {
    public final LogicBuild build;
    public final LExecutor executor;
    public final LVar[] data;
    public final LVar[] view;
    public final int start;
    public int length;

    public ProcessorVars(LogicBuild build) {
        this.build = build;
        this.executor = build.executor;
        this.data = new LVar[executor.vars.length + 2 + (executor.privileged ? 1 : 0)];
        int length = 0;

        // Copy the original dat
        data[length++] = get(executor.counter);
        data[length++] = get(executor.unit);
        data[length++] = get(executor.ipt);
        if (executor.privileged) {
            data[length++] = get(executor.queryResult);
        }

        start = length;

        for (int i = 1; i < executor.vars.length; i++) {
            data[length++] = get(executor.vars[i]);
        }

        view = Arrays.copyOf(data, length);
    }

    protected LVar get(LVar var) {
        return var;
    }

    @Override
    public Building building() {
        return build;
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
        return view[index].name;
    }

    @Override
    public boolean isObj(int index) {
        return view[index].isobj;
    }

    @Override
    public boolean isLink(int index) {
        return view[index].constant && view[index].name.charAt(0) != '@';
    }

    @Override
    public Object obj(int index) {
        return view[index].objval;
    }

    @Override
    public double num(int index) {
        return view[index].numval;
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
    public void setView(boolean sorted, boolean hideTemps, boolean hideLinks) {
        length = 0;
        for (int i = 0; i < data.length; i++) {
            if (hideTemps && isTemp(data[i].name)) continue;
            if (hideLinks && data[i].constant && data[i].name.charAt(0) != '@') continue;
            view[length++] = data[i];
        }

        if (sorted) {
            Arrays.sort(view, start, length, mindcodeOrder);
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

    private static final Comparator<LVar> mindcodeOrder = (la, lb) -> {
        String a = la.name, b = lb.name;

        if (a.isEmpty() || b.isEmpty()) return Integer.compare(a.length(), b.length());

        VariableClass va = variableClass(a), vb = variableClass(b);
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

    @Override
    public void eachObject(Cons<Object> getter) {
        for (int index = 0; index < data.length; index++) {
            if (data[index].isobj) getter.get(data[index].objval);
        }
    }
}
