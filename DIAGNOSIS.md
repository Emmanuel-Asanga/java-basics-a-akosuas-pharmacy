# Diagnosis: Akosua's Pharmacy Tracker

I reviewed the original `PharmacyTracker.java` before making any changes. I kept the starter file as it was, then compiled and ran it to understand how the program currently works and where the problems are.

This is the output I got:

```text
Dispensed 2 x Paracetamol 500mg to Kofi Mensah for GHS 25.0
Dispensed 1 x Amoxicillin 250mg to Ama Owusu (prescribed by Dr. Boateng) for GHS 28.5
Restocked 50 x Paracetamol 500mg
Dispensed 100 x Amoxicillin 250mg to Kofi Mensah (prescribed by Dr. Boateng) for GHS 3000.0

=== Akosua's Pharmacy - end of day ===
Stock levels:
  Paracetamol 500mg (OTC): 148 in stock
  Amoxicillin 250mg (PRESCRIPTION): -61 in stock
Transactions today: 4
```

The main issue I noticed is the last dispensing operation. The pharmacy had only 39 units of Amoxicillin left, but the program still allowed 100 units to be dispensed. It also charged the customer GHS 3000 and showed the remaining stock as `-61`.

This tells me that the program does not properly check the available stock before completing a sale. It is a serious problem because the pharmacy may end up relying on stock figures that are not correct.

## 1. Defects I found and their priorities

I have arranged the defects according to how much damage they could cause to the pharmacy.

### 1. Stock can become negative

**What I found**

The `dispense` method does not check whether enough medicine is available before reducing the stock. Also, the `stock` field in `Medication` is public, which means other classes can change it directly.

**Root cause**

There are two related problems. First, the stock check is missing from the dispensing process. Secondly, the stock field is not protected. Even if I add a check inside `dispense`, another part of the program could still change the stock directly and bypass that check.

There are other ways the stock can become incorrect too:

- Calling `dispense(..., -5, ...)` could increase the stock instead of reducing it.
- Calling `restock(..., -200)` could reduce the stock.
- Restocking with a very large number could cause an integer overflow.

**Business risk**

The pharmacy could sell medicine that is not actually available. It could also make wrong purchasing decisions because the stock report would not reflect the real situation. This is especially serious because other parts of the program depend on the stock value.

I consider this the most important defect because an incorrect stock figure may not be noticed immediately. The program can continue running while staff make decisions using the wrong information.

### 2. The program does not always report failures properly

**What I found**

- The `dispense` method returns `void`. When a prescription sale is refused, it only prints a message. The code that called the method cannot easily know whether the operation succeeded or failed.
- The program checks category values using strings. If the category is something other than exactly `"OTC"` or `"PRESCRIPTION"` (for example, `"Prescription"`), neither branch runs.
- In that situation, the program can still add an entry to the customer's history and create a `DISPENSE` transaction with a total of 0, even though no medicine was dispensed.
- The prescriber check only checks whether `prescriber` is `null`. An empty string (`""`) is still accepted.

**Root cause**

The program uses text messages and string values to control important business rules. It does not return a clear result that tells the caller what happened.

**Business risk**

The pharmacy's transaction records may show a sale that did not actually take place. This can make it difficult to investigate problems and trust the records.

### 3. Too much of the dispensing logic is inside one method

**What I found**

The `dispense` method checks the medicine category using:

- `m.category.equals("OTC")`
- `m.category.equals("PRESCRIPTION")`

Both branches repeat similar operations, such as calculating the price, applying the registered customer discount, reducing stock and printing the result.

**Root cause**

The method uses category strings and separate blocks of repeated code to decide how a medicine should be handled.

**Business risk**

If the pricing or stock logic changes in one branch but not the other, the two categories may start behaving differently. A small change could also introduce another problem without being noticed. This makes the program harder to maintain.

### 4. Important records can be changed from outside the classes

**What I found**

- The `meds` and `customers` lists are public and can be modified directly.
- `getTransactions()` returns the actual internal transaction list. A caller could use `clear()` to remove the transaction history.
- Fields such as `id`, `name`, `price` and `category` are public and mutable.
- The fields in `Transaction` can also be changed.
- Transactions identify medicines and customers using their names. If a name is changed later, it may no longer be clear which medicine or customer an old transaction refers to.

**Root cause**

The program does not properly control access to its data. Other classes can change records that should be managed by the pharmacy itself.

**Business risk**

The pharmacy may not be able to rely on its records. For example, transaction history could be changed or removed after a sale has taken place.

### 5. Other issues I noticed

These issues are also worth addressing, although I would not place them above the stock and transaction problems.

- **Money uses `double`:** This can cause rounding problems when calculating money. A more suitable money type can be considered later.
- **IDs use a static counter:** The `static int counter` is shared globally and is not thread-safe.
- **The `Pharmacy` class handles too many responsibilities:** It manages stock, pricing, discounts, records and console output. This makes it harder to test and maintain.
- **No registration check:** The program does not confirm that a medicine or customer belongs to the pharmacy before dispensing.
- **Repeated magic string:** The value `"(supplier)"` is written directly in the code instead of being managed in one place.

## 2. What the planned controlled-substances feature means for the current design

