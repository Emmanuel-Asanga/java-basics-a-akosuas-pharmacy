package pharmacy.domain;

/**
 * What happened when something was asked of the {@link Pharmacy}.
 *
 * <p>The original printed a message and returned nothing, so a caller could not tell whether a
 * sale had happened. This type is sealed: there are exactly two answers, and a {@code switch}
 * over it must handle both, so a refusal cannot be ignored by accident.
 */
public sealed interface Outcome {

    /** It went through; the transaction is the record that was written. */
    record Completed(Transaction transaction) implements Outcome {
    }

    /** It was refused, with the reason. Nothing was changed or recorded. */
    record Refused(String reason) implements Outcome {
    }
}
