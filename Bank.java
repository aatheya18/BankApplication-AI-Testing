package com.nirma.banking;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Bank {
    private final ConcurrentMap<String, BankAccount> accounts = new ConcurrentHashMap<>();

    public BankAccount createAccount(String accountNumber, String accountHolder, BigDecimal openingBalance) {
        Objects.requireNonNull(accountNumber, "Account number cannot be null");
        Objects.requireNonNull(accountHolder, "Account holder cannot be null");
        Objects.requireNonNull(openingBalance, "Opening balance cannot be null");

        BankAccount account = new BankAccount(accountNumber, accountHolder, openingBalance);
        BankAccount existing = accounts.putIfAbsent(account.getAccountNumber(), account);
        if (existing != null) {
            throw new IllegalArgumentException("Account number already exists: " + accountNumber);
        }
        return account;
    }

    public BankAccount findAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be blank");
        }

        BankAccount account = accounts.get(accountNumber.trim());
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }
        return account;
    }

    public void deposit(String accountNumber, BigDecimal amount) {
        findAccount(accountNumber).deposit(amount);
    }

    public void withdraw(String accountNumber, BigDecimal amount) {
        findAccount(accountNumber).withdraw(amount);
    }

    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        if (fromAccountNumber == null || toAccountNumber == null) {
            throw new IllegalArgumentException("Both account numbers are required");
        }

        String sourceId = fromAccountNumber.trim();
        String targetId = toAccountNumber.trim();

        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }

        BankAccount source = findAccount(sourceId);
        BankAccount destination = findAccount(targetId);

        BankAccount first = sourceId.compareTo(targetId) < 0 ? source : destination;
        BankAccount second = first == source ? destination : source;

        synchronized (first) {
            synchronized (second) {
                source.withdraw(amount);
                destination.deposit(amount);
            }
        }
    }

    public List<BankAccount> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public int getAccountCount() {
        return accounts.size();
    }

    public void clear() {
        accounts.clear();
    }
}
