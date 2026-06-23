package demo.security.password;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PasswordStrengthAnalyzer {

    public enum Strength { VERY_WEAK, WEAK, FAIR, STRONG, VERY_STRONG }

    private static final Set<String> COMMON_PASSWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "letmein", "dragon", "111111", "iloveyou",
            "admin", "welcome", "login", "passw0rd", "starwars"
    )));

    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:'\",.<>/?\\|`~";

    private final PasswordPolicy policy;

    public PasswordStrengthAnalyzer(PasswordPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        this.policy = policy;
    }

    public Result analyze(char[] password) {
        return analyze(password, Collections.<String>emptySet());
    }

    /**
     * Analyzes a password, additionally rejecting it if it contains any
     * context-specific term (e.g. the user's username or email local-part).
     * Terms shorter than the policy's minimum context-term length are ignored
     * to avoid spurious matches on very short fragments.
     */
    public Result analyze(char[] password, Set<String> contextTerms) {
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        if (contextTerms == null) {
            throw new IllegalArgumentException("contextTerms must not be null");
        }

        List<String> violations = new ArrayList<>();
        CharStats stats = collectStats(password);

        if (password.length < policy.getMinLength()) {
            violations.add("Too short (min " + policy.getMinLength() + ")");
        }
        if (password.length > policy.getMaxLength()) {
            violations.add("Too long (max " + policy.getMaxLength() + ")");
        }
        if (policy.isRequireUppercase() && stats.uppercase == 0) {
            violations.add("Missing uppercase letter");
        }
        if (policy.isRequireLowercase() && stats.lowercase == 0) {
            violations.add("Missing lowercase letter");
        }
        if (policy.isRequireDigit() && stats.digits == 0) {
            violations.add("Missing digit");
        }
        if (policy.isRequireSymbol() && stats.symbols == 0) {
            violations.add("Missing symbol");
        }
        if (stats.distinct < policy.getMinDistinctCharacters()) {
            violations.add("Too few distinct characters (min " + policy.getMinDistinctCharacters() + ")");
        }
        if (stats.longestRun > policy.getMaxRepeatedCharacters()) {
            violations.add("Too many repeated characters in a row (max " + policy.getMaxRepeatedCharacters() + ")");
        }
        if (containsCommonPassword(password)) {
            violations.add("Matches a commonly used password");
        }
        if (containsContextTerm(password, contextTerms)) {
            violations.add("Contains personal or account information");
        }
        if (containsSequential(password)) {
            violations.add("Contains a sequential run (e.g. abcd, 1234)");
        }

        double entropy = computeEntropy(password, stats);
        Strength strength = scoreToStrength(entropy, violations.size());
        boolean compliant = violations.isEmpty();

        return new Result(strength, entropy, compliant, violations);
    }

    private CharStats collectStats(char[] password) {
        CharStats s = new CharStats();
        Set<Character> distinct = new HashSet<>();
        int currentRun = 1;
        char previous = 0;

        for (int i = 0; i < password.length; i++) {
            char c = password[i];
            distinct.add(c);
            if (Character.isUpperCase(c)) {
                s.uppercase++;
            } else if (Character.isLowerCase(c)) {
                s.lowercase++;
            } else if (Character.isDigit(c)) {
                s.digits++;
            } else if (SYMBOLS.indexOf(c) >= 0) {
                s.symbols++;
            } else {
                s.other++;
            }
            if (i > 0 && c == previous) {
                currentRun++;
                if (currentRun > s.longestRun) {
                    s.longestRun = currentRun;
                }
            } else {
                currentRun = 1;
            }
            previous = c;
        }
        if (s.longestRun == 0 && password.length > 0) {
            s.longestRun = 1;
        }
        s.distinct = distinct.size();
        return s;
    }

    private double computeEntropy(char[] password, CharStats stats) {
        int charsetSize = 0;
        if (stats.uppercase > 0) charsetSize += 26;
        if (stats.lowercase > 0) charsetSize += 26;
        if (stats.digits > 0) charsetSize += 10;
        if (stats.symbols > 0) charsetSize += SYMBOLS.length();
        if (stats.other > 0) charsetSize += 32;
        if (charsetSize == 0) {
            return 0.0;
        }
        return password.length * (Math.log(charsetSize) / Math.log(2));
    }

    private Strength scoreToStrength(double entropy, int violationCount) {
        if (violationCount >= 3 || entropy < 28) {
            return Strength.VERY_WEAK;
        }
        if (entropy < 36) {
            return Strength.WEAK;
        }
        if (entropy < 60) {
            return Strength.FAIR;
        }
        if (entropy < 128) {
            return Strength.STRONG;
        }
        return Strength.VERY_STRONG;
    }

    private boolean containsCommonPassword(char[] password) {
        String lower = new String(password).toLowerCase(Locale.ROOT);
        for (String common : COMMON_PASSWORDS) {
            if (policy.isDisallowCommonSubstrings() ? lower.contains(common) : lower.equals(common)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsContextTerm(char[] password, Set<String> contextTerms) {
        if (contextTerms.isEmpty()) {
            return false;
        }
        String lower = new String(password).toLowerCase(Locale.ROOT);
        for (String term : contextTerms) {
            if (term == null) {
                continue;
            }
            String normalized = term.toLowerCase(Locale.ROOT).trim();
            if (normalized.length() >= policy.getMinContextTermLength()
                    && lower.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSequential(char[] password) {
        int runLength = 1;
        for (int i = 1; i < password.length; i++) {
            if (password[i] - password[i - 1] == 1) {
                runLength++;
                if (runLength >= 4) {
                    return true;
                }
            } else {
                runLength = 1;
            }
        }
        return false;
    }

    private static final class CharStats {
        int uppercase;
        int lowercase;
        int digits;
        int symbols;
        int other;
        int distinct;
        int longestRun;
    }

    public static final class Result {
        private final Strength strength;
        private final double entropyBits;
        private final boolean compliant;
        private final List<String> violations;

        Result(Strength strength, double entropyBits, boolean compliant, List<String> violations) {
            this.strength = strength;
            this.entropyBits = entropyBits;
            this.compliant = compliant;
            this.violations = Collections.unmodifiableList(violations);
        }

        public Strength getStrength() { return strength; }
        public double getEntropyBits() { return entropyBits; }
        public boolean isCompliant() { return compliant; }
        public List<String> getViolations() { return violations; }
    }
}
