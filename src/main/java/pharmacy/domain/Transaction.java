package pharmacy.domain;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * One line of the audit trail. A record is immutable: once written, a transaction cannot be edited.
 *
 * @param prescriber who prescribed it, for a dispense of a medicine that uses a prescriber;
 *                   {@code null} for everything else (OTC sales and restocks)
 */
public record Transaction(int id, String customerName, String medName, int qty, double total,
                          TransactionType type, String prescriber) {

    /** Who a restock is "from". */
    public static final String SUPPLIER = "(supplier)";

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    public static Transaction dispense(String customerName, String medName, int qty, double total,
                                       String prescriber) {
        return new Transaction(NEXT_ID.getAndIncrement(), customerName, medName, qty, total,
                TransactionType.DISPENSE, prescriber);
    }

    public static Transaction restock(String medName, int qty) {
        return new Transaction(NEXT_ID.getAndIncrement(), SUPPLIER, medName, qty, 0,
                TransactionType.RESTOCK, null);
    }
}
