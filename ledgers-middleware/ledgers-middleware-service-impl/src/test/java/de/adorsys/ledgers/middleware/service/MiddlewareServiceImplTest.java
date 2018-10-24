package de.adorsys.ledgers.middleware.service;

import de.adorsys.ledgers.deposit.api.domain.*;
import de.adorsys.ledgers.deposit.api.exception.DepositAccountNotFoundException;
import de.adorsys.ledgers.deposit.api.exception.PaymentNotFoundException;
import de.adorsys.ledgers.deposit.api.service.DepositAccountPaymentService;
import de.adorsys.ledgers.deposit.api.service.DepositAccountService;
import de.adorsys.ledgers.middleware.converter.AccountConverter;
import de.adorsys.ledgers.middleware.converter.PaymentConverter;
import de.adorsys.ledgers.middleware.service.domain.account.AccountDetailsTO;
import de.adorsys.ledgers.middleware.service.domain.account.AccountStatusTO;
import de.adorsys.ledgers.middleware.service.domain.account.AccountTypeTO;
import de.adorsys.ledgers.middleware.service.domain.account.UsageTypeTO;
import de.adorsys.ledgers.middleware.service.domain.payment.PaymentResultTO;
import de.adorsys.ledgers.middleware.service.domain.payment.TransactionStatusTO;
import de.adorsys.ledgers.middleware.service.exception.AccountNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.PaymentNotFoundMiddlewareException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pro.javatar.commons.reader.ResourceReader;
import pro.javatar.commons.reader.YamlReader;

import java.util.Collections;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MiddlewareServiceImplTest {
    private static final String PAYMENT_ID = "myPaymentId";
    private static final String ACCOUNT_ID = "id";

    @InjectMocks
    private MiddlewareServiceImpl middlewareService;

    @Mock
    private DepositAccountPaymentService paymentService;
    @Mock
    private PaymentConverter converter;
    @Mock
    private DepositAccountService accountService;
    @Mock
    private AccountConverter accountConverter;

    @SuppressWarnings("unchecked")
    @Test
    public void getPaymentStatusById() throws PaymentNotFoundMiddlewareException, PaymentNotFoundException {
        PaymentResultBO<TransactionStatusBO> paymentResultBO = mock(PaymentResultBO.class);
        PaymentResultTO<TransactionStatusTO> paymentResultTO = new PaymentResultTO<>(TransactionStatusTO.RJCT);

        when(paymentService.getPaymentStatusById(PAYMENT_ID)).thenReturn(paymentResultBO);
        when(converter.toPaymentResultTO(paymentResultBO)).thenReturn(paymentResultTO);

        PaymentResultTO<TransactionStatusTO> paymentResult = middlewareService.getPaymentStatusById(PAYMENT_ID);

        assertThat(paymentResult.getPaymentResult().getName(), is(TransactionStatusBO.RJCT.getName()));

        verify(paymentService, times(1)).getPaymentStatusById(PAYMENT_ID);
        verify(converter, times(1)).toPaymentResultTO(paymentResultBO);
    }

    @Test(expected = PaymentNotFoundMiddlewareException.class)
    public void getPaymentStatusByIdWithException() throws PaymentNotFoundMiddlewareException, PaymentNotFoundException {

        when(paymentService.getPaymentStatusById(PAYMENT_ID)).thenThrow(new PaymentNotFoundException());

        middlewareService.getPaymentStatusById(PAYMENT_ID);

        verify(paymentService, times(1)).getPaymentStatusById(PAYMENT_ID);
    }

    @Test
    public void getAccountDetailsByAccountId() throws DepositAccountNotFoundException, AccountNotFoundMiddlewareException {
        when(accountService.getDepositAccountById(any())).thenReturn(getAccount(DepositAccountBO.class));
        when(accountConverter.toAccountDetailsTO(any(), anyList())).thenReturn(getAccount(AccountDetailsTO.class));
        AccountDetailsTO details = middlewareService.getAccountDetailsByAccountId(ACCOUNT_ID);

        assertThat(details).isNotNull();
        verify(accountService, times(1)).getDepositAccountById(ACCOUNT_ID);
    }

    @Test(expected = AccountNotFoundMiddlewareException.class)
    public void getAccountDetailsByAccountId_wrong_id() throws AccountNotFoundMiddlewareException, DepositAccountNotFoundException {
        when(accountService.getDepositAccountById(any())).thenThrow(DepositAccountNotFoundException.class);
        AccountDetailsTO details = middlewareService.getAccountDetailsByAccountId("wrong id");

        verify(accountService, times(1)).getDepositAccountById(ACCOUNT_ID);
    }

    private static <T> T getAccount(Class<T> aClass) {
        return YamlReader.getInstance().getObjectFromFile("de/adorsys/ledgers/middleware/converter/AccountDetails.yml", aClass);
    }
}