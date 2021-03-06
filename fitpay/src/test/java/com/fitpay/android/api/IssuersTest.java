package com.fitpay.android.api;

import com.fitpay.android.Steps;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IssuersTest {

    private static Steps steps = null;

    @BeforeClass
    public static void init() {
        steps = new Steps(IssuersTest.class);
    }

    @Test
    public void test01_getIssuers() throws InterruptedException {
        steps.getIssuers();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        steps.destroy();
        steps = null;
    }
}