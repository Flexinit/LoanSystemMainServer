package main.java;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Config {

    public static final String PUBLIC_KEY = "tOuyfPLXqjAfasGASRrzQnFFoEAqKD";
    public static final String PRIVATE_KEY = "QrJkJVJUhhImPwjxNhBIQzgbPdoCNXCEQlqAIaurhFfyDQWrYZBOaHPrmGwa";
    public static final String GET_TOKEN_LINK = "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
    public static final String PROCESS_MPESA_REQUEST = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";
    public static final String CONSUMER_KEY = "61qHiJZUTbx5wppUUIUJrfVAiqhB4gH0";
    public static final String CONSUMER_SECRET = "nCdwHrPu7mnvFM0W";
    public static final String PASS_KEY = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919";

    public static final String BUSINESS_SHORTCODE = "174379";
    public static final String TRANSACTION_TYPE = "CustomerPayBillOnline";
    public static final String AMOUNT = "1";
    public static final String PARTY_A = "254713021265";
    public static final String PARTY_B = BUSINESS_SHORTCODE;
    public static final String PHONE_NUMBER = "254713021265";
    public static final String CALLBACK_URL = "https://localhost.com";
    public static final String ACCOUNT_REFERENCE = "IESR MPESA INTEGRATION";
    public static final String DESCRIPTION = "IESR MPESA INTEGRATION";

    public static void getCurrentUtcTimeJoda() throws ParseException {
        DateTime now = new DateTime(); // Gives the default time zone.
        DateTime dateTime = now.toDateTime(DateTimeZone.UTC ); // Converting default zone to UTC
        System.out.println(dateTime);
      //  return String.valueOf(dateTime);
    }

    public static Date getCurrentUtcTime() throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMMddHHmmssSSSSSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat localDateFormat = new SimpleDateFormat("yyyyMMMddHHmmssSSSSSS");
        return localDateFormat.parse( simpleDateFormat.format(new Date()) );
    }



    public static String GetUTCdatetimeAsString()
    {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMMddHHmmssSSSSSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = sdf.format(new DateTime());

        return utcTime;
    }
}
