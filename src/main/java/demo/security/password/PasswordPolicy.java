package demo.security.password;

public final class PasswordPolicy {

    public static final int DEFAULT_MIN_LENGTH = 12;
    public static final int DEFAULT_MAX_LENGTH = 128;

    private final int minLength;
    private final int maxLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSymbol;
    private final int minDistinctCharacters;
    private final int maxRepeatedCharacters;

    private PasswordPolicy(Builder b) {
        this.minLength = b.minLength;
        this.maxLength = b.maxLength;
        this.requireUppercase = b.requireUppercase;
        this.requireLowercase = b.requireLowercase;
        this.requireDigit = b.requireDigit;
        this.requireSymbol = b.requireSymbol;
        this.minDistinctCharacters = b.minDistinctCharacters;
        this.maxRepeatedCharacters = b.maxRepeatedCharacters;
    }

    public int getMinLength() { return minLength; }
    public int getMaxLength() { return maxLength; }
    public boolean isRequireUppercase() { return requireUppercase; }
    public boolean isRequireLowercase() { return requireLowercase; }
    public boolean isRequireDigit() { return requireDigit; }
    public boolean isRequireSymbol() { return requireSymbol; }
    public int getMinDistinctCharacters() { return minDistinctCharacters; }
    public int getMaxRepeatedCharacters() { return maxRepeatedCharacters; }

    public static PasswordPolicy defaultPolicy() {
        return new Builder().build();
    }

    public static final class Builder {
        private int minLength = DEFAULT_MIN_LENGTH;
        private int maxLength = DEFAULT_MAX_LENGTH;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSymbol = true;
        private int minDistinctCharacters = 6;
        private int maxRepeatedCharacters = 3;

        public Builder minLength(int v) { this.minLength = v; return this; }
        public Builder maxLength(int v) { this.maxLength = v; return this; }
        public Builder requireUppercase(boolean v) { this.requireUppercase = v; return this; }
        public Builder requireLowercase(boolean v) { this.requireLowercase = v; return this; }
        public Builder requireDigit(boolean v) { this.requireDigit = v; return this; }
        public Builder requireSymbol(boolean v) { this.requireSymbol = v; return this; }
        public Builder minDistinctCharacters(int v) { this.minDistinctCharacters = v; return this; }
        public Builder maxRepeatedCharacters(int v) { this.maxRepeatedCharacters = v; return this; }

        public PasswordPolicy build() {
            if (minLength < 1 || maxLength < minLength) {
                throw new IllegalArgumentException("Invalid length range");
            }
            return new PasswordPolicy(this);
        }
    }
}
