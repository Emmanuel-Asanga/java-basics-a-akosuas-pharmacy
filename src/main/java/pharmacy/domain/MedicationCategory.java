package pharmacy.domain;

import java.util.Optional;

/**
 * The kinds of medicine stock. The rules that depend on the kind live here, in one place,
 * instead of in a chain of {@code if} statements inside the dispensing code.
 */
public enum MedicationCategory {

    /** Over the counter: anyone can buy it. */
    OTC(false),

    /** Prescription only: a prescriber must be given. */
    PRESCRIPTION(true);

    private final boolean usesPrescriber;

    MedicationCategory(boolean usesPrescriber) {
        this.usesPrescriber = usesPrescriber;
    }

    /** Whether the prescriber is part of dispensing this kind of medicine. */
    public boolean usesPrescriber() {
        return usesPrescriber;
    }

    /**
     * Checks the category's own dispensing rule.
     *
     * @return the reason the sale must be refused, or empty if this category allows it
     */
    public Optional<String> screen(String medicationName, String prescriber) {
        if (usesPrescriber && prescriber == null) {
            return Optional.of("Cannot dispense " + medicationName + " without a prescriber");
        }
        return Optional.empty();
    }
}
