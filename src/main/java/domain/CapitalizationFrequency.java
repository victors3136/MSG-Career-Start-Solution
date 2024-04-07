package domain;

public enum CapitalizationFrequency {
    MONTHLY,
    QUARTERLY;

    public byte getMonthIncrement() {
        return switch (this) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
        };
    }
}

