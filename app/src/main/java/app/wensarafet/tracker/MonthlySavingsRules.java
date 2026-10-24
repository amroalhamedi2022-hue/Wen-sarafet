package app.wensarafet.tracker;

/** Cap comparison, not an actual transfer into savings or a new account. */
public final class MonthlySavingsRules {
    private MonthlySavingsRules() {}
    public static final class State {
        public final boolean configured, exceeded, completed;
        public final long remainderMinor, savedMinor, overspendMinor;
        private State(boolean configured, boolean exceeded, boolean completed,
                      long remainderMinor, long savedMinor, long overspendMinor) {
            this.configured=configured; this.exceeded=exceeded; this.completed=completed;
            this.remainderMinor=remainderMinor; this.savedMinor=savedMinor; this.overspendMinor=overspendMinor;
        }
    }
    public static State calculate(long capMinor, long qualifyingExpenseMinor, boolean monthCompleted) {
        if (capMinor <= 0) return new State(false,false,monthCompleted,0,0,0);
        if (qualifyingExpenseMinor < 0) throw new IllegalArgumentException("invalid expense total");
        long remainder=Math.subtractExact(capMinor,qualifyingExpenseMinor);
        return new State(true,remainder<0,monthCompleted,Math.max(0,remainder),
                monthCompleted?Math.max(0,remainder):0,remainder<0?Math.negateExact(remainder):0);
    }
}
