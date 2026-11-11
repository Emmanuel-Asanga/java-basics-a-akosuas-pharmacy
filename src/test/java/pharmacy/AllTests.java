package pharmacy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pharmacy.domain.Customer;
import pharmacy.domain.CustomerKind;
import pharmacy.domain.Medication;
import pharmacy.domain.MedicationCategory;
import pharmacy.domain.Outcome;
import pharmacy.domain.Pharmacy;
import pharmacy.domain.Transaction;
import pharmacy.ui.ConsoleView;

/**
 * Dependency-free tests (no JUnit needed): run with scripts/test.sh.
 * Each test is named for the behaviour it protects.
 */
public final class AllTests {

    private static int passed = 0;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("The stock rule: a count can never become invalid");
        test("an over-dispense is refused and changes nothing", AllTests::overDispenseRefused);
        test("the demo day now ends with 39 Amoxicillin, not -61", AllTests::demoDayEndsAtThirtyNine);
        test("buying exactly the remaining stock is allowed and leaves 0", AllTests::exactStockAllowed);
        test("after reaching 0, one more is refused", AllTests::nothingLeftIsRefused);
        test("zero and negative dispense quantities are refused", AllTests::badDispenseQuantities);
        test("zero and negative restock quantities are refused", AllTests::badRestockQuantities);
        test("a restock that would overflow the count is refused", AllTests::restockOverflowRefused);
        test("a medicine cannot be created with negative stock", AllTests::negativeOpeningStock);
        test("every completed stock change leaves a transaction; refusals leave none",
                AllTests::auditTrailMatchesStock);

        System.out.println("Behaviour kept from the original");
        test("prescription medicine without a prescriber is refused", AllTests::prescriptionNeedsPrescriber);
        test("a blank prescriber is accepted, as in the original", AllTests::blankPrescriberAccepted);
        test("OTC medicine ignores a prescriber that is passed in", AllTests::otcIgnoresPrescriber);
        test("registered customers pay 5% less; walk-ins pay full price", AllTests::discountAmounts);
        test("walk-in customers get a history entry too, as in the original", AllTests::walkInHistoryRecorded);
        test("the console view prints the refusal reason", AllTests::consoleShowsRefusal);

        System.out.println("The improvement: the prescriber is kept on the audit trail");
        test("a prescribed dispense records who prescribed it", AllTests::prescriberIsOnTheAuditTrail);

        System.out.println("The improvement: reorder level warns before stock runs out");
        test("with no reorder level set there is never an alert", AllTests::noLevelNoAlert);
        test("an alert starts when stock reaches the level, and not before", AllTests::alertAtTheLevel);
        test("a restock above the level clears the alert", AllTests::restockClearsAlert);
        test("a refused sale does not change the alert", AllTests::refusalLeavesAlertAlone);
        test("a reorder level cannot be negative", AllTests::negativeReorderLevel);
        test("the report lists what to reorder, and prints nothing extra when nothing is low",
                AllTests::reportShowsReorderSection);

        System.out.println("Encapsulation: nothing outside can corrupt the records");
        test("the audit trail and the lists are read-only views", AllTests::readOnlyViews);
        test("stock can only change through the pharmacy (reflection guard)", AllTests::stockHasNoPublicSetter);
        test("transactions are immutable records", AllTests::transactionsAreRecords);

