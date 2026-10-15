package com.universalshield.engine.input;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Metadata about a payment the user is about to make or was asked to make.
 * Never includes card numbers, CVV, PIN or OTP; the service masks them if pasted.
 *
 * @param amount               payment amount
 * @param currency             ISO-4217-like code, informational
 * @param recipient            recipient label (name, UPI handle, account nickname)
 * @param recipientIsNew       user says they have never paid this recipient before
 * @param paymentMethod        BANK_TRANSFER, UPI, CARD, WALLET, GIFT_CARD, CRYPTO, CASH, OTHER
 * @param attemptsLastHour     how many payment attempts to this recipient in the last hour
 * @param requestedByOtherParty the other party asked/pressured the user to pay
 * @param description          free-text context (what the payment is for, what was said)
 */
public record TransactionInput(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @DecimalMax(value = "1000000000", message = "Amount is too large") BigDecimal amount,
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter code such as INR or USD") String currency,
        @Size(max = 120, message = "Recipient must be at most 120 characters") String recipient,
        Boolean recipientIsNew,
        @Pattern(regexp = "BANK_TRANSFER|UPI|CARD|WALLET|GIFT_CARD|CRYPTO|CASH|OTHER",
                message = "Unsupported payment method") String paymentMethod,
        @Min(value = 0, message = "Attempts cannot be negative")
        @Max(value = 100, message = "Attempts must be at most 100") Integer attemptsLastHour,
        Boolean requestedByOtherParty,
        @Size(max = 2000, message = "Description must be at most 2000 characters") String description) {
}
