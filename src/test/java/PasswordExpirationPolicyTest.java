import demo.security.password.PasswordExpirationPolicy;
import demo.security.password.PasswordExpirationPolicy.Status;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordExpirationPolicyTest {

    private static final Instant NOW = Instant.parse("2026-05-19T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private PasswordExpirationPolicy policy() {
        return new PasswordExpirationPolicy.Builder()
                .maxAge(Duration.ofDays(90))
                .gracePeriod(Duration.ofDays(7))
                .clock(FIXED_CLOCK)
                .build();
    }

    @Test
    public void evaluate_freshPassword_isActive() {
        Instant changed = NOW.minus(Duration.ofDays(10));
        assertEquals(Status.ACTIVE, policy().evaluate(changed));
    }

    @Test
    public void evaluate_atMaxAge_entersGracePeriod() {
        Instant changed = NOW.minus(Duration.ofDays(90));
        assertEquals(Status.GRACE_PERIOD, policy().evaluate(changed));
    }

    @Test
    public void evaluate_pastGracePeriod_isExpired() {
        Instant changed = NOW.minus(Duration.ofDays(98));
        assertEquals(Status.EXPIRED, policy().evaluate(changed));
    }

    @Test
    public void evaluate_nullLastChangedAt_throws() {
        assertThrows(NullPointerException.class, () -> policy().evaluate(null));
    }

    @Test
    public void timeRemaining_activePassword_isPositive() {
        Duration remaining = policy().timeRemaining(NOW.minus(Duration.ofDays(30)));
        assertEquals(Duration.ofDays(60), remaining);
    }

    @Test
    public void timeRemaining_expiredPassword_isZero() {
        Duration remaining = policy().timeRemaining(NOW.minus(Duration.ofDays(200)));
        assertEquals(Duration.ZERO, remaining);
    }

    @Test
    public void defaultPolicy_buildsWithSystemClock() {
        PasswordExpirationPolicy defaultPolicy = PasswordExpirationPolicy.defaultPolicy();
        Status status = defaultPolicy.evaluate(Instant.now().minus(Duration.ofDays(1)));
        assertEquals(Status.ACTIVE, status);
    }

    @Test
    public void builder_zeroMaxAge_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordExpirationPolicy.Builder().maxAge(Duration.ZERO).build());
    }

    @Test
    public void builder_negativeMaxAge_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordExpirationPolicy.Builder().maxAge(Duration.ofDays(-1)).build());
    }

    @Test
    public void builder_negativeGracePeriod_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordExpirationPolicy.Builder().gracePeriod(Duration.ofDays(-1)).build());
    }

    @Test
    public void builder_zeroGracePeriod_isAllowed() {
        PasswordExpirationPolicy strict = new PasswordExpirationPolicy.Builder()
                .gracePeriod(Duration.ZERO)
                .clock(FIXED_CLOCK)
                .build();
        assertTrue(strict.evaluate(NOW.minus(Duration.ofDays(91))) == Status.EXPIRED);
    }
}
