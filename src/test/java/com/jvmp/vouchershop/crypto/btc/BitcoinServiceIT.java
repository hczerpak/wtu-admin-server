package com.jvmp.vouchershop.crypto.btc;

import com.jvmp.vouchershop.Application;
import com.jvmp.vouchershop.notifications.NotificationService;
import com.jvmp.vouchershop.repository.WalletRepository;
import com.jvmp.vouchershop.wallet.Wallet;
import org.bitcoinj.core.Context;
import org.bitcoinj.params.UnitTestParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@ActiveProfiles("it")
public class BitcoinServiceIT {

    @Autowired
    private WalletRepository walletRepository;

    @MockBean
    private BitcoinJAdapter bitcoinJAdapter;

    @MockBean
    private NotificationService notificationService;

    private BitcoinService bitcoinService;

    @Before
    public void setUp() {
        Context btcContext = new Context(UnitTestParams.get());
        bitcoinService = new BitcoinService(walletRepository, btcContext.getParams(), bitcoinJAdapter,
                notificationService);
    }

    @Test
    public void generateWallet() {
        Wallet savedWallet = bitcoinService.generateWallet();
        assertNotNull(savedWallet.getId());
    }
}