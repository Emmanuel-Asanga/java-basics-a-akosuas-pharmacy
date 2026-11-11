package pharmacy;

import pharmacy.domain.Customer;
import pharmacy.domain.CustomerKind;
import pharmacy.domain.Medication;
import pharmacy.domain.MedicationCategory;
import pharmacy.domain.Pharmacy;
import pharmacy.ui.ConsoleView;

public class PharmacyTracker {

    public static void main(String[] args) {
        Pharmacy p = new Pharmacy("Akosua's Pharmacy");
        ConsoleView view = new ConsoleView();

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
}
