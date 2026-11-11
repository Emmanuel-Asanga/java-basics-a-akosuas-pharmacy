package pharmacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pharmacy.domain.Medication;
import pharmacy.domain.MedicationCategory;
import pharmacy.domain.Outcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pharmacy.Shop.completed;
import static pharmacy.Shop.refused;

/** The CONTROLLED category proves the category rule is extensible without touching Pharmacy. */
class ControlledMedicationTest {

    private Shop s;
    private Medication morphine;

    @BeforeEach
    void setUp() {
        s = new Shop();
        morphine = new Medication("Morphine 10mg", 50.0, 20, MedicationCategory.CONTROLLED);
        s.pharmacy.addMedication(morphine);
    }

    @Test
    @DisplayName("a controlled medicine is refused with no prescriber")
    void nullPrescriberRefused() {
        Outcome outcome = s.pharmacy.dispense(s.kofi, morphine, 1, null);
        assertEquals("Cannot dispense Morphine 10mg without a named prescriber", refused(outcome).reason());
        assertEquals(20, morphine.getStock());
        assertEquals(0, s.pharmacy.getTransactions().size());
    }

    @Test
    @DisplayName("a controlled medicine is refused with a blank prescriber (unlike ordinary prescription medicine)")
    void blankPrescriberRefused() {
        Outcome outcome = s.pharmacy.dispense(s.kofi, morphine, 1, "   ");
        assertEquals("Cannot dispense Morphine 10mg without a named prescriber", refused(outcome).reason());
        assertEquals(20, morphine.getStock());
    }

    @Test
    @DisplayName("a controlled medicine with a named prescriber is dispensed and the prescriber is recorded")
    void namedPrescriberAccepted() {
        Outcome.Completed done = completed(s.pharmacy.dispense(s.kofi, morphine, 1, "Dr. Boateng"));
        assertEquals("Dr. Boateng", done.transaction().prescriber());
        assertEquals(19, morphine.getStock());
    }
}
