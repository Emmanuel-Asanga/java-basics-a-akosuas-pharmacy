package pharmacy.domain;

/**
 * Thrown by {@link Medication} when a change would leave its stock count invalid
 * (below zero, a non-positive quantity, or too large to hold).
 *
 * <p>It is unchecked because it signals that a rule was about to be broken. {@link Pharmacy}
 * turns it into a {@link Outcome.Refused}, so the people using the program never see an exception.
 * The message is a short reason such as "only 39 in stock".
 */
public final class InvalidStockChangeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidStockChangeException(String reason) {
        super(reason);
    }
}

