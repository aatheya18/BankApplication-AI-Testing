package com.nirma.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

class BankAccountTest {

    private BankAccount account;

    @BeforeEach
    void setUp() {
        account = new BankAccount("12345", "John Doe", new BigDecimal("100.00"));
    }

    @Test
    void testValidInitialization() {
        assertEquals("12345", account.getAccountNumber());
        assertEquals("John Doe", account.getAccountHolder());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        assertTrue(account.isActive());
        assertEquals(1, account.getTransactionHistory().size());
        assertTrue(account.getTransactionHistory().get(0).startsWith("OPEN:"));
    }

    @Test
    void testInvalidInitialization() {
        assertThrows(IllegalArgumentException.class, () -> new BankAccount("", "John Doe", new BigDecimal("100.00")));
        assertThrows(IllegalArgumentException.class, () -> new BankAccount(null, "John Doe", new BigDecimal("100.00")));
        assertThrows(IllegalArgumentException.class, () -> new BankAccount("12345", "", new BigDecimal("100.00")));
        assertThrows(IllegalArgumentException.class, () -> new BankAccount("12345", "John Doe", new BigDecimal("-10.00")));
        assertThrows(IllegalArgumentException.class, () -> new BankAccount("12345", "John Doe", new BigDecimal("100.005")));
        assertThrows(NullPointerException.class, () -> new BankAccount("12345", "John Doe", null));
    }

    @Test
    void testDeposit() {
        account.deposit(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("150.00"), account.getBalance());
        assertEquals(2, account.getTransactionHistory().size());
        assertTrue(account.getTransactionHistory().get(1).startsWith("DEPOSIT:"));
    }

    @Test
    void testInvalidDeposit() {
        assertThrows(IllegalArgumentException.class, () -> account.deposit(new BigDecimal("-10.00")));
        assertThrows(IllegalArgumentException.class, () -> account.deposit(new BigDecimal("0.00")));
        assertThrows(NullPointerException.class, () -> account.deposit(null));
    }

    @Test
    void testWithdraw() {
        account.withdraw(new BigDecimal("40.00"));
        assertEquals(new BigDecimal("60.00"), account.getBalance());
        assertEquals(2, account.getTransactionHistory().size());
        assertTrue(account.getTransactionHistory().get(1).startsWith("WITHDRAW:"));
    }

    @Test
    void testInvalidWithdraw() {
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(new BigDecimal("-10.00")));
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(new BigDecimal("0.00")));
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(new BigDecimal("150.00"))); // Insufficient funds
    }

    @Test
    void testCloseAccount() {
        account.withdraw(new BigDecimal("100.00")); // Empty the account
        account.close();
        assertFalse(account.isActive());
        assertThrows(IllegalStateException.class, () -> account.deposit(new BigDecimal("10.00")));
        assertThrows(IllegalStateException.class, () -> account.withdraw(new BigDecimal("10.00")));
        assertThrows(IllegalStateException.class, () -> account.close());
    }

    @Test
    void testCloseAccountWithBalance() {
        assertThrows(IllegalStateException.class, () -> account.close());
    }

    @Test
    void testGetSummary() {
        String summary = account.getSummary();
        assertEquals("12345 | John Doe | 100.00 | ACTIVE", summary);
        
        account.withdraw(new BigDecimal("100.00"));
        account.close();
        String closedSummary = account.getSummary();
        assertEquals("12345 | John Doe | 0.00 | CLOSED", closedSummary);
    }
}
