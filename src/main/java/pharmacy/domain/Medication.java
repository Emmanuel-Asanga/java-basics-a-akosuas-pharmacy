package pharmacy.domain;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One medicine the pharmacy stocks.
 *
 * <p>Every field is private. Stock is read through {@link #getStock()} and can only be changed
 * by {@code add} and {@code remove}, which are package-private: only {@link Pharmacy} can call
 * them, so every change to a stock count also produces a {@link Transaction}.
 */
public final class Medication {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;
    private final String name;
    private final double price;
    private final MedicationCategory category;
    private final int reorderLevel;
    private int stock;

    /** A medicine with no reorder alert (reorder level 0). */
    public Medication(String name, double price, int stock, MedicationCategory category) {
        this(name, price, stock, category, 0);
    }

    /**
     * @param reorderLevel the stock count at or below which staff should reorder; 0 means no alert
     */
    public Medication(String name, double price, int stock, MedicationCategory category, int reorderLevel) {
        if (stock < 0) {
            throw new IllegalArgumentException("opening stock cannot be negative: " + stock);
        }
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("reorder level cannot be negative: " + reorderLevel);
        }
        this.id = NEXT_ID.getAndIncrement();
        this.name = Objects.requireNonNull(name, "name");
        this.price = price;
        this.stock = stock;
        this.category = Objects.requireNonNull(category, "category");
        this.reorderLevel = reorderLevel;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public MedicationCategory getCategory() {
        return category;
    }

    public int getStock() {
        return stock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    /**
     * True when an alert is set (level above 0) and stock has fallen to that level or below.
     * With the level at 0 there is no alert, so existing medicines behave exactly as before.
     */
    public boolean needsReorder() {
        return reorderLevel > 0 && stock <= reorderLevel;
    }

    /**
     * Adds stock. This and {@link #remove} are the only code that changes {@code stock}, and both
     * check the rule first, so the count can never become invalid.
     *
     * @throws InvalidStockChangeException if {@code qty} is not positive or the count would overflow
     */
    void add(int qty) {
        requirePositive(qty);
        try {
            stock = Math.addExact(stock, qty);
        } catch (ArithmeticException overflow) {
            throw new InvalidStockChangeException("stock cannot exceed " + Integer.MAX_VALUE);
        }
    }

    /**
     * Takes stock out.
     *
     * @throws InvalidStockChangeException if {@code qty} is not positive or is more than is in stock
     */
    void remove(int qty) {
        requirePositive(qty);
        if (qty > stock) {
            throw new InvalidStockChangeException("only " + stock + " in stock");
        }
        stock = stock - qty;
    }

    private static void requirePositive(int qty) {
        if (qty < 1) {
            throw new InvalidStockChangeException("quantity must be at least 1");
        }
    }
}
