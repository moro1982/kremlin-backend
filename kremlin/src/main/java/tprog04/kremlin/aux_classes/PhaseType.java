package tprog04.kremlin.aux_classes;

public enum PhaseType {
    CURES(1),
    PURGE(2),
    SPY_INVESTIGATION(3),
    HEALTH(4),
    FUNERAL_COMMISSION(5),
    REPLACEMENT(6),
    REHABILITATION(7),
    PARADE(8);

    private final int order;

    PhaseType(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }

    public static PhaseType fromOrder(int order) {
        for (PhaseType phase : values()) {
            if (phase.getOrder() == order) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Orden de Fase inválido: " + order);
    }


}
