package com.universalshield.engine.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A message the user pasted for screening.
 *
 * @param text    the message body
 * @param channel SMS, EMAIL, CHAT or SOCIAL (optional, defaults to OTHER)
 * @param sender  the displayed sender name or address, if the user wants to include it
 */
public record MessageInput(
        @NotBlank(message = "Message text is required")
        @Size(max = 5000, message = "Message text must be at most 5000 characters") String text,
        @Pattern(regexp = "SMS|EMAIL|CHAT|SOCIAL|OTHER", message = "Channel must be SMS, EMAIL, CHAT, SOCIAL or OTHER")
        String channel,
        @Size(max = 120, message = "Sender must be at most 120 characters") String sender) {
}
