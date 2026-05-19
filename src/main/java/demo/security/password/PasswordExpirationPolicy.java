package demo.security.password;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class PasswordExpirationPolicy {

    public enum Status { ACTIVE, GRACE_PERIOD, EXPIRED }

    public static final Duration DEFAULT_MAX_AGE = Duration.ofDays(90);
    public static final Duration DEFAULT_GRACE_PERIOD = Duration.ofDays(7);

    private final Duration maxAge;
    private final Duration gracePeriod;
    private final Clock clock;

    private PasswordExpirationPolicy(Builder b) {
        this.maxAge = b.maxAge;
        this.gracePeriod = b.gracePeriod;
        this.clock = b.clock;
    }

    public Status evaluate(Instant lastChangedAt) {
        Objects.requireNonNull(lastChangedAt, "lastChangedAt must not be null");
        Duration age = Duration.between(lastChangedAt, clock.instant());
        if (age.compareTo(maxAge) < 0) {
            return Status.ACTIVE;
        }
        if (age.compareTo(maxAge.plus(gracePeriod)) < 0) {
            return Status.GRACE_PERIOD;
        }
        return Status.EXPIRED;
    }

    public Duration timeRemaining(Instant lastChangedAt) {
        Objects.requireNonNull(lastChangedAt, "lastChangedAt must not be null");
        Duration remaining = maxAge.minus(Duration.between(lastChangedAt, clock.instant()));
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public static PasswordExpirationPolicy defaultPolicy() {
        return new Builder().build();
    }

    public static final class Builder {
        private Duration maxAge = DEFAULT_MAX_AGE;
        private Duration gracePeriod = DEFAULT_GRACE_PERIOD;
        private Clock clock = Clock.systemUTC();

        public Builder maxAge(Duration v) { this.maxAge = v; return this; }
        public Builder gracePeriod(Duration v) { this.gracePeriod = v; return this; }
        public Builder clock(Clock v) { this.clock = v; return this; }

        public PasswordExpirationPolicy build() {
            Objects.requireNonNull(maxAge, "maxAge must not be null");
            Objects.requireNonNull(gracePeriod, "gracePeriod must not be null");
            Objects.requireNonNull(clock, "clock must not be null");
            if (maxAge.isZero() || maxAge.isNegative()) {
                throw new IllegalArgumentException("maxAge must be positive");
            }
            if (gracePeriod.isNegative()) {
                throw new IllegalArgumentException("gracePeriod must not be negative");
            }
            return new PasswordExpirationPolicy(this);
        }
    }
}
