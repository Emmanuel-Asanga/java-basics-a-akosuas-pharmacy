package pharmacy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pharmacy.domain.Outcome;
import pharmacy.domain.Transaction;
import pharmacy.ui.ConsoleView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static pharmacy.Shop.completed;
import static pharmacy.Shop.refused;

/** Behaviour deliberately kept from the original program, plus the prescriber audit record. */
class OriginalBehaviourTest {

    private Shop s;

    @BeforeEach
    void setUp() {
        s = new Shop();
    }

    @Test
    @DisplayName("prescription medicine without a prescriber is refused")
    void prescriptionNeedsPrescriber() {
        Outcome outcome = s.pharmacy.dispense(s.kofi, s.amox, 1, null);
        assertEquals("Cannot dispense Amoxicillin 250mg without a prescriber", refused(outcome).reason());
        assertEquals(40, s.amox.getStock());
        assertEquals(0, s.pharmacy.getTransactions().size());
        assertEquals(0, s.kofi.getHistory().size());
    }

    @Test
    @DisplayName("a blank prescriber is accepted for ordinary prescription medicine, as in the original")
    void blankPrescriberAccepted() {
        // Characterisation test: the original only rejected null. Pinned so a later change is deliberate.
        Outcome.Completed done = completed(s.pharmacy.dispense(s.kofi, s.amox, 1, ""));
        assertEquals("", done.transaction().prescriber());
        assertEquals(39, s.amox.getStock());
    }

    @Test
    @DisplayName("OTC medicine ignores a prescriber that is passed in")
    void otcIgnoresPrescriber() {
        Outcome.Completed done = completed(s.pharmacy.dispense(s.kofi, s.para, 1, "Dr. Boateng"));
        assertNull(done.transaction().prescriber());
    }

    @Test
    @DisplayName("a prescribed dispense records who prescribed it")
    void prescriberIsOnTheAuditTrail() {
        s.pharmacy.dispense(s.ama, s.amox, 1, "Dr. Boateng");
        s.pharmacy.dispense(s.kofi, s.para, 1, null);
        s.pharmacy.restock(s.para, 5);
        List<Transaction> trail = s.pharmacy.getTransactions();
        assertEquals("Dr. Boateng", trail.get(0).prescriber());
        assertNull(trail.get(1).prescriber());
        assertNull(trail.get(2).prescriber());
    }

    @Test
    @DisplayName("registered customers pay 5% less; walk-ins pay full price")
    void discountAmounts() {
        assertEquals(25.0, completed(s.pharmacy.dispense(s.kofi, s.para, 2, null)).transaction().total());
        assertEquals(28.5, completed(s.pharmacy.dispense(s.ama, s.amox, 1, "Dr. Boateng")).transaction().total());
        assertEquals(35.625, completed(s.pharmacy.dispense(s.ama, s.para, 3, null)).transaction().total());
    }

    @Test
    @DisplayName("walk-in customers get a history entry too, as in the original")
    void walkInHistoryRecorded() {
        completed(s.pharmacy.dispense(s.kofi, s.para, 2, null));
        assertEquals(List.of("Got 2 x Paracetamol 500mg"), s.kofi.getHistory());
    }

    @Test
    @DisplayName("the console view prints the refusal reason")
    void consoleShowsRefusal() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleView view = new ConsoleView(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        view.show(s.pharmacy.dispense(s.kofi, s.amox, 100, "Dr. Boateng"));
        assertEquals("Cannot dispense 100 x Amoxicillin 250mg: only 40 in stock" + System.lineSeparator(),
                bytes.toString(StandardCharsets.UTF_8));
    }
}
