package com.cloudflow.cloudflow.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

// No Spring annotations — this is a pure unit test.
// Runs in milliseconds. No application context loaded.
class CronValidatorTest {

    private CronValidator cronValidator;

    @BeforeEach
    void setUp() {
        // Instantiate directly — no @Autowired needed in unit tests
        cronValidator = new CronValidator();
    }

    @Test
    @DisplayName("Should return true for valid 6-field Quartz cron expression")
    void isValid_validExpression_returnsTrue() {
        assertThat(cronValidator.isValid("0 * * * * ?")).isTrue();
        assertThat(cronValidator.isValid("0 0 23 * * ?")).isTrue();
        assertThat(cronValidator.isValid("0 0 9 ? * MON")).isTrue();
        assertThat(cronValidator.isValid("0 0 0 1 * ?")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-cron",
            "* * * * *",          // 5-field (Unix format, not Quartz)
            "0 0 25 * * ?",       // invalid hour (25)
            "",
            "   "
    })
    @DisplayName("Should return false for invalid cron expressions")
    void isValid_invalidExpression_returnsFalse(String expression) {
        assertThat(cronValidator.isValid(expression)).isFalse();
    }

    @Test
    @DisplayName("validateOrThrow should throw IllegalArgumentException for invalid cron")
    void validateOrThrow_invalidExpression_throwsException() {
        assertThatThrownBy(() -> cronValidator.validateOrThrow("not-a-cron"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cron expression");
    }

    @Test
    @DisplayName("validateOrThrow should not throw for valid cron")
    void validateOrThrow_validExpression_doesNotThrow() {
        assertThatNoException().isThrownBy(
                () -> cronValidator.validateOrThrow("0 0 23 * * ?")
        );
    }
}