package com.universalshield.security;

import com.universalshield.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSafetyCheckTest {

    private static void verify(String secret) throws Exception {
        var props = new AppProperties(new AppProperties.Risk(new AppProperties.Thresholds(30, 60, 85)),
                new AppProperties.Security(secret, List.of(), new AppProperties.RateLimit(true, 10, 30)),
                new AppProperties.Retention(90, 365, "0 0 3 * * *"), new AppProperties.Demo(false, "d@x.test", ""));
        var check = new com.universalshield.config.ProductionSafetyCheck(props);
        Method m = check.getClass().getDeclaredMethod("verify");
        m.setAccessible(true);
        try {
            m.invoke(check);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void refusesDefaultOrShortSecretInProduction() {
        assertThatThrownBy(() -> verify("change-me-dev-only-secret")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> verify("too-short")).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> verify("x9Vb7Qp2Lr8Tn4Wc6Zk1Ms3Hd5Jf0Ga8Ye2Uq")).doesNotThrowAnyException();
    }
}
