# Akosua's Pharmacy Tracker (Rebuild)

This project is a rebuild of the original stock and dispensing program for Akosua's Pharmacy.

The main aim is to organise the code so it is easier to understand, test and maintain. Most of the original behaviour is kept. The main change is that the program now prevents the stock quantity from becoming negative or otherwise invalid.

- **Diagnosis:** [DIAGNOSIS.md](DIAGNOSIS.md) — written before I changed the code.
- **Structure sketch:** [docs/Pharmacy_Rebuild.drawio.pdf](docs/Pharmacy_Rebuild.drawio.pdf)
- **Original program:** [legacy/PharmacyTracker.java](legacy/PharmacyTracker.java) — kept unchanged so I can compare it with the rebuild.

## Running the project

You need JDK 21 or later and Maven. There are no runtime dependencies; JUnit 5 is used for tests only.

```bash
mvn test                                   # compile and run all tests
mvn compile                                # compile only
java -cp target/classes pharmacy.PharmacyTracker   # run the demo
```


## Project structure

```text
src/main/java/pharmacy/
  PharmacyDemo.java             Entry point and demo
  PharmacyTracker.java             
  domain/
    Pharmacy.java                   Handles dispensing, restocking and transaction history
    Medication.java                 Stores medicine details and controls stock changes
    MedicationCategory.java         OTC and PRESCRIPTION categories and prescriber rules
    Customer.java                   Stores customer details and purchase history
    CustomerKind.java               WALK_IN and REGISTERED customer types and discount rule
    Transaction.java                Immutable record of a pharmacy transaction
    TransactionType.java             DISPENSE and RESTOCK transaction types
    Outcome.java                     Result of an operation: Completed or Refused
    InvalidStockChangeException.java Exception raised for an invalid stock change
  ui/
    ConsoleView.java                 Handles console output

src/test/java/pharmacy/
  Shop.java                          Shared test fixture (same stock and customers as the demo)
  StockRuleTest.java                 Stock protection, overflow, refusal reasons
  OriginalBehaviourTest.java         Behaviour kept from the original, prescriber audit record
  ReorderLevelTest.java              Reorder-level alerts and report section
  EncapsulationTest.java             Read-only views, private fields, immutable records
  ControlledMedicationTest.java      CONTROLLED category rules
```

The main classes are separated according to what they do. The domain classes handle the pharmacy rules, while `ConsoleView` handles printing. The test folder contains the JUnit tests I used during the rebuild.

## Main behaviour change: stock protection

The original program allowed a dispensing operation to go through even when there was not enough stock. In the demo, it dispensed 100 units of Amoxicillin when only 39 were available. The stock then became `-61`.

The rebuild refuses operations that would make the stock invalid.

| Situation | Original program | Rebuilt program |
|---|---|---|
| Dispense more medicine than is available | Completes the sale and allows stock to become negative (`-61` in the demo) | Refuses the operation. Stock, customer history and transactions remain unchanged. |
| Dispense or restock with a zero or negative quantity | Allows the operation; a negative dispense can increase stock, while a negative restock can reduce it | Refuses the operation |
| Restock beyond the maximum value of an `int` | Can overflow and produce an incorrect negative value | Refuses the operation |
| Create medicine with negative opening stock | Accepts it | Rejects it during construction |

For the over-dispensing example, the rebuilt program displays:

```text
Cannot dispense 100 x Amoxicillin 250mg: only 39 in stock
```

In the demo, the expected changes are the refusal message, Amoxicillin remaining at `39` instead of `-61`, and the transaction count changing from 4 to 3. The comparison script shows the differences between the two outputs.

## Design decisions

### 1. Stock validation is handled by `Medication`

In the original program, there was no single place responsible for making sure stock stayed valid. Adding a check only inside `Pharmacy.dispense()` would help with dispensing, but another part of the program could still change the public `stock` field directly.

In the rebuild, `stock` is private. The `add()` and `remove()` methods check the quantity before changing it. This means the stock rule is enforced where the stock is managed.

