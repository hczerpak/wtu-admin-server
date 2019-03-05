package com.jvmp.vouchershop.voucher.impl;

import com.jvmp.vouchershop.exception.IllegalOperationException;
import com.jvmp.vouchershop.exception.ResourceNotFoundException;
import com.jvmp.vouchershop.repository.VoucherRepository;
import com.jvmp.vouchershop.voucher.Voucher;
import com.jvmp.vouchershop.voucher.VoucherNotFoundException;
import com.jvmp.vouchershop.wallet.Wallet;
import com.jvmp.vouchershop.wallet.WalletService;
import org.bitcoinj.core.Context;
import org.bitcoinj.params.UnitTestParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static com.jvmp.vouchershop.utils.RandomUtils.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultVoucherServiceTest {

    @Mock
    private WalletService walletService;

    @Mock
    private VoucherRepository voucherRepository;

    private DefaultVoucherService subject;

    @Before
    public void setUp() {
        Context btcContext = new Context(UnitTestParams.get());
        Context.propagate(btcContext);
        subject = new DefaultVoucherService(walletService, voucherRepository);
    }

    @Test(expected = IllegalOperationException.class)
    public void generateVouchersWithIndivisibleInput() {
        VoucherGenerationDetails spec = randomVoucherGenerationSpec()
                .withCount(10)
                .withTotalAmount(99);

        subject.generateVouchers(spec);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void generateVouchersButWalletDoesntExist() {
        VoucherGenerationDetails spec = randomVoucherGenerationSpec();

        subject.generateVouchers(spec);
    }

    @Test
    public void generateVouchers() {
        Wallet wallet = randomWallet().withId(1L);
        VoucherGenerationDetails spec = randomVoucherGenerationSpec().withWalletId(wallet.getId());
        when(walletService.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        List<Voucher> vouchers = subject.generateVouchers(spec);

        assertEquals(spec.getCount(), vouchers.size());
    }

    @Test(expected = VoucherNotFoundException.class)
    public void redeemVoucher_noVoucher() throws VoucherNotFoundException {
        String code = randomString();

        when(voucherRepository.findByCode(eq(code))).thenReturn(Optional.empty());

        subject.redeemVoucher(new RedemptionRequest(randomString(), code));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void redeemVoucher_noWallet() throws VoucherNotFoundException {
        Voucher voucher = randomVoucher()
                .withSold(true)
                .withPublished(true)
                .withRedeemed(false);
        String code = voucher.getCode();
        String destinationAddress = randomString();

        when(voucherRepository.findByCode(eq(code))).thenReturn(Optional.of(voucher));

        subject.redeemVoucher(new RedemptionRequest(destinationAddress, code));
    }

    @Test
    public void redeemVoucher_notEnoughMoney() throws Exception {
        Wallet wallet = randomWallet(UnitTestParams.get());
        Voucher voucher = randomVoucher()
                .withWalletId(wallet.getId())
                .withSold(true)
                .withPublished(true)
                .withRedeemed(false);
        String code = voucher.getCode();
        String destinationAddress = randomString();

        when(voucherRepository.findByCode(eq(code))).thenReturn(Optional.of(voucher));
        when(walletService.findById(eq(wallet.getId()))).thenReturn(Optional.of(wallet));
        when(walletService.sendMoney(eq(wallet), eq(destinationAddress), eq(voucher.getAmount()))).thenReturn(randomString());

        subject.redeemVoucher(new RedemptionRequest(destinationAddress, code));

        verify(walletService, times(1)).sendMoney(eq(wallet), eq(destinationAddress), eq(voucher.getAmount()));
        verify(voucherRepository, times(1)).save(eq(voucher.withRedeemed(true)));
    }

    @Test
    public void redeemVoucher_happyEnding() throws VoucherNotFoundException {
        Wallet wallet = randomWallet(UnitTestParams.get());
        Voucher voucher = randomVoucher()
                .withWalletId(wallet.getId())
                .withSold(true)
                .withPublished(true)
                .withRedeemed(false);
        String code = voucher.getCode();
        String destinationAddress = randomString();

        when(voucherRepository.findByCode(eq(code))).thenReturn(Optional.of(voucher));
        when(walletService.findById(eq(wallet.getId()))).thenReturn(Optional.of(wallet));
        when(walletService.sendMoney(eq(wallet), eq(destinationAddress), eq(voucher.getAmount()))).thenReturn(randomString());

        subject.redeemVoucher(new RedemptionRequest(destinationAddress, code));

        verify(walletService, times(1)).sendMoney(eq(wallet), eq(destinationAddress), eq(voucher.getAmount()));
        verify(voucherRepository, times(1)).save(eq(voucher.withRedeemed(true)));
    }

    @Test(expected = IllegalOperationException.class)
    public void checkVoucher_alreadyRedeemed() {
        DefaultVoucherService.checkVoucher(randomVoucher()
                .withPublished(true)
                .withSold(true)
                .withRedeemed(true)
        );
    }

    @Test(expected = IllegalOperationException.class)
    public void checkVoucher_notSoldYet() {
        DefaultVoucherService.checkVoucher(randomVoucher()
                .withPublished(true)
        );
    }

    @Test(expected = IllegalOperationException.class)
    public void checkVoucher_notPublished() {
        DefaultVoucherService.checkVoucher(randomVoucher());
    }
}