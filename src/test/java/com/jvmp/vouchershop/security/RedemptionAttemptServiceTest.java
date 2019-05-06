package com.jvmp.vouchershop.security;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static com.jvmp.vouchershop.utils.RandomUtils.randomString;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RedemptionAttemptServiceTest {

    private RedemptionAttemptService subject;

    private int maxAttempts;

    @Before
    public void setUp() {
        maxAttempts = nextInt(10, 20);

        subject = new RedemptionAttemptService(nextInt(10, 20), "MILLISECONDS", maxAttempts);
    }

    @Test
    public void testBlockingAndReset() {
        String ip = randomString(); // random ip?
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);

        for (int i = 0; i < maxAttempts - 1; i++) {
            assertFalse(subject.isBlocked(ip));
            subject.failed(request);
        }

        assertFalse(subject.isBlocked(ip));

        subject.succeeded(request); // should reset counter

        for (int i = 0; i < maxAttempts; i++) {
            assertFalse(subject.isBlocked(ip));
            subject.failed(request);
        }

        assertTrue(subject.isBlocked(ip));
    }
}