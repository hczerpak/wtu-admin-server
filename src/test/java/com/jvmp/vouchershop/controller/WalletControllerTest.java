package com.jvmp.vouchershop.controller;

import com.jvmp.vouchershop.Application;
import org.bitcoinj.core.Coin;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.Wallet;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class WalletControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private WalletAppKit walletAppKit;

    @Before
    public void setUp() {
        Wallet walletMock = mock(Wallet.class);
        when(walletMock.getBalance()).thenReturn(Coin.COIN);
        when(walletAppKit.wallet()).thenReturn(walletMock);
    }

    @WithMockUser(ControllerUtils.USER_NAME)
    @Test
    public void getAllWallets() throws Exception {
        mvc.perform(get("/wallets"))
                .andExpect(status().isOk());
    }

    @WithMockUser(ControllerUtils.USER_NAME)
    @Test
    public void generateWallet() throws Exception {
        mvc.perform(post("/wallets/new", "BTC"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.address").isNotEmpty())
                .andExpect(jsonPath("$.balance").isNumber())
                .andExpect(jsonPath("$.currency").value("BTC"))
                .andExpect(jsonPath("$.createdAt").value(Matchers.greaterThan(1322697600)))
                .andExpect(jsonPath("$.mnemonic").isNotEmpty());

        mvc.perform(post("/wallets/new", "ETH"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/wallets/new", "XYZ"))
                .andExpect(status().isBadRequest());
    }
}