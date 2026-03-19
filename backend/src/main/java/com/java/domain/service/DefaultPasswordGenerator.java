package com.java.domain.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class DefaultPasswordGenerator implements PasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String SPECIAL = "@#$%";
    private static final String ALL = UPPER + LOWER + DIGIT + SPECIAL;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder();
        sb.append(randomChar(UPPER));
        sb.append(randomChar(LOWER));
        sb.append(randomChar(DIGIT));
        sb.append(randomChar(SPECIAL));

        while (sb.length() < 10) {
            sb.append(randomChar(ALL));
        }

        return shuffle(sb.toString());
    }

    private char randomChar(String source) {
        return source.charAt(random.nextInt(source.length()));
    }

    private String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}