I did not choose to validate only inside `Pharmacy.dispense()` because that would leave other ways for the stock value to become invalid.

### 2. Stock changes go through `Pharmacy`

The `add()` and `remove()` methods are package-private. The `Pharmacy` class controls dispensing and restocking, and it also keeps the transaction history.

The methods `getTransactions()`, `getMedications()`, `getCustomers()` and `getHistory()` return read-only views. `Transaction` is also immutable.

I avoided making `Medication.remove()` public because that would allow stock to change without the pharmacy creating a transaction for it.

### 3. Enums are used instead of category strings

The original program used strings such as `"OTC"` and `"PRESCRIPTION"` to decide how a medicine should be handled. A spelling mistake could cause the program to skip the expected logic.

The rebuild uses `MedicationCategory` and `CustomerKind` enums. The prescriber rule is kept in `MedicationCategory`, while the 5% registered-customer discount is kept in `CustomerKind`.

I considered using separate subclasses such as `OtcMedication` and `PrescriptionMedication`. I chose enums for now because the categories have the same basic data but follow different rules. If a future category needs different data or much more specialised behaviour, subclasses may be worth considering.

### 4. Exceptions are used inside the domain, and `Outcome` is returned to the caller

`Medication` throws `InvalidStockChangeException` when a stock change would break the stock rule.

At the pharmacy level, a refused sale is a normal business outcome, not something staff should see as a program crash. For that reason, `Pharmacy` returns an `Outcome` that is either `Completed` or `Refused(reason)`.

**Where the exception is caught.** `Medication.add()` and `remove()` throw `InvalidStockChangeException`. `Pharmacy.dispense()` and `Pharmacy.restock()` catch it and return `Refused(reason)`, so it never reaches `ConsoleView` or the caller. `StockRuleTest.refusalReasonsAreNeverBlank` checks that every refusal path carries a reason.
This also improves on the original `void` return type. The caller can now check whether the operation succeeded or was refused.


### 5. Console output is handled separately

In the original program, the dispensing logic also printed messages. This made it harder to test the pharmacy rules separately from the console.

The rebuild moves printing into `ConsoleView`. The domain classes handle the rules and return results, while `ConsoleView` decides what to print.

### 6. Java features used in the rebuild

I used some newer Java features where they help make the code clearer or safer:

- `record` for immutable transaction data.
- `enum` values to represent medicine categories, customer types and transaction types.
- A `sealed` interface for the possible operation outcomes.
- Pattern matching with `switch` so the outcome cases are handled explicitly.
- `Optional` for values that may not be present, such as a refusal reason or phone number.


### 7. Behaviour I deliberately kept

There are a few things I left unchanged so the rebuild would not introduce extra behaviour changes.

| Kept behaviour | Reason | Possible follow-up |
|---|---|---|
| Money is still stored as `double` | Changing to `BigDecimal` could change the displayed amounts, such as `25.0` to `25.00` | Consider changing the money handling separately (pinned by `OriginalBehaviourTest.blankPrescriberAccepted`)|
| Walk-in customers still receive a history entry | This is what the original program did | Confirm whether the pharmacy wants history only for registered customers |
| A blank prescriber (`""`) is still accepted for an ordinary prescription medicine | Rejecting it would be another behaviour change | The controlled category will require a named prescriber |

## Adding controlled substances later

I designed `MedicationCategory` so the prescriber rule is handled there, and I added a `CONTROLLED` constant to show it works. It overrides `screen()` to require a named, non-blank prescriber:
(keep the existing code block, then add:)
`ControlledMedicationTest` covers a null prescriber, a blank prescriber and a named prescriber. `Pharmacy.dispense()` did not need to change.


The main reason this is easier in the rebuild is that the stock rule, customer discount and transaction handling are already managed separately. Adding another category does not require another copy of the dispensing logic.

In the original design, I would have needed to make changes in several places:

