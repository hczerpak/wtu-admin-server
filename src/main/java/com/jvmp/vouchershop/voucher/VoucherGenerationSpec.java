package com.jvmp.vouchershop.voucher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Wither
public class VoucherGenerationSpec implements Serializable {

    /**
     * Number of vouchers to generate
     */
    int count;

    /**
     * Total amount of money to use during voucher generation in Wallet's currency and in smallest undividable units, i.e. without decimals. For BTC total
     * amount would be in satoshis.
     */
    long totalAmount;

    /**
     * Wallet to take the money from to cover these vouchers.
     */
    long walletId;

    /**
     * price of a single voucher in "singlePriceCurrency"
     */
    long singlePrice;

    /**
     * Target currency in which vouchers will be sold. Eg. GBP
     */
    String singlePriceCurrency;
}
