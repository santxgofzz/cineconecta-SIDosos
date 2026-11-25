package co.edu.icesi.cineconecta.common.error;

/** Códigos de error compartidos por Mi lista y reproducción. */
public final class ErrorCodes {
    private ErrorCodes() {}

    public static final String ACCOUNT_REQUIRED = "ACCOUNT_REQUIRED";
    public static final String PROFILE_NOT_ALLOWED = "PROFILE_NOT_ALLOWED";
    public static final String PROFILE_INACTIVE = "PROFILE_INACTIVE";
    public static final String SUBSCRIPTION_REQUIRED = "SUBSCRIPTION_REQUIRED";
    public static final String CONTENT_NOT_FOUND = "CONTENT_NOT_FOUND";
    public static final String INVALID_CONTENT = "INVALID_CONTENT";
    public static final String CONTENT_CONFLICT = "CONTENT_CONFLICT";
    public static final String CONTENT_NOT_AVAILABLE = "CONTENT_NOT_AVAILABLE";
    public static final String AGE_RATING_EXCEEDED = "AGE_RATING_EXCEEDED";
    public static final String EPISODE_REQUIRED = "EPISODE_REQUIRED";
    public static final String EPISODE_NOT_FOUND = "EPISODE_NOT_FOUND";
    public static final String EPISODE_NOT_APPLICABLE = "EPISODE_NOT_APPLICABLE";
    public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";
    public static final String SESSION_CLOSED = "SESSION_CLOSED";
    public static final String SESSION_CONFLICT = "SESSION_CONFLICT";
    public static final String INVALID_EVENT = "INVALID_EVENT";
    public static final String ORACLE_UNAVAILABLE = "ORACLE_UNAVAILABLE";
    public static final String MONGO_UNAVAILABLE = "MONGO_UNAVAILABLE";
}
