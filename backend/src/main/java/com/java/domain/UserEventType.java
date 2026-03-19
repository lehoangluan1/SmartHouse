package com.java.domain;

public final class UserEventType {
    private UserEventType() {}

    public static final String USER_ADDED_TO_HOME = "USER_ADDED_TO_HOME";
    public static final String USER_UPDATED_IN_HOME = "USER_UPDATED_IN_HOME";
    public static final String USER_REMOVED_FROM_HOME = "USER_REMOVED_FROM_HOME";
    public static final String USER_PASSWORD_SET = "USER_PASSWORD_SET";
    public static final String HOME_PROFILE_ACTIVATED = "HOME_PROFILE_ACTIVATED";

    public static final String ADMIN_USER_UPDATED = "ADMIN_USER_UPDATED";
    public static final String ADMIN_USER_PASSWORD_RESET = "ADMIN_USER_PASSWORD_RESET";

    public static final String USER_PROVISIONED = "USER_PROVISIONED";
    public static final String USER_GOOGLE_LINKED = "USER_GOOGLE_LINKED";
}