package com.nirma.banking;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BankAccount {
    private final String accountNumber;
    private final String accountHolder;
    private BigDecimal balance;
    private boolean active = true;
    private final List<String> transactionHistory = new ArrayList<>();
    private final Object lock = new Object();

    public BankAccount(String accountNumber, String accountHolder, BigDecimal openingBalance) {
        this.accountNumber = requireText(accountNumber, "Account number");
        this.accountHolder = requireText(accountHolder, "Account holder");
        this.balance = validateAmount(openingBalance, "Opening balance");
        recordTransaction("OPEN", openingBalance);
    }

    public void deposit(BigDecimal amount) {
        synchronized (lock) {
            ensureActive();
            BigDecimal normalizedAmount = validatePositiveAmount(amount, "Deposit amount");
            balance = balance.add(normalizedAmount);
            recordTransaction("DEPOSIT", normalizedAmount);
        }
    }

    public void withdraw(BigDecimal amount) {
        synchronized (lock) {
            ensureActive();
            BigDecimal normalizedAmount = validatePositiveAmount(amount, "Withdrawal amount");
            if (balance.compareTo(normalizedAmount) < 0) {
                throw new IllegalArgumentException("Insufficient funds");
            }
            balance = balance.subtract(normalizedAmount);
            recordTransaction("WITHDRAW", normalizedAmount);
        }
    }

    public void close() {
        synchronized (lock) {
            ensureActive();
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalStateException("Account balance must be zero before closing");
            }
            active = false;
            recordTransaction("CLOSE", BigDecimal.ZERO);
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public BigDecimal getBalance() {
        synchronized (lock) {
            return balance;
        }
    }

    public boolean isActive() {
        synchronized (lock) {
            return active;
        }
    }

    public List<String> getTransactionHistory() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(transactionHistory));
        }
    }

    public synchronized String getSummary() {
        return accountNumber + " | " + accountHolder + " | " + balance.toPlainString() + " | " + (active ? "ACTIVE" : "CLOSED");
    }

    private void ensureActive() {
        if (!active) {
            throw new IllegalStateException("Account is closed");
        }
    }

    private void recordTransaction(String type, BigDecimal amount) {
        transactionHistory.add(type + ": " + amount.toPlainString());
    }

    private static BigDecimal validatePositiveAmount(BigDecimal amount, String label) {
        BigDecimal validated = validateAmount(amount, label);
        if (validated.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero");
        }
        return validated;
    }

    private static BigDecimal validateAmount(BigDecimal amount, String label) {
        Objects.requireNonNull(amount, label + " cannot be null");
        if (amount.scale() > 2 || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " must be a non-negative amount with at most two decimals");
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value.trim();
    }
}
