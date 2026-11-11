package pharmacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pharmacy.domain.Medication;
import pharmacy.domain.MedicationCategory;
import pharmacy.domain.Outcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pharmacy.Shop.completed;
import static pharmacy.Shop.refused;

/** The stock rule: a count can never become invalid. */
class StockRuleTest {

    private Shop s;

    @BeforeEach
    void setUp() {
        s = new Shop();
    }

    @Test
    @DisplayName("an over-dispense is refused and changes nothing")
    void overDispenseRefused() {
        Outcome outcome = s.pharmacy.dispense(s.kofi, s.amox, 100, "Dr. Boateng");
        assertEquals("Cannot dispense 100 x Amoxicillin 250mg: only 40 in stock", refused(outcome).reason());
        assertEquals(40, s.amox.getStock());
        assertEquals(0, s.pharmacy.getTransactions().size());
        assertEquals(0, s.kofi.getHistory().size());
    }

    @Test
    @DisplayName("the demo day now ends with 39 Amoxicillin, not -61")
    void demoDayEndsAtThirtyNine() {
        s.pharmacy.dispense(s.kofi, s.para, 2, null);
        s.pharmacy.dispense(s.ama, s.amox, 1, "Dr. Boateng");
        s.pharmacy.restock(s.para, 50);
        Outcome outcome = s.pharmacy.dispense(s.kofi, s.amox, 100, "Dr. Boateng");
        assertEquals("Cannot dispense 100 x Amoxicillin 250mg: only 39 in stock", refused(outcome).reason());
        assertEquals(148, s.para.getStock());
        assertEquals(39, s.amox.getStock());
        assertEquals(3, s.pharmacy.getTransactions().size());
    }

    @Test
    @DisplayName("buying exactly the remaining stock is allowed and leaves 0")
    void exactStockAllowed() {
        completed(s.pharmacy.dispense(s.kofi, s.amox, 40, "Dr. Boateng"));
        assertEquals(0, s.amox.getStock());
    }

    @Test
    @DisplayName("after reaching 0, one more is refused")
    void nothingLeftIsRefused() {
        completed(s.pharmacy.dispense(s.kofi, s.amox, 40, "Dr. Boateng"));
        Outcome outcome = s.pharmacy.dispense(s.kofi, s.amox, 1, "Dr. Boateng");
        assertEquals("Cannot dispense 1 x Amoxicillin 250mg: only 0 in stock", refused(outcome).reason());
        assertEquals(0, s.amox.getStock());
    }

    @Test
    @DisplayName("zero and negative dispense quantities are refused")
    void badDispenseQuantities() {
        for (int qty : new int[] {0, -1, -5}) {
            Outcome outcome = s.pharmacy.dispense(s.kofi, s.para, qty, null);
            assertEquals("Cannot dispense " + qty + " x Paracetamol 500mg: quantity must be at least 1",
                    refused(outcome).reason());
        }
        assertEquals(100, s.para.getStock());
        assertEquals(0, s.pharmacy.getTransactions().size());
    }

    @Test
    @DisplayName("zero and negative restock quantities are refused")
    void badRestockQuantities() {
        for (int qty : new int[] {0, -1, -200}) {
            Outcome outcome = s.pharmacy.restock(s.para, qty);
            assertEquals("Cannot restock " + qty + " x Paracetamol 500mg: quantity must be at least 1",
                    refused(outcome).reason());
        }
        assertEquals(100, s.para.getStock());
        assertEquals(0, s.pharmacy.getTransactions().size());
    }

    @Test
    @DisplayName("a restock that would overflow the count is refused and records nothing")
    void restockOverflowRefused() {
        Outcome outcome = s.pharmacy.restock(s.para, Integer.MAX_VALUE);
        assertEquals("Cannot restock " + Integer.MAX_VALUE + " x Paracetamol 500mg: stock cannot exceed "
                + Integer.MAX_VALUE, refused(outcome).reason());
        assertEquals(100, s.para.getStock());
        assertEquals(0, s.pharmacy.getTransactions().size());
    }

    @Test
    @DisplayName("a medicine cannot be created with negative stock")
    void negativeOpeningStock() {
        assertThrows(IllegalArgumentException.class,
                () -> new Medication("Bad", 1.0, -1, MedicationCategory.OTC));
    }

    @Test
    @DisplayName("every completed stock change leaves a transaction; refusals leave none")
    void auditTrailMatchesStock() {
        completed(s.pharmacy.dispense(s.kofi, s.para, 2, null));   // +1 transaction
        refused(s.pharmacy.dispense(s.kofi, s.amox, 1, null));     // refused: none
        refused(s.pharmacy.dispense(s.kofi, s.para, 1000, null));  // refused: none
        completed(s.pharmacy.restock(s.para, 10));                 // +1 transaction
        refused(s.pharmacy.restock(s.para, 0));                    // refused: none
        assertEquals(2, s.pharmacy.getTransactions().size());
        assertEquals(108, s.para.getStock());
    }

    @Test
    @DisplayName("every refusal path carries a non-blank reason")
    void refusalReasonsAreNeverBlank() {
        Outcome[] refusals = {
            s.pharmacy.dispense(s.kofi, s.amox, 1, null),                  // missing prescriber
            s.pharmacy.dispense(s.kofi, s.amox, 1000, "Dr. Boateng"),      // not enough stock
            s.pharmacy.dispense(s.kofi, s.para, 0, null),                  // bad quantity
            s.pharmacy.restock(s.para, -5),                                // bad restock quantity
            s.pharmacy.restock(s.para, Integer.MAX_VALUE),                 // overflow
        };
        for (Outcome outcome : refusals) {
            assertFalse(refused(outcome).reason().isBlank(), "refusal had no reason: " + outcome);
        }
    }
}
