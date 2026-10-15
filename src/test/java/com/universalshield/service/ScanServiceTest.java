package com.universalshield.service;

import com.universalshield.dto.ScanRequest;
import com.universalshield.engine.ScanCategory;
import com.universalshield.engine.RiskEngine;
import com.universalshield.engine.input.CallInput;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.engine.input.UrlInput;
import com.universalshield.exception.BadRequestException;
import com.universalshield.exception.NotFoundException;
import com.universalshield.repository.ScanRepository;
import com.universalshield.security.SubjectHasher;
import com.universalshield.user.UserPreferenceRepository;
import com.universalshield.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanServiceTest {

    @Mock RiskEngine engine;
    @Mock ScanRepository scans;
    @Mock UserRepository users;
    @Mock UserPreferenceRepository prefs;
    @Mock ScanHistoryService history;
    @Mock SubjectHasher hasher;

    ScanService service;

    @BeforeEach
    void setUp() {
        service = new ScanService(engine, scans, users, prefs, history, new InputSanitizer(), hasher, new ScanMapper());
    }

    @Test
    void rejectsCategoryWithoutMatchingInput() {
        var req = new ScanRequest(ScanCategory.URL, new MessageInput("hello", null, null), null, null, null, null);
        assertThatThrownBy(() -> service.create(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("'url'");
        verify(engine, never()).evaluate(any());
    }

    @Test
    void anotherUsersScanIsNotFound() {
        UUID id = UUID.randomUUID();
        when(scans.findByPublicIdAndUserId(id, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(2L, id)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(2L, id)).isInstanceOf(NotFoundException.class);
        verify(scans, never()).delete(any(com.universalshield.domain.Scan.class));
    }

    @Test
    void listValidatesPagingDatesAndSearch() {
        assertThatThrownBy(() -> service.list(1L, null, null, null, null, null, -1, 10)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.list(1L, null, null, null, null, null, 0, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.list(1L, null, null, null, null, null, 0, 51)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.list(1L, null, null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, 0, 10))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.list(1L, null, null, null, null, "x".repeat(101), 0, 10)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void subjectsAreDerivedPerCategory() {
        assertThat(ScanService.subject(new ScanRequest(ScanCategory.URL, null, new UrlInput("https://a.b.example.com/x"), null, null, null)))
                .isEqualTo("domain:example.com");
        assertThat(ScanService.subject(new ScanRequest(ScanCategory.CALL, null, null, null,
                new CallInput("Bank", "+1 (555) 0100", "d", null), null))).isEqualTo("number:+15550100");
        assertThat(ScanService.subject(new ScanRequest(ScanCategory.MESSAGE, new MessageInput("t", null, null), null, null, null, null))).isNull();
    }
}
