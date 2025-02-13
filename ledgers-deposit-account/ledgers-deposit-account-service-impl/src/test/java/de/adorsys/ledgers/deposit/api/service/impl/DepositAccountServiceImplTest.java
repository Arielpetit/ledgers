/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.deposit.api.service.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.adorsys.ledgers.deposit.api.domain.*;
import de.adorsys.ledgers.deposit.api.service.DepositAccountConfigService;
import de.adorsys.ledgers.deposit.api.service.mappers.TransactionDetailsMapper;
import de.adorsys.ledgers.deposit.db.domain.AccountType;
import de.adorsys.ledgers.deposit.db.domain.AccountUsage;
import de.adorsys.ledgers.deposit.db.domain.DepositAccount;
import de.adorsys.ledgers.deposit.db.repository.DepositAccountRepository;
import de.adorsys.ledgers.postings.api.domain.AccountStmtBO;
import de.adorsys.ledgers.postings.api.domain.LedgerAccountBO;
import de.adorsys.ledgers.postings.api.domain.LedgerBO;
import de.adorsys.ledgers.postings.api.domain.PostingLineBO;
import de.adorsys.ledgers.postings.api.service.AccountStmtService;
import de.adorsys.ledgers.postings.api.service.LedgerService;
import de.adorsys.ledgers.postings.api.service.PostingService;
import de.adorsys.ledgers.util.exception.DepositModuleException;
import de.adorsys.ledgers.util.exception.PostingModuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import pro.javatar.commons.reader.YamlReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static de.adorsys.ledgers.util.exception.PostingErrorCode.POSTING_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
class DepositAccountServiceImplTest {
    private static final String ACCOUNT_ID = "ACCOUNT_ID";
    private static final String POSTING_ID = "posting_ID";
    private static final String SYSTEM = "System";
    private static final String USER_ID = "123";
    private static final LocalDateTime CREATED = LocalDateTime.now();
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final Currency EUR = Currency.getInstance("EUR");

    @Mock
    private DepositAccountRepository depositAccountRepository;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private DepositAccountConfigService depositAccountConfigService;
    @Mock
    private PostingService postingService;
    @Mock
    private TransactionDetailsMapper transactionDetailsMapper;
    @Mock
    private AccountStmtService accountStmtService;

    @InjectMocks
    private DepositAccountServiceImpl depositAccountService;

