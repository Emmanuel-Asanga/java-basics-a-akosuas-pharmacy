package pharmacy.domain;

/** The kinds of customer. Each kind knows its own price rule, so no caller compares strings. */
public enum CustomerKind {

    WALK_IN {
        @Override
        public double priceFor(double subtotal) {
            return subtotal;
        }
    },

    REGISTERED {
        @Override
        public double priceFor(double subtotal) {
            return subtotal * 0.95; // registered patients get 5% off
        }
    };

    /** The amount this kind of customer pays for a basket worth {@code subtotal}. */
    public abstract double priceFor(double subtotal);
}
