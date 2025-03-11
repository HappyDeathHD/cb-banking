package testutil;

import java.util.concurrent.ThreadLocalRandom;

public class TestDataGenerator {

    public static String generateUniquePhone() {
        return "+7" + String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L));
    }

    public static String generateUniqueINN() {
        return String.format("%012d", ThreadLocalRandom.current().nextLong(1_000_000_000_000L));
    }

    public static String generateAccountNumber() {
        return String.format("%020d", ThreadLocalRandom.current().nextLong(1_000_000_000_000L));
    }

    public static String generateBIK() {
        return "04" + String.format("%07d", ThreadLocalRandom.current().nextInt(1_000_0000));
    }
}