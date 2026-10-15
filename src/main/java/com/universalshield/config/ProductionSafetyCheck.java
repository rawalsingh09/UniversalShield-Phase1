package com.universalshield.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Refuses to start in production with the development hash secret or a weak one. */
@Component
@Profile("prod")
public class ProductionSafetyCheck {

    static final int MIN_SECRET_LENGTH = 32;

    private final AppProperties props;

    public ProductionSafetyCheck(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void verify() {
        String secret = props.security().hashSecret();
        if (secret == null || secret.length() < MIN_SECRET_LENGTH || secret.contains("change-me")) {
            throw new IllegalStateException("APP_HASH_SECRET must be set to a random value of at least "
                    + MIN_SECRET_LENGTH + " characters in production.");
        }
    }
}
