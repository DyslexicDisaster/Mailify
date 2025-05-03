package utils;

public class EmailUtils {
    // SERVICE LOCATION
    public static final String HOSTNAME = "localhost";
    public static final int PORT        = 12345;

    // JSON COMMANDS
    public static final String LOGIN           = "LOGIN";             // login request
    public static final String REGISTER        = "REGISTER";          // register request
    public static final String SEND            = "SEND";              // send email
    public static final String LIST_INBOX      = "LIST_INBOX";        // list received metadata
    public static final String SEARCH_INBOX    = "SEARCH_INBOX";      // search received
    public static final String LIST_SENT       = "LIST_SENT";         // list sent metadata
    public static final String SEARCH_SENT     = "SEARCH_SENT";       // search sent
    public static final String READ            = "READ";              // fetch content
    public static final String LOGOUT          = "LOGOUT";            // logout session
    public static final String EXIT            = "EXIT";              // terminate client

    // JSON RESPONSE STATUSES
    public static final String STATUS_OK                   = "OK";
    public static final String STATUS_LOGIN_SUCCESS        = "LOGIN_SUCCESS";
    public static final String STATUS_LOGIN_FAILURE        = "LOGIN_FAILURE";
    public static final String STATUS_REGISTERED           = "REGISTERED";
    public static final String STATUS_REGISTER_FAILURE     = "REGISTER_FAILURE";
    public static final String STATUS_SENT                 = "SENT";
    public static final String STATUS_SEND_FAILURE         = "SEND_FAILURE";
    public static final String STATUS_INBOX                = "INBOX";
    public static final String STATUS_INBOX_EMPTY          = "INBOX_EMPTY";
    public static final String STATUS_SEARCH_RESULTS       = "SEARCH_RESULTS";
    public static final String STATUS_NO_MATCHES           = "NO_MATCHES";
    public static final String STATUS_SENT_LIST            = "SENT_LIST";
    public static final String STATUS_SENT_EMPTY           = "SENT_EMPTY";
    public static final String STATUS_SEARCH_SENT_RESULTS  = "SEARCH_SENT_RESULTS";
    public static final String STATUS_EMAIL_CONTENT        = "EMAIL_CONTENT";
    public static final String STATUS_EMAIL_NOT_FOUND      = "EMAIL_NOT_FOUND";
    public static final String STATUS_LOGOUT_SUCCESS       = "LOGOUT_SUCCESS";
    public static final String STATUS_GOODBYE              = "GOODBYE";
    public static final String STATUS_ERROR                = "ERROR";

    // JSON NOTIFICATIONS
    public static final String NOTIF_NEW_EMAIL = "NEW_EMAIL";            // push notification

    // COMMON FIELDS
    public static final String FIELD_COMMAND      = "command";           // JSON field names
    public static final String FIELD_STATUS       = "status";
    public static final String FIELD_ERROR        = "error";
    public static final String FIELD_USERNAME     = "username";
    public static final String FIELD_RECIPIENT    = "recipient";
    public static final String FIELD_SENDER       = "sender";
    public static final String FIELD_SUBJECT      = "subject";
    public static final String FIELD_BODY         = "body";
    public static final String FIELD_TIMESTAMP    = "timestamp";
    public static final String FIELD_EMAILS       = "emails";
    public static final String FIELD_EMAIL        = "email";
    public static final String FIELD_NOTIFICATION = "notification";
}
