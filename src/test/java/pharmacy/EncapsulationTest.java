package pharmacy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pharmacy.domain.Customer;
import pharmacy.domain.Medication;
import pharmacy.domain.Pharmacy;
import pharmacy.domain.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pharmacy.Shop.completed;

/** Encapsulation: nothing outside can corrupt the records. */
class EncapsulationTest {

    private Shop s;

    @BeforeEach
    void setUp() {
        s = new Shop();
    }

    @Test
    @DisplayName("the audit trail and the lists are read-only views")
    void readOnlyViews() {
        completed(s.pharmacy.dispense(s.kofi, s.para, 1, null));
        assertThrows(UnsupportedOperationException.class, () -> s.pharmacy.getTransactions().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.pharmacy.getMedications().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.pharmacy.getCustomers().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.kofi.getHistory().clear());
        assertEquals(1, s.pharmacy.getTransactions().size());
    }

    @Test
    @DisplayName("stock can only change through the pharmacy (reflection guard)")
    void stockHasNoPublicSetter() {
        for (Class<?> type : new Class<?>[] {Medication.class, Customer.class, Pharmacy.class}) {
            for (Field field : type.getDeclaredFields()) {
                assertTrue(Modifier.isPrivate(field.getModifiers()),
                        type.getSimpleName() + "." + field.getName() + " must be private");
            }
        }
        for (Method method : Medication.class.getMethods()) {
            String name = method.getName();
            assertFalse(name.equals("add") || name.equals("remove") || name.startsWith("setStock"),
                    "Medication must not expose a public way to change stock: " + name);
        }
    }

    @Test
    @DisplayName("transactions are immutable records")
    void transactionsAreRecords() {
        assertTrue(Transaction.class.isRecord(), "Transaction must stay an immutable record");
    }
}
