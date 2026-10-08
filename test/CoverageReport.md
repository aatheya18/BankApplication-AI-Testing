# Test Coverage Report

## Overview
A comprehensive test suite was implemented for the Banking system (`Bank.java` and `BankAccount.java`) using JUnit 5. The tests were run, and a coverage report was generated using JaCoCo.

## Coverage Metrics
The test suite achieves **100% test coverage** across all critical metrics for both classes.

### 1. `com.nirma.banking.Bank`
* **Instruction Coverage:** 100% (167 / 167)
* **Branch Coverage:** 100% (18 / 18)
* **Line Coverage:** 100% (41 / 41)
* **Method Coverage:** 100% (9 / 9)
* **Cyclomatic Complexity:** 100% (18 / 18)

### 2. `com.nirma.banking.BankAccount`
* **Instruction Coverage:** 100% (250 / 250)
* **Branch Coverage:** 100% (18 / 18)
* **Line Coverage:** 100% (58 / 58)
* **Method Coverage:** 100% (15 / 15)
* **Cyclomatic Complexity:** 100% (24 / 24)

## Summary of Test Cases
- **Initialization:** Verification of successful object creation and validation against `null` or invalid arguments (e.g. empty strings, negative balances).
- **Core Operations:** Tests for deposits, withdrawals, and transfers including balance updates and strict checking of negative/insufficient amounts.
- **Account State & History:** Tests ensuring proper status transitions (e.g. closing an account) and accurate recording of transaction histories.
- **Concurrency & Threading (Transfer):** Validation of lock ordering within the `transfer()` method to prevent deadlocks when simultaneously sending funds.

*Note: The generated JaCoCo HTML report is available in the `report_jacoco/` directory in the project root.*
