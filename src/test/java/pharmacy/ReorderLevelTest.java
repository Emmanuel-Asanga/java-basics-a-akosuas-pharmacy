package pharmacy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pharmacy.domain.Medication;
import pharmacy.domain.MedicationCategory;
import pharmacy.domain.Pharmacy;
import pharmacy.ui.ConsoleView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pharmacy.Shop.completed;
import static pharmacy.Shop.refused;

/** The improvement: a reorder level warns before stock runs out. */
class ReorderLevelTest {

    private Shop s;

    @BeforeEach
    void setUp() {
        s = new Shop();
    }

    private static Medication withLevel(Pharmacy pharmacy, int stock, int level) {
        Medication m = new Medication("Ibuprofen 200mg", 8.0, stock, MedicationCategory.OTC, level);
        pharmacy.addMedication(m);
        return m;
    }

    @Test
    @DisplayName("with no reorder level set there is never an alert")
    void noLevelNoAlert() {
        completed(s.pharmacy.dispense(s.kofi, s.amox, 40, "Dr. Boateng")); // stock now 0
        assertFalse(s.amox.needsReorder(), "level 0 must mean no alert, even at zero stock");
        assertEquals(0, s.pharmacy.getMedicationsToReorder().size());
    }

    @Test
    @DisplayName("an alert starts when stock reaches the level, and not before")
    void alertAtTheLevel() {
        Medication m = withLevel(s.pharmacy, 20, 10);
        assertFalse(m.needsReorder(), "20 in stock, level 10: no alert");
        completed(s.pharmacy.dispense(s.kofi, m, 9, null));   // 11 left
        assertFalse(m.needsReorder(), "11 in stock, level 10: no alert");
        completed(s.pharmacy.dispense(s.kofi, m, 1, null));   // 10 left: at the level
        assertTrue(m.needsReorder(), "10 in stock, level 10: alert");
        completed(s.pharmacy.dispense(s.kofi, m, 10, null));  // 0 left
        assertTrue(m.needsReorder(), "0 in stock, level 10: alert");
        assertEquals(List.of(m), s.pharmacy.getMedicationsToReorder());
    }

    @Test
    @DisplayName("a restock above the level clears the alert")
    void restockClearsAlert() {
        Medication m = withLevel(s.pharmacy, 5, 10);
        assertTrue(m.needsReorder(), "5 in stock, level 10: alert");
        completed(s.pharmacy.restock(m, 6));                  // 11
        assertFalse(m.needsReorder(), "11 in stock, level 10: alert cleared");
    }

    @Test
    @DisplayName("a refused sale does not change the alert")
    void refusalLeavesAlertAlone() {
        Medication m = withLevel(s.pharmacy, 12, 10);
        refused(s.pharmacy.dispense(s.kofi, m, 50, null));    // too many: refused, stock unchanged
        assertEquals(12, m.getStock());
        assertFalse(m.needsReorder(), "a refused sale must not trigger an alert");
    }

    @Test
    @DisplayName("a reorder level cannot be negative")
    void negativeReorderLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> new Medication("Bad", 1.0, 5, MedicationCategory.OTC, -1));
    }

    @Test
    @DisplayName("the report lists what to reorder, and prints nothing extra when nothing is low")
    void reportShowsReorderSection() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleView view = new ConsoleView(new PrintStream(bytes, true, StandardCharsets.UTF_8));

        view.printReport(s.pharmacy);                          // nothing is low
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("Reorder"),
                "no reorder section when nothing is low");

        bytes.reset();
        Medication m = withLevel(s.pharmacy, 12, 10);
        completed(s.pharmacy.dispense(s.kofi, m, 4, null));    // 8 left
        view.printReport(s.pharmacy);
        String report = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(report.contains("Reorder needed:" + System.lineSeparator()
                + "  Ibuprofen 200mg: 8 left (reorder at 10)" + System.lineSeparator()),
                "report should list the low medicine, got:\n" + report);
    }
}
