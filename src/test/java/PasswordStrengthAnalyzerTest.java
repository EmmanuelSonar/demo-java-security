import demo.security.password.PasswordPolicy;
import demo.security.password.PasswordStrengthAnalyzer;
import demo.security.password.PasswordStrengthAnalyzer.Result;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordStrengthAnalyzerTest {

    private final PasswordStrengthAnalyzer analyzer =
            new PasswordStrengthAnalyzer(PasswordPolicy.defaultPolicy());

    @Test
    public void analyze_strongCompliantPassword_isCompliant() {
        Result result = analyzer.analyze("T7r!vGq2#pLxZ".toCharArray());
        assertTrue(result.isCompliant(), "violations: " + result.getViolations());
    }

    @Test
    public void analyze_commonPasswordAsSubstring_isRejected() {
        // Satisfies length/upper/lower/digit/symbol/distinct rules, but embeds "password".
        Result result = analyzer.analyze("Password123!@".toCharArray());
        assertFalse(result.isCompliant());
        assertTrue(result.getViolations().contains("Matches a commonly used password"),
                "violations: " + result.getViolations());
    }

    @Test
    public void analyze_commonSubstringDisabled_allowsEmbeddedCommonWord() {
        PasswordPolicy lenient = new PasswordPolicy.Builder()
                .disallowCommonSubstrings(false)
                .build();
        PasswordStrengthAnalyzer lenientAnalyzer = new PasswordStrengthAnalyzer(lenient);
        Result result = lenientAnalyzer.analyze("Password123!@".toCharArray());
        assertFalse(result.getViolations().contains("Matches a commonly used password"),
                "violations: " + result.getViolations());
    }

    @Test
    public void analyze_passwordContainsUsername_isRejected() {
        Set<String> context = new HashSet<>();
        context.add("alice");
        Result result = analyzer.analyze("aLiCe9#Kq2!tR".toCharArray(), context);
        assertFalse(result.isCompliant());
        assertTrue(result.getViolations().contains("Contains personal or account information"),
                "violations: " + result.getViolations());
    }

    @Test
    public void analyze_shortContextTermBelowMinLength_isIgnored() {
        // "ann" is only 3 chars, below the default minContextTermLength of 4.
        Set<String> context = new HashSet<>();
        context.add("ann");
        Result result = analyzer.analyze("aNnG7#Kq2!tRzX".toCharArray(), context);
        assertFalse(result.getViolations().contains("Contains personal or account information"),
                "violations: " + result.getViolations());
    }

    @Test
    public void analyze_emptyContext_doesNotFlagContext() {
        Result result = analyzer.analyze("T7r!vGq2#pLxZ".toCharArray(), Collections.<String>emptySet());
        assertFalse(result.getViolations().contains("Contains personal or account information"),
                "violations: " + result.getViolations());
    }

    @Test
    public void analyze_nullContext_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.analyze("T7r!vGq2#pLxZ".toCharArray(), null));
    }
}
