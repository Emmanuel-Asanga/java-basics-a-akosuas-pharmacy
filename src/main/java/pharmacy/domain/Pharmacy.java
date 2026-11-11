package pharmacy.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The pharmacy: the only place stock is changed and the only writer of the audit trail.
 * Everything it hands out is a read-only view.
 */
public final class Pharmacy {

    private final String name;
    private final List<Medication> medications = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();

    public Pharmacy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addMedication(Medication m) {
        medications.add(m);
    }

    public void addCustomer(Customer c) {
        customers.add(c);
    }

    // Dispense medicine to a customer. The kind of medicine and the kind of customer
    // each decide their own rule; this method no longer compares strings.
    public Outcome dispense(Customer c, Medication m, int qty, String prescriber) {
        Optional<String> refusal = m.getCategory().screen(m.getName(), prescriber);
        if (refusal.isPresent()) {
            return new Outcome.Refused(refusal.get());
        }
        try {
            m.remove(qty);
        } catch (InvalidStockChangeException e) {
            return new Outcome.Refused("Cannot dispense " + qty + " x " + m.getName() + ": " + e.getMessage());
        }
        double total = c.getKind().priceFor(m.getPrice() * qty);
        c.recordDispense(m.getName(), qty);
        // Record the prescriber on the audit trail whenever this kind of medicine uses one.
        String recordedPrescriber = m.getCategory().usesPrescriber() ? prescriber : null;
        Transaction transaction = Transaction.dispense(c.getName(), m.getName(), qty, total, recordedPrescriber);
        transactions.add(transaction);
        return new Outcome.Completed(transaction);
    }

    public Outcome restock(Medication m, int qty) {
        try {
            m.add(qty);
        } catch (InvalidStockChangeException e) {
            return new Outcome.Refused("Cannot restock " + qty + " x " + m.getName() + ": " + e.getMessage());
        }
        Transaction transaction = Transaction.restock(m.getName(), qty);
        transactions.add(transaction);
        return new Outcome.Completed(transaction);
    }

    public List<Medication> getMedications() {
        return Collections.unmodifiableList(medications);
    }

    /** The medicines whose stock has fallen to their reorder level, in the order they were added. */
    public List<Medication> getMedicationsToReorder() {
        return medications.stream().filter(Medication::needsReorder).toList();
    }

    public List<Customer> getCustomers() {
        return Collections.unmodifiableList(customers);
    }

    /** A read-only view of the audit trail: callers can read it but not alter it. */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }
}
