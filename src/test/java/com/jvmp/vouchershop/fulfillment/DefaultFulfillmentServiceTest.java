package com.jvmp.vouchershop.fulfillment;

import com.jvmp.vouchershop.email.EmailService;
import com.jvmp.vouchershop.exception.IllegalOperationException;
import com.jvmp.vouchershop.exception.ResourceNotFoundException;
import com.jvmp.vouchershop.repository.FulfillmentRepository;
import com.jvmp.vouchershop.repository.VoucherRepository;
import com.jvmp.vouchershop.shopify.ShopifyService;
import com.jvmp.vouchershop.shopify.domain.Customer;
import com.jvmp.vouchershop.shopify.domain.FinancialStatus;
import com.jvmp.vouchershop.shopify.domain.LineItem;
import com.jvmp.vouchershop.shopify.domain.Order;
import com.jvmp.vouchershop.voucher.Voucher;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static com.jvmp.Collections.asSet;
import static com.jvmp.vouchershop.utils.RandomUtils.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultFulfillmentServiceTest {

    @Mock
    private FulfillmentRepository fulfillmentRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private ShopifyService shopifyService;

    @Mock
    private EmailService emailService;

    private DefaultFulfillmentService service;

    @Before
    public void setUp() {
        service = new DefaultFulfillmentService(fulfillmentRepository, voucherRepository, shopifyService, emailService);
    }

    @Test
    public void fulfillOrder() {
        long orderNumber = nextLong();
        long orderId = nextLong();
        String sku = randomSku();
        Voucher voucher = randomVoucher()
                .withAmount(100)
                .withSku(sku);
        String email = "test@email." + RandomStringUtils.randomAlphabetic(3);
        Order order = randomOrder()
                .withId(orderId)
                .withOrderNumber(orderNumber)
                .withFinancialStatus(FinancialStatus.paid)
                .withCustomer(new Customer().withEmail(email))
                .withLineItems(singletonList(
                        new LineItem()
                                .withSku(sku)
                                .withQuantity(1)
                ));

        when(voucherRepository.findBySoldFalseAndSku(eq(sku))).thenReturn(singletonList(voucher));

        Fulfillment fulfillment = new Fulfillment()
                .withVouchers(singleton(voucher))
                .withOrderId(orderNumber);

        Fulfillment savedFulfillment = fulfillment.withId(nextLong(1, Long.MAX_VALUE));

        when(fulfillmentRepository.save(eq(fulfillment))).thenReturn(savedFulfillment);

        service.fulfillOrder(order);

        verify(shopifyService, times(1)).markOrderFulfilled(eq(orderId));
        verify(emailService, times(1)).sendVouchers(eq(singleton(voucher)), eq(order));
        verify(fulfillmentRepository, times(1)).save(eq(fulfillment));
        verify(voucherRepository, times(1)).save(eq(
                voucher
                        .withSold(true)

                // TODO add expires at calculation
        ));
    }

    @Test
    public void completeFulfillment() {
        Set<Voucher> vouchers = asSet(
                randomVoucher()
                        .withSku(randomSku())
                        .withAmount(100),
                randomVoucher()
                        .withSku(randomSku())
                        .withAmount(100)
        );

        Fulfillment fulfillment = new Fulfillment()
                .withOrderId(nextLong(0, Long.MAX_VALUE))
                .withVouchers(vouchers);

        service.completeFulfillment(fulfillment);

        verify(fulfillmentRepository, times(1)).save(eq(fulfillment));
        fulfillment.getVouchers().forEach(voucher ->
                verify(voucherRepository, times(1)).save(eq(voucher.withSold(true))));
    }

    @Test(expected = InvalidOrderException.class)
    public void checkIfSupplyIsEnough() {
        service.checkIfSupplyIsEnough(
                singleton(ImmutablePair.of("sku", 2)), emptySet()
        );
    }


    @Test
    public void checkIfSupplyIsEnough_oneItem_shouldSucceed() {
        String sku = randomSku();
        service.checkIfSupplyIsEnough(
                singleton(ImmutablePair.of(sku, 2)),
                asSet(
                        randomVoucher().withSku(sku),
                        randomVoucher().withSku(sku),
                        randomVoucher().withSku(sku)
                )
        );
    }

    @Test
    public void findSupplyForDemand() {
        Set<ImmutablePair<String, Integer>> demand = singleton(ImmutablePair.of("sku", 2));

        service.findSupplyForDemand(demand);

        verify(voucherRepository, times(1)).findBySoldFalseAndSku(eq("sku"));
    }

    @Test
    public void findVouchersSkuAndQuantity() {
        String sku1 = randomString(), sku2 = randomString();
        Set<ImmutablePair<String, Integer>> skuAndQuantity = service.findVouchersSkuAndQuantity(randomOrder()
                .withLineItems(asList(
                        new LineItem().withSku(sku1).withQuantity(1),
                        new LineItem().withSku(sku2).withQuantity(2)
                )));

        assertEquals(new HashSet<>(asList(
                ImmutablePair.of(sku1, 1),
                ImmutablePair.of(sku2, 2)
        )), skuAndQuantity);
    }

    @Test(expected = IllegalOperationException.class)
    public void checkIfOrderIHasNotBeenFulFilledYet() {
        long orderId = nextLong();

        when(fulfillmentRepository.findByOrderId(eq(orderId))).thenReturn(new Fulfillment());

        service.checkIfOrderIHasNotBeenFulfilledYet(randomOrder().withId(orderId));
    }

    @Test(expected = InvalidOrderException.class)
    public void checkIfOrderIsValid() {
        service.checkIfOrderIsValid(randomOrder());
    }

    @Test(expected = InvalidOrderException.class)
    public void checkIfOrderHasBeenFullyPaid() {
        service.checkIfOrderHasBeenFullyPaid(randomOrder());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void findFulfillmentByUnknownOrderId() {
        when(fulfillmentRepository.findByOrderId(anyLong())).thenReturn(null);

        service.findByOrderId(1L);
    }
}