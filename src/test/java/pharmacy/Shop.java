package pharmacy;

import pharmacy.domain.Customer;
import pharmacy.domain.CustomerKind;
import pharmacy.domain.Medication;
import pharmacy.domain.MedicationCategory;
import pharmacy.domain.Outcome;
import pharmacy.domain.Pharmacy;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Test fixture: a fresh pharmacy with the same stock and customers as the demo. */
final class Shop {
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

    static Outcome.Completed completed(Outcome outcome) {
        return assertInstanceOf(Outcome.Completed.class, outcome);
    }

    static Outcome.Refused refused(Outcome outcome) {
        return assertInstanceOf(Outcome.Refused.class, outcome);
    }
}