        System.out.println();
        System.out.println(passed + " passed, " + failures.size() + " failed");
        if (!failures.isEmpty()) {
            failures.forEach(name -> System.out.println("  FAILED: " + name));
            System.exit(1);
        }
    }

    // ---- the stock rule --------------------------------------------------------------------

    private static void overDispenseRefused() {
        Shop s = new Shop();
        Outcome outcome = s.pharmacy.dispense(s.kofi, s.amox, 100, "Dr. Boateng");
        expectEquals("Cannot dispense 100 x Amoxicillin 250mg: only 40 in stock", refused(outcome).reason());
        expectEquals(40, s.amox.getStock());
        expectEquals(0, s.pharmacy.getTransactions().size());
        expectEquals(0, s.kofi.getHistory().size());
    }

    private static void demoDayEndsAtThirtyNine() {
        Shop s = new Shop();
        s.pharmacy.dispense(s.kofi, s.para, 2, null);
        s.pharmacy.dispense(s.ama, s.amox, 1, "Dr. Boateng");
        s.pharmacy.restock(s.para, 50);
        Outcome outcome = s.pharmacy.dispense(s.kofi, s.amox, 100, "Dr. Boateng");
        expectEquals("Cannot dispense 100 x Amoxicillin 250mg: only 39 in stock", refused(outcome).reason());
        expectEquals(148, s.para.getStock());
        expectEquals(39, s.amox.getStock());
        expectEquals(3, s.pharmacy.getTransactions().size());
    }

    private static void exactStockAllowed() {
        Shop s = new Shop();
        completed(s.pharmacy.dispense(s.kofi, s.amox, 40, "Dr. Boateng"));
        expectEquals(0, s.amox.getStock());
    }

    private static void nothingLeftIsRefused() {
        Shop s = new Shop();
        completed(s.pharmacy.dispense(s.kofi, s.amox, 40, "Dr. Boateng"));
        Outcome outcome = s.pharmacy.dispense(s.kofi, s.amox, 1, "Dr. Boateng");
        expectEquals("Cannot dispense 1 x Amoxicillin 250mg: only 0 in stock", refused(outcome).reason());
        expectEquals(0, s.amox.getStock());
    }

    private static void badDispenseQuantities() {
        Shop s = new Shop();
        for (int qty : new int[] {0, -1, -5}) {
            Outcome outcome = s.pharmacy.dispense(s.kofi, s.para, qty, null);
            expectEquals("Cannot dispense " + qty + " x Paracetamol 500mg: quantity must be at least 1",
                    refused(outcome).reason());
        }
        expectEquals(100, s.para.getStock());
        expectEquals(0, s.pharmacy.getTransactions().size());
    }

    private static void badRestockQuantities() {
        Shop s = new Shop();
        for (int qty : new int[] {0, -1, -200}) {
            Outcome outcome = s.pharmacy.restock(s.para, qty);
            expectEquals("Cannot restock " + qty + " x Paracetamol 500mg: quantity must be at least 1",
                    refused(outcome).reason());
        }
        expectEquals(100, s.para.getStock());
        expectEquals(0, s.pharmacy.getTransactions().size());
    }

    private static void restockOverflowRefused() {
        Shop s = new Shop();
        Outcome outcome = s.pharmacy.restock(s.para, Integer.MAX_VALUE);
        expectEquals("Cannot restock " + Integer.MAX_VALUE + " x Paracetamol 500mg: stock cannot exceed "
                + Integer.MAX_VALUE, refused(outcome).reason());
        expectEquals(100, s.para.getStock());
    }

    private static void negativeOpeningStock() {
        try {
            new Medication("Bad", 1.0, -1, MedicationCategory.OTC);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    private static void auditTrailMatchesStock() {
        Shop s = new Shop();
        completed(s.pharmacy.dispense(s.kofi, s.para, 2, null));          // +1 transaction
        refused(s.pharmacy.dispense(s.kofi, s.amox, 1, null));            // refused: none
        refused(s.pharmacy.dispense(s.kofi, s.para, 1000, null));         // refused: none
        completed(s.pharmacy.restock(s.para, 10));                        // +1 transaction
        refused(s.pharmacy.restock(s.para, 0));                           // refused: none
        expectEquals(2, s.pharmacy.getTransactions().size());
        expectEquals(108, s.para.getStock());
    }

    // ---- behaviour kept from the original --------------------------------------------------

    private static void prescriptionNeedsPrescriber() {
        Shop s = new Shop();
        Outcome outcome = s.pharmacy.dispense(s.kofi, s.amox, 1, null);
        expectEquals("Cannot dispense Amoxicillin 250mg without a prescriber", refused(outcome).reason());
        expectEquals(40, s.amox.getStock());
        expectEquals(0, s.pharmacy.getTransactions().size());
        expectEquals(0, s.kofi.getHistory().size());
    }

    private static void blankPrescriberAccepted() {
        // Characterisation test: the original only rejected null. Pinned so a later change is deliberate.
        Shop s = new Shop();
        Outcome.Completed done = completed(s.pharmacy.dispense(s.kofi, s.amox, 1, ""));
        expectEquals("", done.transaction().prescriber());
        expectEquals(39, s.amox.getStock());
    }

    private static void otcIgnoresPrescriber() {
        Shop s = new Shop();
        Outcome.Completed done = completed(s.pharmacy.dispense(s.kofi, s.para, 1, "Dr. Boateng"));
        expectEquals(null, done.transaction().prescriber());
    }

    private static void prescriberIsOnTheAuditTrail() {
        Shop s = new Shop();
        s.pharmacy.dispense(s.ama, s.amox, 1, "Dr. Boateng");
        s.pharmacy.dispense(s.kofi, s.para, 1, null);
        s.pharmacy.restock(s.para, 5);
        List<Transaction> trail = s.pharmacy.getTransactions();
        expectEquals("Dr. Boateng", trail.get(0).prescriber());
        expectEquals(null, trail.get(1).prescriber());
        expectEquals(null, trail.get(2).prescriber());
    }

    private static void discountAmounts() {
        Shop s = new Shop();
        expectEquals(25.0, completed(s.pharmacy.dispense(s.kofi, s.para, 2, null)).transaction().total());
        expectEquals(28.5, completed(s.pharmacy.dispense(s.ama, s.amox, 1, "Dr. Boateng")).transaction().total());
        expectEquals(35.625, completed(s.pharmacy.dispense(s.ama, s.para, 3, null)).transaction().total());
    }

    private static void walkInHistoryRecorded() {
        Shop s = new Shop();
        completed(s.pharmacy.dispense(s.kofi, s.para, 2, null));
        expectEquals(List.of("Got 2 x Paracetamol 500mg"), s.kofi.getHistory());
    }

    private static void consoleShowsRefusal() {
        Shop s = new Shop();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleView view = new ConsoleView(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        view.show(s.pharmacy.dispense(s.kofi, s.amox, 100, "Dr. Boateng"));
        expectEquals("Cannot dispense 100 x Amoxicillin 250mg: only 40 in stock" + System.lineSeparator(),
                bytes.toString(StandardCharsets.UTF_8));
    }

    // ---- the improvement: reorder level -----------------------------------------------------

    private static Medication withLevel(Pharmacy pharmacy, int stock, int level) {
        Medication m = new Medication("Ibuprofen 200mg", 8.0, stock, MedicationCategory.OTC, level);
        pharmacy.addMedication(m);
        return m;
    }

    private static void noLevelNoAlert() {
        Shop s = new Shop();
        completed(s.pharmacy.dispense(s.kofi, s.amox, 40, "Dr. Boateng")); // stock now 0
        expect(!s.amox.needsReorder(), "level 0 must mean no alert, even at zero stock");
        expectEquals(0, s.pharmacy.getMedicationsToReorder().size());
    }

    private static void alertAtTheLevel() {
        Shop s = new Shop();
        Medication m = withLevel(s.pharmacy, 20, 10);
        expect(!m.needsReorder(), "20 in stock, level 10: no alert");
        completed(s.pharmacy.dispense(s.kofi, m, 9, null));   // 11 left
        expect(!m.needsReorder(), "11 in stock, level 10: no alert");
        completed(s.pharmacy.dispense(s.kofi, m, 1, null));   // 10 left: at the level
        expect(m.needsReorder(), "10 in stock, level 10: alert");
        completed(s.pharmacy.dispense(s.kofi, m, 10, null));  // 0 left
        expect(m.needsReorder(), "0 in stock, level 10: alert");
        expectEquals(List.of(m), s.pharmacy.getMedicationsToReorder());
    }

    private static void restockClearsAlert() {
        Shop s = new Shop();
        Medication m = withLevel(s.pharmacy, 5, 10);
        expect(m.needsReorder(), "5 in stock, level 10: alert");
        completed(s.pharmacy.restock(m, 6));                  // 11
        expect(!m.needsReorder(), "11 in stock, level 10: alert cleared");
    }

    private static void refusalLeavesAlertAlone() {
        Shop s = new Shop();
        Medication m = withLevel(s.pharmacy, 12, 10);
        refused(s.pharmacy.dispense(s.kofi, m, 50, null));    // too many: refused, stock unchanged
        expectEquals(12, m.getStock());
        expect(!m.needsReorder(), "a refused sale must not trigger an alert");
    }

    private static void negativeReorderLevel() {
        try {
            new Medication("Bad", 1.0, 5, MedicationCategory.OTC, -1);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    private static void reportShowsReorderSection() {
        Shop s = new Shop();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleView view = new ConsoleView(new PrintStream(bytes, true, StandardCharsets.UTF_8));

        view.printReport(s.pharmacy);                          // nothing is low
        expect(!bytes.toString(StandardCharsets.UTF_8).contains("Reorder"),
                "no reorder section when nothing is low");

        bytes.reset();
        Medication m = withLevel(s.pharmacy, 12, 10);
        completed(s.pharmacy.dispense(s.kofi, m, 4, null));    // 8 left
        view.printReport(s.pharmacy);
        String report = bytes.toString(StandardCharsets.UTF_8);
        expect(report.contains("Reorder needed:" + System.lineSeparator()
                + "  Ibuprofen 200mg: 8 left (reorder at 10)" + System.lineSeparator()),
                "report should list the low medicine, got:\n" + report);
    }

    // ---- encapsulation ---------------------------------------------------------------------

    private static void readOnlyViews() {
        Shop s = new Shop();
        completed(s.pharmacy.dispense(s.kofi, s.para, 1, null));
        expectThrows(UnsupportedOperationException.class, () -> s.pharmacy.getTransactions().clear());
        expectThrows(UnsupportedOperationException.class, () -> s.pharmacy.getMedications().clear());
        expectThrows(UnsupportedOperationException.class, () -> s.pharmacy.getCustomers().clear());
        expectThrows(UnsupportedOperationException.class, () -> s.kofi.getHistory().clear());
        expectEquals(1, s.pharmacy.getTransactions().size());
    }

    private static void stockHasNoPublicSetter() {
        for (Class<?> type : new Class<?>[] {Medication.class, Customer.class, Pharmacy.class}) {
            for (Field field : type.getDeclaredFields()) {
                expect(Modifier.isPrivate(field.getModifiers()),
                        type.getSimpleName() + "." + field.getName() + " must be private");
            }
        }
        for (Method method : Medication.class.getMethods()) {
            String name = method.getName();
            expect(!name.equals("add") && !name.equals("remove") && !name.startsWith("setStock"),
                    "Medication must not expose a public way to change stock: " + name);
        }
    }

    private static void transactionsAreRecords() {
        expect(Transaction.class.isRecord(), "Transaction must stay an immutable record");
    }

    // ---- tiny test kit ---------------------------------------------------------------------

    /** A fresh pharmacy with the same stock and customers as the demo. */
    private static final class Shop {
        final Pharmacy pharmacy = new Pharmacy("Test Pharmacy");
        final Medication para = new Medication("Paracetamol 500mg", 12.50, 100, MedicationCategory.OTC);
        final Medication amox = new Medication("Amoxicillin 250mg", 30.00, 40, MedicationCategory.PRESCRIPTION);
        final Customer kofi = new Customer("Kofi Mensah", CustomerKind.WALK_IN);
        final Customer ama = new Customer("Ama Owusu", CustomerKind.REGISTERED);

        Shop() {
            pharmacy.addMedication(para);
            pharmacy.addMedication(amox);
            pharmacy.addCustomer(kofi);
            pharmacy.addCustomer(ama);
        }
    }

    private interface Check {
        void run() throws Exception;
    }

    private static void test(String name, Check check) {
        try {
            check.run();
            passed++;
            System.out.println("  ok    " + name);
        } catch (Throwable problem) {
            failures.add(name);
            System.out.println("  FAIL  " + name + "  ->  " + problem);
        }
    }

    private static Outcome.Completed completed(Outcome outcome) {
        if (outcome instanceof Outcome.Completed c) {
            return c;
        }
        throw new AssertionError("expected Completed but was " + outcome);
    }

    private static Outcome.Refused refused(Outcome outcome) {
        if (outcome instanceof Outcome.Refused r) {
            return r;
        }
        throw new AssertionError("expected Refused but was " + outcome);
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void expectThrows(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) {
                return;
            }
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + thrown);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }
}
