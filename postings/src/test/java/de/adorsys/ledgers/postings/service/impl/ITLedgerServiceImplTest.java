package de.adorsys.ledgers.postings.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

import java.util.List;
import java.util.Optional;

import de.adorsys.ledgers.postings.domain.Ledger;
import de.adorsys.ledgers.postings.repository.ChartOfAccountRepository;
import de.adorsys.ledgers.postings.repository.LedgerRepository;
import de.adorsys.ledgers.postings.utils.Ids;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import de.adorsys.ledgers.postings.domain.ChartOfAccount;
import de.adorsys.ledgers.postings.domain.LedgerAccountType;
import de.adorsys.ledgers.postings.repository.LedgerAccountTypeRepository;
import de.adorsys.ledgers.postings.service.ChartOfAccountService;
import de.adorsys.ledgers.tests.PostingsApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes=PostingsApplication.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class,DbUnitTestExecutionListener.class})
@DatabaseSetup("ITLedgerServiceImplTest-db-entries.xml")
@DatabaseTearDown(value={"ITLedgerServiceImplTest-db-entries.xml"}, type=DatabaseOperation.DELETE_ALL)

public class ITLedgerServiceImplTest {



    @Autowired
    private ChartOfAccountRepository chartOfAccountRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Test
    public void test_new_ledger() {
        // @TODO implement
        /*ChartOfAccount coa = chartOfAccountRepository.findById("ci8k8PDcTrCsi-F3sT3i-g").orElse(null);
        Ledger ledger = Ledger.builder().id(Ids.id()).name("Sample Ledger-2").user("Sample User").coa(coa).build();
        ledgerRepository.save(ledger);
        */
    }

    @Test
    public void test_find_ledger_by_id() {
        // @TODO implement
    }
    @Test
    public void test_find_ledger_by_name() {
        // @TODO implement
    }
    @Test
    public void test_new_ledger_accounts() {
        // @TODO implement
    }

    @Test
    public void test_find_ledger_account_by_id() {
        // @TODO implement
    }
    @Test
    public void test_find_ledger_account() {
        // @TODO implement
    }
    @Test
    public void test_find_n_ledger_accounts() {
        // @TODO implement
    }
}
