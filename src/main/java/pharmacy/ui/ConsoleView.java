package pharmacy.ui;

import java.io.PrintStream;
import java.util.List;

import pharmacy.domain.Medication;
import pharmacy.domain.Outcome;
import pharmacy.domain.Pharmacy;
import pharmacy.domain.Transaction;

/**
 * The only class that prints. Keeping output here means the pharmacy rules can be run and tested
 * without a console, and the wording of a message can change without touching a rule.
 */
public final class ConsoleView {

    private final PrintStream out;

    public ConsoleView() {
        this(System.out);
    }

    public ConsoleView(PrintStream out) {
        this.out = out;
    }

    /** Prints the result of a dispense or restock. */
    public void show(Outcome outcome) {
        switch (outcome) {
            case Outcome.Refused refused -> out.println(refused.reason());
            case Outcome.Completed completed -> out.println(describe(completed));
        }
    }

    private static String describe(Outcome.Completed completed) {
        Transaction t = completed.transaction();
        return switch (t.type()) {
            case DISPENSE -> {
                String prescribedBy = t.prescriber() == null
                        ? "" : " (prescribed by " + t.prescriber() + ")";
                yield "Dispensed " + t.qty() + " x " + t.medName() + " to " + t.customerName()
                        + prescribedBy + " for GHS " + t.total();
            }
            case RESTOCK -> "Restocked " + t.qty() + " x " + t.medName();
        };
    }

    public void printReport(Pharmacy pharmacy) {
        out.println();
        out.println("=== " + pharmacy.getName() + " - end of day ===");
        out.println("Stock levels:");
        for (Medication m : pharmacy.getMedications()) {
            out.println("  " + m.getName() + " (" + m.getCategory() + "): " + m.getStock() + " in stock");
        }
        out.println("Transactions today: " + pharmacy.getTransactions().size());
        List<Medication> toReorder = pharmacy.getMedicationsToReorder();
        if (!toReorder.isEmpty()) {
            out.println("Reorder needed:");
            for (Medication m : toReorder) {
                out.println("  " + m.getName() + ": " + m.getStock() + " left (reorder at "
                        + m.getReorderLevel() + ")");
            }
        }
    }
}
