// Akosua's Pharmacy - stock and dispensing tracker
// Original program. Written in a hurry by the intern who has since left.
// It works, but the team is nervous about changing it. That is your job.
//
// Run it as-is first to see what it currently does before you change anything.

import java.util.*;

public class PharmacyTracker {

    public static void main(String[] args) {
        Pharmacy p = new Pharmacy("Akosua's Pharmacy");

        Medication paracetamol = new Medication("Paracetamol 500mg", 12.50, 100, "OTC");
        Medication amoxicillin = new Medication("Amoxicillin 250mg", 30.00, 40, "PRESCRIPTION");
        p.addMedication(paracetamol);
        p.addMedication(amoxicillin);

        Customer kofi = new Customer("Kofi Mensah", "WALK_IN");
        Customer ama = new Customer("Ama Owusu", "REGISTERED");
        ama.phone = "0244 000 000"; // registered patients have contact details on file
        p.addCustomer(kofi);
        p.addCustomer(ama);

        // a normal morning
        p.dispense(kofi, paracetamol, 2, null);
        p.dispense(ama, amoxicillin, 1, "Dr. Boateng"); // prescription-only
        p.restock(paracetamol, 50);

        // ... and the sort of thing that keeps Akosua up at night:
        p.dispense(kofi, amoxicillin, 100, "Dr. Boateng"); // far more than we have in stock

        p.printReport();
    }
}

class Medication {
    public String name;
    public double price;
    public int stock;
    public String category; // "OTC" or "PRESCRIPTION"
    public int id;
    static int counter = 1;

    public Medication(String name, double price, int stock, String category) {
        this.id = counter;
        counter++;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }
}

class Customer {
    public String name;
    public String kind; // "WALK_IN" or "REGISTERED"
    public String phone;
    public int id;
    public List<String> history = new ArrayList<>();
    static int counter = 1;

    public Customer(String name, String kind) {
        this.id = counter;
        counter++;
        this.name = name;
        this.kind = kind;
    }
}

class Transaction {
    public int id;
    public String customerName;
    public String medName;
    public int qty;
    public double total;
    public String type; // "DISPENSE" or "RESTOCK"
    static int counter = 1;

    public Transaction(String customerName, String medName, int qty, double total, String type) {
        this.id = counter;
        counter++;
        this.customerName = customerName;
        this.medName = medName;
        this.qty = qty;
        this.total = total;
        this.type = type;
    }
}

class Pharmacy {
    public String name;
    public List<Medication> meds = new ArrayList<>();
    public List<Customer> customers = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();

    public Pharmacy(String name) {
        this.name = name;
    }

    public void addMedication(Medication m) {
        meds.add(m);
    }

    public void addCustomer(Customer c) {
        customers.add(c);
    }

    // Dispense medicine to a customer. The rules depend on the kind of medicine.
    public void dispense(Customer c, Medication m, int qty, String prescriber) {
        double total = 0;
        if (m.category.equals("OTC")) {
            total = m.price * qty;
            if (c.kind.equals("REGISTERED")) {
                total = total * 0.95; // registered patients get 5% off
            }
            m.stock = m.stock - qty;
            System.out.println("Dispensed " + qty + " x " + m.name + " to " + c.name
                    + " for GHS " + total);
        } else if (m.category.equals("PRESCRIPTION")) {
            if (prescriber == null) {
                System.out.println("Cannot dispense " + m.name + " without a prescriber");
                return;
            }
            total = m.price * qty;
            if (c.kind.equals("REGISTERED")) {
                total = total * 0.95; // registered patients get 5% off
            }
            m.stock = m.stock - qty;
            System.out.println("Dispensed " + qty + " x " + m.name + " to " + c.name
                    + " (prescribed by " + prescriber + ") for GHS " + total);
        }
        c.history.add("Got " + qty + " x " + m.name);
        transactions.add(new Transaction(c.name, m.name, qty, total, "DISPENSE"));
    }

    public void restock(Medication m, int qty) {
        m.stock = m.stock + qty;
        transactions.add(new Transaction("(supplier)", m.name, qty, 0, "RESTOCK"));
        System.out.println("Restocked " + qty + " x " + m.name);
    }

    public List<Transaction> getTransactions() {
        return transactions; // hand the list back so callers can read it
    }

    public void printReport() {
        System.out.println();
        System.out.println("=== " + name + " - end of day ===");
        System.out.println("Stock levels:");
        for (Medication m : meds) {
            System.out.println("  " + m.name + " (" + m.category + "): " + m.stock + " in stock");
        }
        System.out.println("Transactions today: " + transactions.size());
    }
}