    private static final ObjectMapper STATIC_MAPPER = new ObjectMapper()
                                                              .findAndRegisterModules()
                                                              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                                                              .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                                                              .configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false)
                                                              .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                                                              .registerModule(new Jdk8Module())
                                                              .registerModule(new JavaTimeModule())
                                                              .registerModule(new ParameterNamesModule());

    @Test
    void createDepositAccount() {
        // Given
        when(depositAccountConfigService.getDepositParentAccount()).thenReturn(getLedgerAccountBO().getName());
        when(ledgerService.newLedgerAccount(any(), anyString())).thenReturn(getLedgerAccountBO());
        when(depositAccountRepository.save(any())).thenReturn(getDepositAccount(false, null));
        when(ledgerService.findLedgerByName(any())).thenReturn(Optional.of(getLedger()));

        // When
        DepositAccountBO depositAccount = depositAccountService.createNewAccount(getDepositAccountBO(), SYSTEM, "");

        // Then
        assertNotNull(depositAccount);
        assertFalse(depositAccount.getId().isEmpty());
    }

    @Test
    void createDepositAccount_account_already_exist() {
        // Given
        when(depositAccountRepository.findByIbanAndCurrency(anyString(), anyString())).thenReturn(Optional.of(getDepositAccount(false, null)));
        DepositAccountBO depositAccountBO = getDepositAccountBO();
        // Then
        assertThrows(DepositModuleException.class, () -> depositAccountService.createNewAccount(depositAccountBO, SYSTEM, ""));
    }

    @Test
    void getDepositAccountById() {
        // Given
        when(depositAccountRepository.findById(any())).thenReturn(Optional.of(getDepositAccount(false, null)));
        // When
        DepositAccountDetailsBO depositAccountDetailsBO = depositAccountService.getAccountDetailsById("id", NOW, false);
        // Then
        assertNotNull(depositAccountDetailsBO);
        assertNotNull(depositAccountDetailsBO.getAccount());
    }

    @Test
    void getDepositAccountById_wrong_id() {
        // Given
        when(depositAccountRepository.findById("wrong_id")).thenReturn(Optional.empty());

        // Then
        assertThrows(DepositModuleException.class, () -> depositAccountService.getAccountDetailsById("wrong_id", NOW, false));
    }

    @Test
    void getDepositAccountByIBANAndCurrency() {
        // Given
        when(depositAccountRepository.findByIbanAndCurrency(any(), any())).thenReturn(Optional.of(getDepositAccount(false, null)));
        // When
        DepositAccountDetailsBO accountDetailsBO = depositAccountService.getAccountDetailsByIbanAndCurrency("iban", EUR, NOW, false);
        // Then
        assertNotNull(accountDetailsBO);
        assertNotNull(accountDetailsBO.getAccount());
    }

    @Test
    void checkAccountStatus_enabled() {
        // Given
        when(depositAccountRepository.findByIbanAndCurrency(any(), any())).thenReturn(Optional.of(getDepositAccount(false, null)));
        // When
        DepositAccountDetailsBO accountDetailsBO = depositAccountService.getAccountDetailsByIbanAndCurrency("iban", EUR, NOW, false);
        boolean checkAccountStatus = accountDetailsBO.isEnabled();

        // Then
        assertNotNull(accountDetailsBO);
        assertNotNull(accountDetailsBO.getAccount());
        assertTrue(checkAccountStatus);
    }

    @Test
    void checkAccountStatus_blocked() {
        // Given
        when(depositAccountRepository.findByIbanAndCurrency(any(), any())).thenReturn(Optional.of(getDepositAccount(true, null)));
        // When
        DepositAccountDetailsBO accountDetailsBO = depositAccountService.getAccountDetailsByIbanAndCurrency("iban", EUR, NOW, false);
        boolean checkAccountStatus = accountDetailsBO.isEnabled();

        // Then
        assertNotNull(accountDetailsBO);
        assertNotNull(accountDetailsBO.getAccount());
        assertFalse(checkAccountStatus);
    }

    @Test
    void checkAccountStatus_deleted() {
        // Given
        when(depositAccountRepository.findByIbanAndCurrency(any(), any())).thenReturn(Optional.of(getDepositAccount(true, null)));
        // When
        DepositAccountDetailsBO accountDetailsBO = depositAccountService.getAccountDetailsByIbanAndCurrency("iban", EUR, NOW, false);
        boolean checkAccountStatus = accountDetailsBO.isEnabled();

        // Then
        assertNotNull(accountDetailsBO);
        assertNotNull(accountDetailsBO.getAccount());
        assertFalse(checkAccountStatus);
    }

    @Test
    void findDepositAccountsByBranch() {
        // Given
        when(depositAccountRepository.findByBranch(any())).thenReturn(Collections.singletonList(getDepositAccount(false, null)));
        // When
        List<DepositAccountDetailsBO> accounts = depositAccountService.findDetailsByBranch(anyString());

        // Then
        // account detail collection is not null
        assertNotNull(accounts);
        // account detail collection is not empty
        assertFalse(accounts.isEmpty());
    }

    @Test
    void getTransactionById() {
        // Given
        when(depositAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(readFile(DepositAccount.class, "DepositAccount.yml")));
        when(postingService.findPostingLineById(any(), any())).thenReturn(new PostingLineBO());
        when(transactionDetailsMapper.toTransactionSigned(any())).thenReturn(readFile(TransactionDetailsBO.class, "Transaction.yml"));
        // When
        TransactionDetailsBO result = depositAccountService.getTransactionById(ACCOUNT_ID, POSTING_ID);

        // Then
        assertNotNull(result);
        assertEquals(readFile(TransactionDetailsBO.class, "Transaction.yml"), result);
    }

    @Test
    void getTransactionById_Failure() {
        // Given
        when(depositAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(readFile(DepositAccount.class, "DepositAccount.yml")));
        when(postingService.findPostingLineById(any(), any())).thenThrow(PostingModuleException.builder()
                                                                                 .errorCode(POSTING_NOT_FOUND)
                                                                                 .devMsg(String.format("Could not find posting by ac id: %s and posting id: %s", ACCOUNT_ID, POSTING_ID))
                                                                                 .build());
        // Then
        assertThrows(PostingModuleException.class, () -> depositAccountService.getTransactionById(ACCOUNT_ID, POSTING_ID));
    }

    @Test
    void getTransactionsByDates() throws JsonProcessingException {
        // Given
        when(depositAccountRepository.findById(any())).thenReturn(Optional.of(new DepositAccount()));
        when(postingService.findPostingsByDates(any(), any(), any())).thenReturn(Collections.singletonList(newPostingLineBO()));
        when(transactionDetailsMapper.toTransactionSigned(any())).thenReturn(readFile(TransactionDetailsBO.class, "Transaction.yml"));

        // When
        List<TransactionDetailsBO> result = depositAccountService.getTransactionsByDates(ACCOUNT_ID, LocalDateTime.of(2018, 12, 12, 0, 0), LocalDateTime.of(2018, 12, 18, 0, 0));

        // Then
        assertFalse(result.isEmpty());
    }

    @Test
    void confirmationOfFunds_more_than_necessary_available() throws NoSuchFieldException {
        confirmationOfFunds_more_than_necessary_available(100, BigDecimal.ZERO);
        confirmationOfFunds_more_than_necessary_available(101, BigDecimal.ZERO);
    }

    @Test
    void confirmationOfFunds_more_than_necessary_available_credit_enabled() throws NoSuchFieldException {
        confirmationOfFunds_more_than_necessary_available(90, BigDecimal.valueOf(10));
        confirmationOfFunds_more_than_necessary_available(91, BigDecimal.valueOf(10));
    }

    @Test
    void confirmationOfFunds_Failure_credit_enabled() {
        when(depositAccountRepository.findByIbanAndCurrency(any(), any())).thenReturn(Optional.of(getDepositAccount(false, BigDecimal.TEN)));
        when(accountStmtService.readStmt(any(), any())).thenReturn(newAccountStmtBO(50));
        when(ledgerService.findLedgerByName(any())).thenReturn(Optional.of(getLedger()));

        ReflectionTestUtils.setField(depositAccountService, "exchangeRatesService", new CurrencyExchangeRatesServiceImpl(null, null));

        FundsConfirmationRequestBO requestBO = readFile(FundsConfirmationRequestBO.class, "FundsConfirmationRequest.yml");
        // Then
        boolean result = depositAccountService.confirmationOfFunds(requestBO);
        assertFalse(result);
    }

    @Test
    void confirmationOfFunds_Failure() {
        FundsConfirmationRequestBO requestBO = readFile(FundsConfirmationRequestBO.class, "FundsConfirmationRequest.yml");
        // Then
        assertThrows(DepositModuleException.class, () -> depositAccountService.confirmationOfFunds(requestBO));
    }

    @Test
    void getAccountsByIbanAndParamCurrency() {
        when(depositAccountRepository.findAllByIbanAndCurrencyContaining(anyString(), anyString())).thenReturn(Collections.singletonList(getDepositAccount(false, BigDecimal.ZERO)));
        List<DepositAccountBO> result = depositAccountService.getAccountsByIbanAndParamCurrency("iban", "EUR");
        assertEquals(Collections.singletonList(getDepositAccountBO()), result);
    }

    @Test
    void getAccountByIbanAndCurrency() {
        // Given
        when(depositAccountRepository.findByIbanAndCurrency(anyString(), anyString())).thenReturn(Optional.of(getDepositAccount(false, BigDecimal.ZERO)));
        // When
        DepositAccountBO result = depositAccountService.getAccountByIbanAndCurrency("iban", EUR);

        // Then
        assertEquals(getDepositAccountBO(), result);
    }

    @Test
    void getAccountByIbanAndCurrency_not_found() {
        // Given
        when(depositAccountRepository.findByIbanAndCurrency(anyString(), anyString())).thenReturn(Optional.empty());

        // Then
        assertThrows(DepositModuleException.class, () -> depositAccountService.getAccountByIbanAndCurrency("iban", EUR));
    }

    @Test
    void getAccountById() {
        // Given
        when(depositAccountRepository.findById(anyString())).thenReturn(Optional.of(getDepositAccount(false, BigDecimal.ZERO)));
        // When
        DepositAccountBO result = depositAccountService.getAccountById("accountId");

        // Then
        assertEquals(getDepositAccountBO(), result);
    }

    @Test
    void getAccountById_not_found() {
        // Given
        when(depositAccountRepository.findById(anyString())).thenReturn(Optional.empty());

        // Then
        assertThrows(DepositModuleException.class, () -> depositAccountService.getAccountById("accountId"));
    }

    @Test
    void getTransactionsByDatesPaged() throws JsonProcessingException {
        // Given
        when(depositAccountRepository.findById(anyString())).thenReturn(Optional.of(getDepositAccount(false, null)));
        when(postingService.findPostingsByDatesPaged(any(), any(), any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(newPostingLineBO())));
        when(transactionDetailsMapper.toTransactionSigned(any())).thenReturn(readFile(TransactionDetailsBO.class, "Transaction.yml"));
        // When
        Page<TransactionDetailsBO> result = depositAccountService.getTransactionsByDatesPaged("accountId", NOW, NOW, Pageable.unpaged());

        // Then
        assertEquals(new PageImpl<>(Collections.singletonList(readFile(TransactionDetailsBO.class, "Transaction.yml"))), result);
    }

    @Test
    void findDetailsByBranchPaged() {
        // Given
        when(depositAccountRepository.findByBranchAndIbanContaining(anyString(), anyString(), any())).thenReturn(new PageImpl<>(Collections.singletonList(getDepositAccount(false, null))));
        // When
        Page<DepositAccountDetailsBO> result = depositAccountService.findDetailsByBranchPaged("branchId", "someParam", false, Pageable.unpaged());

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
    }

    @Test
    void readIbanById() {
        // Given
        DepositAccount account = new DepositAccount();
        account.setIban("DE123456789");
        when(depositAccountRepository.findById(anyString())).thenReturn(Optional.of(account));

        // When
        String result = depositAccountService.readIbanById(ACCOUNT_ID);

        // Then
        assertEquals("DE123456789", result);
    }

    private void confirmationOfFunds_more_than_necessary_available(long amount, BigDecimal creditAmount) {
        when(depositAccountRepository.findByIbanAndCurrency(any(), any())).thenReturn(Optional.of(getDepositAccount(false, creditAmount)));
        when(accountStmtService.readStmt(any(), any())).thenReturn(newAccountStmtBO(amount));
        when(ledgerService.findLedgerByName(any())).thenReturn(Optional.of(getLedger()));

        ReflectionTestUtils.setField(depositAccountService, "exchangeRatesService", new CurrencyExchangeRatesServiceImpl(null, null));

        boolean response = depositAccountService.confirmationOfFunds(readFile(FundsConfirmationRequestBO.class, "FundsConfirmationRequest.yml"));
        assertTrue(response);
    }

    private PostingLineBO newPostingLineBO() throws JsonProcessingException {
        PostingLineBO pl = new PostingLineBO();
        pl.setAccount(new LedgerAccountBO());
        pl.setDetails(STATIC_MAPPER.writeValueAsString(new TransactionDetailsBO()));
        return pl;
    }

    private DepositAccount getDepositAccount(boolean status, BigDecimal creditAmount) {
        return new DepositAccount("id", "iban", "msisdn", "EUR",
                                  "name", "displayName", "product", null, AccountType.CASH, "bic", "linked",
                                  AccountUsage.PRIV, "details", status, false, CREATED, creditAmount);
    }

    private DepositAccountBO getDepositAccountBO() {
        return DepositAccountBO.builder().id("id")
                       .iban("iban").msisdn("msisdn")
                       .currency(EUR)
                       .name("name").product("product")
                       .accountType(AccountTypeBO.CASH)
                       .bic("bic")
                       .usageType(AccountUsageBO.PRIV).details("details")
                       .linkedAccounts("linked")
                       .displayName("displayName")
                       .created(CREATED)
                       .creditLimit(BigDecimal.ZERO)
                       .build();
    }

    private LedgerAccountBO getLedgerAccountBO() {
        LedgerAccountBO bo = new LedgerAccountBO();
        bo.setId("id");
        bo.setName("name");

        return bo;
    }

    private static LedgerBO getLedger() {
        LedgerBO ledgerBO = new LedgerBO();
        ledgerBO.setName("ledger");
        return ledgerBO;
    }

    private AccountStmtBO newAccountStmtBO(long amount) {
        AccountStmtBO result = new AccountStmtBO();
        LedgerAccountBO ledgerAccountBO2 = readFile(LedgerAccountBO.class, "LedgerAccount.yml");
        result.setAccount(ledgerAccountBO2);
        result.setPstTime(NOW);
        result.setTotalCredit(BigDecimal.valueOf(amount));
        return result;
    }

    private <T> T readFile(Class<T> t, String file) {
        try {
            return YamlReader.getInstance().getObjectFromResource(DepositAccountPaymentServiceImpl.class, file, t);
        } catch (IOException e) {
            e.printStackTrace(); //NOPMD
            throw new IllegalStateException("Resource file not found", e);
        }
    }

    @Test
    void changeAccountsBlockedStatus_system_block() {
        depositAccountService.changeAccountsBlockedStatus(USER_ID, true, true);
        verify(depositAccountRepository, times(1)).updateSystemBlockedStatus(USER_ID, true);
    }

    @Test
    void changeAccountsBlockedStatus_regular_block() {
        depositAccountService.changeAccountsBlockedStatus(USER_ID, false, true);
        verify(depositAccountRepository, times(1)).updateBlockedStatus(USER_ID, true);
    }

    @Test
    void changeAccountsBlockedStatus_list_system_block() {
        // When
        depositAccountService.changeAccountsBlockedStatus(Collections.singleton(USER_ID), true, true);

        // Then
        verify(depositAccountRepository, times(1)).updateSystemBlockedStatus(Collections.singleton(USER_ID), true);
    }

    @Test
    void changeAccountsBlockedStatus_list_regular_block() {
        // When
        depositAccountService.changeAccountsBlockedStatus(Collections.singleton(USER_ID), false, true);

        // Then
        verify(depositAccountRepository, times(1)).updateBlockedStatus(Collections.singleton(USER_ID), true);
    }

    @Test
    void changeCreditLimit() {
        DepositAccount depositAccount = getDepositAccount(true, BigDecimal.TEN);
        when(depositAccountRepository.findById(any())).thenReturn(Optional.of(depositAccount));
        depositAccountService.changeCreditLimit(ACCOUNT_ID, BigDecimal.ONE);
        assertEquals(0, depositAccount.getCreditLimit().compareTo(BigDecimal.ONE));
    }

}