The pharmacy may need to support controlled substances in the future. For this category, every dispensing operation must record the prescriber's details.

The current design will make this change difficult for a few reasons:

1. The prescriber is not stored in the transaction. It only appears in the console message. The `Transaction` class does not have a field for it, so the information is lost after the message is printed.
2. Adding a third medicine category would require another condition in `dispense`, together with another block of similar pricing and stock logic.
3. `printReport` and any other code that checks the category string would also need to be reviewed.
4. If the new category is typed incorrectly, it could cause the same problem described in defect 2: the program may record a transaction even though the dispensing operation did not happen.

This means the current design would require changes in several places, including `dispense` and `Transaction`, and possibly `printReport`. I want the new structure to make it easier to add another medicine category without having to keep adding conditions to one large method.

## 3. Decisions before implementation

The requirement is to keep the original program's behaviour while making sure the stock rule is enforced. I have considered the areas where these requirements could conflict and made the following decisions.

| Question | My decision | Reason |
|---|---|---|
| What should happen when someone tries to dispense more than the available stock? | Refuse the operation. Stock should remain unchanged, and no history or transaction should be created. The caller should receive a clear `Refused` result with a message. | The original program already refuses a prescription sale by printing a message and recording nothing. I will keep that behaviour but return a result so the calling code can know what happened. |
| What should happen when the quantity is zero or negative? | Refuse the dispense or restock operation. | Negative quantities could be used to change stock in the wrong direction. Zero quantities should not create unnecessary transactions. |
| Should I change the money type? | Keep `double` for this rebuild. | Changing to `BigDecimal` could change the displayed output, such as `25.0` becoming `25.00`. I will record this as a possible future improvement instead of changing it now. |
| Should walk-in customers still have their history recorded? | Yes, keep the original behaviour. | The original program records their history. I do not want to introduce another behaviour change without a clear requirement. |
| Should a blank prescriber name be accepted for an ordinary prescription medicine? | Keep accepting it for now, as the original program does. | Rejecting it would introduce another behaviour change. I will point it out as something that needs a decision. The controlled category will have a stricter prescriber requirement. |
| How should unknown category and customer-type values be handled? | Use enums instead of free-text strings. | This prevents spelling differences and invalid values from silently bypassing the program's logic. |

## 4. Planned project structure

The approved structure is shown in `docs/structure-sketch.svg`. I have also written it below so the responsibilities of the classes are clear.

```text
pharmacy.PharmacyTracker
    Entry point and demo only
          |
          v
pharmacy.ui
    ConsoleView
    The only class responsible for printing

pharmacy.domain
    Pharmacy
        Controls dispensing and restocking
        Owns the audit trail

    Medication
        Keeps stock private
        Provides controlled stock changes

    MedicationCategory
        Enum: OTC, PRESCRIPTION
        Can be extended with CONTROLLED later
        Holds the prescriber rule

    Customer
        Keeps customer history private
        Provides a read-only view

    CustomerKind
        Enum: WALK_IN, REGISTERED
        Holds the 5% discount rule

    Transaction
        Immutable transaction record

    TransactionType
        Enum: DISPENSE, RESTOCK

    Outcome
        Result of an operation:
        Completed or Refused(reason)

    InvalidStockChangeException
        Used when a stock change would break the stock rule
```

The main reason for this structure is to make sure each class has a clear responsibility.

The stock rule will be enforced inside `Medication`, where the stock value is kept private. This means other classes cannot directly set the stock to an invalid value. The `Pharmacy` class will control dispensing and restocking, so stock changes can be recorded in the transaction history.

I will also use enums for medicine categories and customer types instead of comparing free-text strings. The rules that depend on each type will be kept with the relevant enum. Finally, console printing will be handled by `ConsoleView`, rather than being mixed with the business logic. This should make the main operations easier to test.

## 5. How I will check that the original behaviour is preserved

I do not want the rebuild to change several things at once. My plan is:

1. Keep the original program's output as a reference before making code changes.
2. After each refactoring commit, run the rebuilt program and compare its output with the original output.
3. Keep the commit that enforces the stock rule separate from the other refactoring work. Before that commit, the output should remain the same, including the `-61` stock figure. After that commit, the over-dispensing operation should be refused, so the output and final stock figures should change as expected.
4. Test additional cases that are not covered by the main demo. These include:
   - A registered customer buying an OTC medicine and receiving the 5% discount.
   - A prescription medicine with a missing prescriber.
   - A prescription medicine with a blank prescriber.
   - An OTC medicine being given a prescriber.
   - A customer buying exactly the remaining available stock.

I will use these tests to check that the refactoring has not changed the existing behaviour unexpectedly.

## 6. Improvement I also plan to make

One improvement I want to include is storing the prescriber's name in the transaction record whenever a prescribed medicine is dispensed and also adding reordered level check for stock.
   - what it does 
     when stock falls to a medicine’s reorder level or below, the end-of-day report adds a “Reorder needed” section.

At the moment, the prescriber's name is only printed in the console message. It is not saved in the transaction history. Adding it to the transaction will make the information available later, especially when the pharmacy introduces controlled substances.

This will add useful information to the records without changing the existing console output.
