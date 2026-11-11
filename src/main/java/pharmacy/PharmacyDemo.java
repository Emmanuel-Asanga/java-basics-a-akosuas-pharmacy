package pharmacy;

import pharmacy.domain.Customer;
import pharmacy.domain.CustomerKind;
import pharmacy.domain.Medication;
import pharmacy.domain.MedicationCategory;
import pharmacy.domain.Pharmacy;
import pharmacy.ui.ConsoleView;

/** Sample scenarios used to demonstrate the pharmacy. Kept out of the entry point on purpose. */
public final class PharmacyDemo {

    private PharmacyDemo() {
    }

    /** The original demo day, unchanged, so its output can be compared with the legacy program. */
    public static void run(ConsoleView view) {
        Pharmacy p = new Pharmacy("Akosua's Pharmacy");

        Medication paracetamol = new Medication("Paracetamol 500mg", 12.50, 100, MedicationCategory.OTC);
        Medication amoxicillin = new Medication("Amoxicillin 250mg", 30.00, 40, MedicationCategory.PRESCRIPTION);
        p.addMedication(paracetamol);
        p.addMedication(amoxicillin);

        Customer kofi = new Customer("Kofi Mensah", CustomerKind.WALK_IN);
        Customer ama = new Customer("Ama Owusu", CustomerKind.REGISTERED);
        ama.setPhone("0244 000 000"); // registered patients have contact details on file
        p.addCustomer(kofi);
        p.addCustomer(ama);

        // a normal morning
        view.show(p.dispense(kofi, paracetamol, 2, null));
        view.show(p.dispense(ama, amoxicillin, 1, "Dr. Boateng")); // prescription-only
        view.show(p.restock(paracetamol, 50));

        // ... and the sort of thing that keeps Akosua up at night:
        view.show(p.dispense(kofi, amoxicillin, 100, "Dr. Boateng")); // far more than we have in stock

        view.printReport(p);
    }

    /** A second day where stock falls to the reorder levels, so the "Reorder needed" report shows. */
    public static void runReorderDemo(ConsoleView view) {
        Pharmacy p = new Pharmacy("Akosua's Pharmacy (reorder demo)");

        Medication paracetamol = new Medication("Paracetamol 500mg", 12.50, 100, MedicationCategory.OTC, 20);
        Medication amoxicillin = new Medication("Amoxicillin 250mg", 30.00, 40, MedicationCategory.PRESCRIPTION, 10);
        p.addMedication(paracetamol);
        p.addMedication(amoxicillin);

        Customer kofi = new Customer("Kofi Mensah", CustomerKind.WALK_IN);
        p.addCustomer(kofi);

        view.show(p.dispense(kofi, paracetamol, 85, null));                // 15 left, level 20
        view.show(p.dispense(kofi, amoxicillin, 36, "Dr. Boateng"));       // 4 left, level 10

        view.printReport(p);
    }
}
