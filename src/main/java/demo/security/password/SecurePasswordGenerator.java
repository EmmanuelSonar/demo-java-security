package demo.security.password;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SecurePasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>/?";

    private final SecureRandom random;
    private final PasswordPolicy policy;

    public SecurePasswordGenerator(PasswordPolicy policy) {
        this(policy, new SecureRandom());
    }

    SecurePasswordGenerator(PasswordPolicy policy, SecureRandom random) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.policy = policy;
        this.random = random;
    }

    public char[] generate(int length) {
        if (length < policy.getMinLength()) {
            throw new IllegalArgumentException("Length below policy minimum");
        }
        if (length > policy.getMaxLength()) {
            throw new IllegalArgumentException("Length above policy maximum");
        }

        String charset = buildCharset();
        if (charset.isEmpty()) {
            throw new IllegalStateException("Policy disables all character classes");
        }

        List<Character> chars = new ArrayList<>(length);

        if (policy.isRequireUppercase()) {
            chars.add(pickFrom(UPPERCASE));
        }
        if (policy.isRequireLowercase()) {
            chars.add(pickFrom(LOWERCASE));
        }
        if (policy.isRequireDigit()) {
            chars.add(pickFrom(DIGITS));
        }
        if (policy.isRequireSymbol()) {
            chars.add(pickFrom(SYMBOLS));
        }

        while (chars.size() < length) {
            chars.add(pickFrom(charset));
        }

        shuffle(chars);

        char[] out = new char[chars.size()];
        for (int i = 0; i < chars.size(); i++) {
            out[i] = chars.get(i);
        }
        return out;
    }

    private String buildCharset() {
        StringBuilder sb = new StringBuilder();
        if (policy.isRequireUppercase()) sb.append(UPPERCASE);
        if (policy.isRequireLowercase()) sb.append(LOWERCASE);
        if (policy.isRequireDigit()) sb.append(DIGITS);
        if (policy.isRequireSymbol()) sb.append(SYMBOLS);
        if (sb.isEmpty()) {
            sb.append(UPPERCASE).append(LOWERCASE).append(DIGITS);
        }
        return sb.toString();
    }

    private char pickFrom(String source) {
        return source.charAt(random.nextInt(source.length()));
    }

    private void shuffle(List<Character> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}
