package ru.hd.util;

import java.util.regex.Pattern;

public class ValidationPattern {

    public static final Pattern PHONE = Pattern.compile("^\\+7\\d{10}$");
    public static final Pattern INN = Pattern.compile("^\\d{12}$");
    public static final Pattern BIK = Pattern.compile("^04\\d{7}$");
    public static final Pattern ACCOUNT_NUMBER= Pattern.compile("\\d{20}");
}