| Area | Change that would be needed in the original |
|---|---|
| `Pharmacy.dispense()` | Add another `else if` and repeat the pricing, discount and stock logic |
| `Transaction` | Add a field for the prescriber, because it was not stored |
| `printReport()` and other category checks | Review the places that read the category string |

A typing mistake in the new category could also have caused the original program to record a transaction for a sale that did not happen.

## Additional improvement: reorder levels

`PharmacyDemo.runReorderDemo()` shows this in the committed demo. Run it with `java -cp target/classes pharmacy.PharmacyTracker`; it prints:
```text
=== Akosua's Pharmacy (reorder demo) - end of day ===
Stock levels:
  Paracetamol 500mg (OTC): 15 in stock
  Amoxicillin 250mg (PRESCRIPTION): 4 in stock
Transactions today: 2
Reorder needed:
  Paracetamol 500mg: 15 left (reorder at 20)
  Amoxicillin 250mg: 4 left (reorder at 10)
```

I chose this improvement because preventing negative stock is important, but it also means staff may only discover a shortage when a customer is trying to buy the medicine. A reorder warning can give the pharmacy an earlier indication that stock is getting low.

The main decisions for this feature are:

- The reorder level defaults to `0`, which means no reorder warning is shown.
- `needsReorder()` checks `reorderLevel > 0 && stock <= reorderLevel`.
- The reorder level is final and cannot be negative.
- The feature only reads stock. It does not change the stock quantity.
- `ConsoleView` is still responsible for printing the warning.

I considered automatic supplier reordering and saving stock to a file. I did not include them because they would require additional decisions and changes beyond this rebuild. A reorder level was a smaller feature that I could test and connect directly to the stock problem.

The prescriber field in `Transaction` is separate from this improvement. I added it as groundwork for the controlled-substances requirement, since that information needs to be available in the audit record.

## Testing and comparison with the original
Tests are written with JUnit 5 and run with `mvn test`. 
The checks cover the following:

| Check | What I verified |
|---|---|
| Demo without the over-dispensing call | The original and rebuilt output are identical |
| Additional scenarios not covered by the demo | Registered discount on OTC medicine, missing and blank prescriber, OTC medicine given a prescriber, exact remaining-stock purchase, IDs, history and transactions |
| Full demo | The differences are limited to the stock-protection behaviour described above |

I also compared the output after each of the five refactoring commits. Those versions reproduced the original output exactly, including the `-61` stock figure. The stock-protection change was kept in a separate commit.

I tested the checks by changing the discount from `0.95` to `0.96`. The comparison and unit tests then failed, which showed that the checks could detect that change.

The project has a small hand-written test runner in `AllTests.java`, so it does not need JUnit or a network connection to run the tests.

## Changes from the original structure sketch

The implementation follows the structure in `docs/Pharmacy_Rebuild.drawio.pdf`, which was committed before the reorder-level code.

There are a few differences from the first sketch:

- **The additional improvement changed.** In `DIAGNOSIS.md`, I initially planned to make prescriber recording the improvement. I later treated it as groundwork for controlled substances and chose reorder levels as the additional feature. I left `DIAGNOSIS.md` unchanged because it records what I planned before implementation.
- **The `Completed` outcome changed.** The first sketch showed `Outcome.Completed(transaction, prescriber)`. Since the prescriber is now stored in `Transaction`, `Completed` only needs the transaction.
- **`MedicationCategory` has a `usesPrescriber()` flag.** This helps the display logic stay consistent with the original behaviour, where the prescriber message was shown for prescription medicine.

## Current limitations

There are still some limitations in the program:

- Data is kept in memory, so it is lost when the program closes. The original program worked the same way.
- IDs use `AtomicInteger`, but stock changes are not synchronised. This is suitable for the current single-user console program. If several terminals share one `Pharmacy` instance in the future, stock changes will need proper synchronisation.
- The domain classes are in the same `pharmacy.domain` package. Package-private methods are therefore accessible to other classes in that package. A reflection test checks that a public stock setter has not been added.
