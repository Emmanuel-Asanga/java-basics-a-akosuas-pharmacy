package pharmacy.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A walk-in or registered customer. The history can be read but not edited from outside:
 * entries are only added by {@link Pharmacy} when a dispense really happens.
 */
public final class Customer {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;
    private final String name;
    private final CustomerKind kind;
    private String phone;
    private final List<String> history = new ArrayList<>();

    public Customer(String name, CustomerKind kind) {
        this.id = NEXT_ID.getAndIncrement();
        this.name = name;
        this.kind = kind;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CustomerKind getKind() {
        return kind;
    }

    public Optional<String> getPhone() {
        return Optional.ofNullable(phone);
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** A read-only view of what this customer has been given. */
    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    void recordDispense(String medicationName, int qty) {
        history.add("Got " + qty + " x " + medicationName);
    }
}
