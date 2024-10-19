package tech.skidonion.obfuscator.utils.commons;

import tech.skidonion.obfuscator.mba.expr.Expr;

import java.util.ArrayList;

public class Subs {
    private final ArrayList<Pair<String, Expr>> subs;

    public Subs() {
        subs = new ArrayList<>();
    }

    public ArrayList<Pair<String, Expr>> getSubs() {
        return subs;
    }

    /**
     * Adds a new substitution.
     */
    public String add(Expr expr) {
        // Do we have this expression stored already?
        for (Pair<String, Expr> sub : subs) {
            if (sub.getSecond() == expr) {
                return sub.getFirst();
            }
        }
        // Create a new substitution variable.
        String var = String.format("_sub_%s", this.subs.size());
        this.subs.add(new Pair<>(var, expr));
        return var;
    }
}
