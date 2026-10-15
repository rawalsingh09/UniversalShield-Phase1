package com.universalshield.engine;

import com.universalshield.engine.input.AppPermissionInput;
import com.universalshield.engine.input.CallInput;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.engine.input.TransactionInput;
import com.universalshield.engine.input.UrlInput;

/**
 * Something the user wants screened. Exactly one input matching {@link #category()} is set.
 */
public record FraudEvent(ScanCategory category, MessageInput message, UrlInput url,
                         TransactionInput transaction, CallInput call, AppPermissionInput app,
                         ScanHistory history) {

    public FraudEvent {
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }
        history = history == null ? ScanHistory.EMPTY : history;
    }

    public static FraudEvent ofMessage(MessageInput in, ScanHistory h) {
        return new FraudEvent(ScanCategory.MESSAGE, in, null, null, null, null, h);
    }

    public static FraudEvent ofUrl(UrlInput in, ScanHistory h) {
        return new FraudEvent(ScanCategory.URL, null, in, null, null, null, h);
    }

    public static FraudEvent ofTransaction(TransactionInput in, ScanHistory h) {
        return new FraudEvent(ScanCategory.TRANSACTION, null, null, in, null, null, h);
    }

    public static FraudEvent ofCall(CallInput in, ScanHistory h) {
        return new FraudEvent(ScanCategory.CALL, null, null, null, in, null, h);
    }

    public static FraudEvent ofApp(AppPermissionInput in, ScanHistory h) {
        return new FraudEvent(ScanCategory.APP_PERMISSION, null, null, null, null, in, h);
    }

    /** Free text carried by the event, used by detectors that scan text for embedded links. */
    public String freeText() {
        return switch (category) {
            case MESSAGE -> message == null ? "" : message.text();
            case CALL -> call == null ? "" : call.description();
            case TRANSACTION -> transaction == null || transaction.description() == null
                    ? "" : transaction.description();
            default -> "";
        };
    }
}
