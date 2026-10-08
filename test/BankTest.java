package com.nirma.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;

class BankTest {

    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank();
    }

    @Test
    void testCreateAccount() {
        BankAccount account = bank.createAccount("111", "Alice", new BigDecimal("500.00"));
        assertNotNull(account);
        assertEquals("111", account.getAccountNumber());
        assertEquals("Alice", account.getAccountHolder());
        assertEquals(new BigDecimal("500.00"), account.getBalance());
        assertEquals(1, bank.getAccountCount());
    }

    @Test
    void testCreateAccountInvalid() {
        assertThrows(NullPointerException.class, () -> bank.createAccount(null, "Alice", new BigDecimal("500.00")));
        assertThrows(NullPointerException.class, () -> bank.createAccount("111", null, new BigDecimal("500.00")));
        assertThrows(NullPointerException.class, () -> bank.createAccount("111", "Alice", null));
        
        bank.createAccount("111", "Alice", new BigDecimal("500.00"));
        assertThrows(IllegalArgumentException.class, () -> bank.createAccount("111", "Bob", new BigDecimal("100.00"))); // Duplicate
    }

    @Test
    void testFindAccount() {
        bank.createAccount("111", "Alice", new BigDecimal("500.00"));
        BankAccount found = bank.findAccount("111");
        assertNotNull(found);
        assertEquals("Alice", found.getAccountHolder());
        
        // Test trim
        BankAccount foundTrim = bank.findAccount(" 111 ");
        assertEquals("Alice", foundTrim.getAccountHolder());
    }

    @Test
    void testFindAccountInvalid() {
        assertThrows(IllegalArgumentException.class, () -> bank.findAccount(null));
        assertThrows(IllegalArgumentException.class, () -> bank.findAccount(""));
        assertThrows(IllegalArgumentException.class, () -> bank.findAccount("   "));
        assertThrows(IllegalArgumentException.class, () -> bank.findAccount("999")); // Not found
    }

    @Test
    void testDepositAndWithdraw() {
        bank.createAccount("111", "Alice", new BigDecimal("500.00"));
        bank.deposit("111", new BigDecimal("200.00"));
        assertEquals(new BigDecimal("700.00"), bank.findAccount("111").getBalance());

        bank.withdraw("111", new BigDecimal("100.00"));
        assertEquals(new BigDecimal("600.00"), bank.findAccount("111").getBalance());
    }

    @Test
    void testTransfer() {
        bank.createAccount("111", "Alice", new BigDecimal("500.00"));
        bank.createAccount("222", "Bob", new BigDecimal("300.00"));

        bank.transfer("111", "222", new BigDecimal("150.00"));

        assertEquals(new BigDecimal("350.00"), bank.findAccount("111").getBalance());
        assertEquals(new BigDecimal("450.00"), bank.findAccount("222").getBalance());
        
        // Reverse order of account numbers to test lock ordering
        bank.transfer("222", "111", new BigDecimal("50.00"));
        
        assertEquals(new BigDecimal("400.00"), bank.findAccount("111").getBalance());
        assertEquals(new BigDecimal("400.00"), bank.findAccount("222").getBalance());
    }

    @Test
    void testTransferInvalid() {
        bank.createAccount("111", "Alice", new BigDecimal("500.00"));
        
        assertThrows(IllegalArgumentException.class, () -> bank.transfer(null, "111", new BigDecimal("10.00")));
        assertThrows(IllegalArgumentException.class, () -> bank.transfer("111", null, new BigDecimal("10.00")));
        assertThrows(IllegalArgumentException.class, () -> bank.transfer("111", "111", new BigDecimal("10.00"))); // Same account
    }

    @Test
    void testGetAllAccountsAndClear() {
        bank.createAccount("111", "Alice", new BigDecimal("500.00"));
        bank.createAccount("222", "Bob", new BigDecimal("300.00"));

        List<BankAccount> all = bank.getAllAccounts();
        assertEquals(2, all.size());

        bank.clear();
        assertEquals(0, bank.getAccountCount());
        assertEquals(0, bank.getAllAccounts().size());
    }
}
