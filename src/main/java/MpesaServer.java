package main.java;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.squareup.okhttp.*;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;


import static akka.http.javadsl.server.Directives.route;
import static main.java.Config.*;

import org.json.*;


public class MpesaServer {


    final static ActorSystem system = ActorSystem.create("httpStream");
    final static Materializer material = ActorMaterializer.create(system);
    static final Logger log = LoggerFactory.getLogger(MpesaServer.class);
    //private final main.java.IESRmpesa.IESRmpesaRoutes simpleRoutes;
    public Mpesa mpesa;


    public static void main() throws IOException, JSONException {

        final Http http = Http.get(system);
        // final MpesaServer app = new MpesaServer(system);
        //final String host = "192.168.88.100";
        final String host = "192.168.88.9";
        //final String host = "127.0.0.1";
        final int port = 9000;


        String token = getToken();
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String passWord = Config.BUSINESS_SHORTCODE + Config.PASS_KEY + timeStamp;
        byte[] bytesEncoded = Base64.encodeBase64(passWord.getBytes());
        String encodedPass = new String(bytesEncoded);


        //makePayment();
        //Register Confirmation/Validation URL
        Mpesa.payViaMpesa();
/*
        STKPushSimulation(Config.BUSINESS_SHORTCODE, encodedPass, timeStamp, Config.TRANSACTION_TYPE, Config.AMOUNT, Config.PHONE_NUMBER, Config.PARTY_A,
                main.java.IESRmpesa.Config.PARTY_B, main.java.IESRmpesa.Config.CALLBACK_URL, Config.CALLBACK_URL, Config.ACCOUNT_REFERENCE, Config.DESCRIPTION);
*/


    }


    public static String getToken() throws IOException {

        URL url = new URL(Config.GET_TOKEN_LINK);

        String authStr = Config.CONSUMER_KEY + ":" + Config.CONSUMER_SECRET;
        System.out.println("Original String is " + authStr);

        // encode data on your side using BASE64
        byte[] bytesEncoded = Base64.encodeBase64(authStr.getBytes());
        String authEncoded = new String(bytesEncoded);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + authEncoded);


        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        String line = null;
        StringBuilder sb = new StringBuilder();

        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }

        bufferedReader.close();
        String result = sb.toString();

        System.out.println("ResponseMessage: " + connection.getResponseMessage());
        System.out.println("ResponseContent: " + result);

        JsonObject jsonObject = new JsonObject(result);

        String accessToken = jsonObject.getString("access_token");
        System.out.println("Access Token: " + accessToken);
        String expiresIn = jsonObject.getString("expires_in");


        return accessToken;
    }

    public static void makePayment() throws IOException {
        String passWord = null;
        String token = getToken();
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());


        URL object = new URL(Config.PROCESS_MPESA_REQUEST);

        HttpURLConnection con = (HttpURLConnection) object.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json; charset=UTF-8");
        con.setRequestProperty("Authorization", "Bearer " + token);
        con.setRequestMethod("POST");

        passWord = Config.BUSINESS_SHORTCODE + Config.PASS_KEY + timeStamp;
        byte[] bytesEncoded = Base64.encodeBase64(passWord.getBytes());
        String encodedPass = new String(bytesEncoded);


        JsonObject details = new JsonObject();
        details.put("BusinessShortCode", Config.BUSINESS_SHORTCODE);
        details.put("Password", encodedPass);
        details.put("Timestamp", timeStamp);
        details.put("TransactionType", "CustomerPayBillOnline");
        details.put("Amount", "1");
        details.put("PartyA", "254724497834");
        details.put("PartyB", Config.BUSINESS_SHORTCODE);
        details.put("PhoneNumber", "254724497834");
        details.put("CallBackURL", "https://localhost.com");
        details.put("AccountReference", "IESR MPESA INTEGRATION");
        details.put("TransactionDesc", "IESR MPESA INTEGRATION");

        System.out.println("DATA SENT: " + details.toString());

        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(details.toString());
        wr.flush();

//display what returns the POST request

        StringBuilder sb = new StringBuilder();
        int HttpResult = con.getResponseCode();
        if (HttpResult == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            System.out.println("RESPONSE: " + sb.toString());
        } else {
            System.out.println("ERROR: " + con.getResponseMessage());
        }
    }

    public static String STKPushSimulation(String businessShortCode, String password, String timestamp, String transactionType, String amount, String phoneNumber, String partyA, String partyB, String callBackURL, String queueTimeOutURL, String accountReference, String transactionDesc) throws IOException, JSONException {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("BusinessShortCode", businessShortCode);
        jsonObject.put("Password", password);
        jsonObject.put("Timestamp", timestamp);
        jsonObject.put("TransactionType", transactionType);
        jsonObject.put("Amount", amount);
        jsonObject.put("PhoneNumber", phoneNumber);
        jsonObject.put("PartyA", partyA);
        jsonObject.put("PartyB", partyB);
        jsonObject.put("CallBackURL", callBackURL);
        jsonObject.put("AccountReference", accountReference);
        jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
        jsonObject.put("TransactionDesc", transactionDesc);


        jsonArray.put(jsonObject);

        String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");

        OkHttpClient client = new OkHttpClient();
        String url = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, requestJson);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("authorization", "Bearer " + getToken())
                .addHeader("cache-control", "no-cache")
                .build();


        Response response = client.newCall(request).execute();
        System.out.println("RESPONSE: " + response.body().string());
        return response.body().toString();


    }

    public static class Mpesa {
        String appKey;
        String appSecret;

        public Mpesa(String app_key, String app_secret) {
            appKey = app_key;
            appSecret = app_secret;
        }

        public Mpesa() {
        }

        public String authenticate() throws IOException, JSONException {
            String app_key = appKey/*"GvzjNnYgNJtwgwfLBkZh65VPwfuKvs0V"*/;
            String app_secret = appSecret;
            String appKeySecret = app_key + ":" + app_secret;
            byte[] bytes = appKeySecret.getBytes("ISO-8859-1");
            String encoded = java.util.Base64.getEncoder().encodeToString(bytes);


            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("https://api.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials")
                    .get()
                    .addHeader("authorization", "Basic " + encoded)
                    .addHeader("cache-control", "no-cache")
                    .build();

            Response response = client.newCall(request).execute();
            //  JSONObject jsonObject = new JSONObject(response.body().string().trim());
            System.out.println(response.toString());
            //return jsonObject.getString("access_token");
            return response.toString();
        }

        public String C2BSimulation(String shortCode, String commandID, String amount, String MSISDN, String billRefNumber) throws IOException, JSONException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ShortCode", shortCode);
            jsonObject.put("CommandID", commandID);
            jsonObject.put("Amount", amount);
            jsonObject.put("Msisdn", MSISDN);
            jsonObject.put("BillRefNumber", billRefNumber);

            jsonArray.put(jsonObject);

            String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");
            System.out.println(requestJson);
            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson);
            Request request = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/mpesa/c2b/v1/simulate")
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .addHeader("authorization", "Bearer " + authenticate())
                    .addHeader("cache-control", "no-cache")
                    .build();

            Response response = client.newCall(request).execute();
            System.out.println("C2B Response: " + response.body().string());
            return response.body().toString();
        }

        public String B2CRequest(String initiatorName, String securityCredential, String commandID, String amount, String partyA, String partyB, String remarks, String queueTimeOutURL, String resultURL, String occassion) throws IOException, JSONException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("InitiatorName", initiatorName);
            jsonObject.put("SecurityCredential", securityCredential);
            jsonObject.put("CommandID", commandID);
            jsonObject.put("Amount", amount);
            jsonObject.put("PartyA", partyA);
            jsonObject.put("PartyB", partyB);
            jsonObject.put("Remarks", remarks);
            jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
            jsonObject.put("ResultURL", resultURL);
            jsonObject.put("Occassion", occassion);
            jsonArray.put(jsonObject);

            String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");

            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson);
            Request request = new Request.Builder()
                    .url("https://api.safaricom.co.ke/mpesa/b2c/v1/paymentrequest")
                    .post(body)
                    .addHeader("content-type", "application/json")
                    //   .addHeader("authorization", "Bearer " + authenticate())
                    .addHeader("authorization", "Bearer " + "cqWHEqeEYEWHbBa13uMCf5WPk4Sz")
                    .addHeader("cache-control", "no-cache")
                    .build();

            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
            return response.body().toString();
        }


        public static String queryCRB(String reportType, String identityNumber, String identityType, int loanAmount, String report_reason) throws IOException, JSONException, ParseException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("report_type", reportType);
            jsonObject.put("identity_number", identityNumber);
            jsonObject.put("identity_type", identityType);
            jsonObject.put("loan_amount", loanAmount);
            jsonObject.put("report_reason", report_reason);

            Instant instant = Instant.now();
            System.out.println(instant.toString());

            DateTime now = new DateTime(DateTimeZone.UTC);

            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSSSSS").format(getCurrentUtcTime());


            System.out.println("Timestamp: " + timeStamp);

            String originalString = PRIVATE_KEY + jsonObject + PUBLIC_KEY + timeStamp;

            String sha256hex = DigestUtils.sha256Hex(originalString);

            String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");

            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, String.valueOf(jsonObject));
            Request request = new Request.Builder()
                    .url("https://api.metropol.co.ke:5555/v2_1/report/json")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-METROPOL-REST-API-KEY", "tOuyfPLXqjAfasGASRrzQnFFoEAqKD")
                    .addHeader("X-METROPOL-REST-API-HASH", sha256hex)
                    .addHeader("X-METROPOL-REST-API-TIMESTAMP", timeStamp)
                    .addHeader("cache-control", "no-cache")
                    .build();

            Response response = client.newCall(request).execute();
            System.out.println(request.body().contentType());
            System.out.println(response.body().contentType());
            System.out.println(response.body().string());
            return response.body().toString();
        }

        public static void makeCRBQuery(int reportType, String identityNumber, String identityType, int loanAmount, Integer report_reason) throws ParseException, JSONException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("report_type", 5);
            jsonObject.put("identity_number", identityNumber);
            jsonObject.put("identity_type", identityType);
            jsonObject.put("loan_amount", Integer.valueOf(loanAmount));
            jsonObject.put("report_reason", 1);

            HttpClient httpClient = new DefaultHttpClient();
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSSSSS").format(getCurrentUtcTime());


            System.out.println("Timestamp: " + timeStamp);

            String originalString = PRIVATE_KEY + jsonObject + PUBLIC_KEY + timeStamp;

            String sha256hex = DigestUtils.sha256Hex(originalString);

            try {
                HttpPost request = new HttpPost("https://api.metropol.co.ke:5555/v2_1/report/json");
                StringEntity params = new StringEntity("{\"report_type\":\"" + reportType + "\",\"identity_number\":\"" +
                        identityNumber + "\",\"identity_type\":\"" + identityType + "\",\"loan_amount\":\"" + loanAmount + "\",\"report_reason\":\"" + report_reason + "\"}");

                String json_string = "{\"report_type\":\"" + reportType + "\",\"identity_number\":\"" +
                        identityNumber + "\",\"identity_type\":\"" + identityType + "\",\"loan_amount\":\"" + loanAmount + "\",\"report_reason\":\"" + report_reason + "\"}";

                JsonObject parameters = new JsonObject(json_string);
                System.out.println(parameters);

                StringEntity params1 = new StringEntity(jsonObject.toString().trim());

                request.addHeader("content-type", "application/json");
                request.addHeader("X-METROPOL-REST-API-KEY", "tOuyfPLXqjAfasGASRrzQnFFoEAqKD");
                request.addHeader("X-METROPOL-REST-API-HASH", sha256hex);
                request.addHeader("X-METROPOL-REST-API-TIMESTAMP", timeStamp);
                request.addHeader("Accept", "application/json");
                request.setEntity(params);
                HttpResponse response = httpClient.execute(request);

                String responseData = IOUtils.toString(response.getEntity().getContent(), "utf-8");

                System.out.println("\n\nAsk CRB: " + responseData);
                // handle response here...
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                httpClient.getConnectionManager().shutdown();
            }


        }

        public String B2BRequest(String initiatorName, String accountReference, String securityCredential, String commandID, String senderIdentifierType, String receiverIdentifierType, float amount, String partyA, String partyB, String remarks, String queueTimeOutURL, String resultURL, String occassion) throws IOException, JSONException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Initiator", initiatorName);
            jsonObject.put("SecurityCredential", securityCredential);
            jsonObject.put("CommandID", commandID);
            jsonObject.put("SenderIdentifierType", senderIdentifierType);
            jsonObject.put("RecieverIdentifierType", receiverIdentifierType);
            jsonObject.put("Amount", amount);
            jsonObject.put("PartyA", partyA);
            jsonObject.put("PartyB", partyB);
            jsonObject.put("Remarks", remarks);
            jsonObject.put("AccountReference", accountReference);
            jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
            jsonObject.put("ResultURL", resultURL);


            jsonArray.put(jsonObject);

            String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");
            System.out.println(requestJson);

            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson);
            Request request = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/safaricom/b2b/v1/paymentrequest")
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .addHeader("authorization", "Bearer " + authenticate())
                    .addHeader("cache-control", "no-cache")

                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();

        }


        public String STKPushSimulation(String businessShortCode, String password, String timestamp, String transactionType, String amount, String phoneNumber, String partyA, String partyB, String callBackURL, String queueTimeOutURL, String accountReference, String transactionDesc) throws IOException, JSONException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("BusinessShortCode", businessShortCode);
            jsonObject.put("Password", password);
            jsonObject.put("Timestamp", timestamp);
            jsonObject.put("TransactionType", transactionType);
            jsonObject.put("Amount", amount);
            jsonObject.put("PhoneNumber", phoneNumber);
            jsonObject.put("PartyA", partyA);
            jsonObject.put("PartyB", partyB);
            jsonObject.put("CallBackURL", callBackURL);
            jsonObject.put("AccountReference", accountReference);
            jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
            jsonObject.put("TransactionDesc", transactionDesc);


            jsonArray.put(jsonObject);

            String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");

            OkHttpClient client = new OkHttpClient();
            String url = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .addHeader("authorization", "Bearer " + authenticate())
                    .addHeader("cache-control", "no-cache")
                    .build();


            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
            return response.body().toString();
        }

        public String STKPushTransactionStatus(String businessShortCode, String password, String timestamp, String checkoutRequestID) throws IOException, JSONException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("BusinessShortCode", businessShortCode);
            jsonObject.put("Password", password);
            jsonObject.put("Timestamp", timestamp);
            jsonObject.put("CheckoutRequestID", checkoutRequestID);


            jsonArray.put(jsonObject);

            String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");


            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson);
            Request request = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/mpesa/stkpushquery/v1/query")
                    .post(body)
                    .addHeader("authorization", "Bearer " + authenticate())
                    .addHeader("content-type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
            return response.body().toString();

        }

        public String reversal(String initiator, String securityCredential, String commandID, String transactionID, String amount, String receiverParty, String recieverIdentifierType, String resultURL, String queueTimeOutURL, String remarks, String ocassion) throws IOException, JSONException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Initiator", initiator);
            jsonObject.put("SecurityCredential", securityCredential);
            jsonObject.put("CommandID", commandID);
            jsonObject.put("TransactionID", transactionID);
            jsonObject.put("Amount", amount);
            jsonObject.put("ReceiverParty", receiverParty);
            jsonObject.put("RecieverIdentifierType", recieverIdentifierType);
            jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
            jsonObject.put("ResultURL", resultURL);
            jsonObject.put("Remarks", remarks);
            jsonObject.put("Occasion", ocassion);


            jsonArray.put(jsonObject);

            String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");
            System.out.println(requestJson);

            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson);
            Request request = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/safaricom/reversal/v1/request")
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .addHeader("authorization", "Bearer xNA3e9KhKQ8qkdTxJJo7IDGkpFNV")
                    .addHeader("cache-control", "no-cache")
                    .build();

            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
            return response.body().string();
        }

        public String balanceInquiry(String initiator, String commandID, String securityCredential, String partyA, String identifierType, String remarks, String queueTimeOutURL, String resultURL) throws IOException, JSONException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Initiator", initiator);
            jsonObject.put("SecurityCredential", securityCredential);
            jsonObject.put("CommandID", commandID);
            jsonObject.put("PartyA", partyA);
            jsonObject.put("IdentifierType", identifierType);
            jsonObject.put("Remarks", remarks);
            jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
            jsonObject.put("ResultURL", resultURL);


            jsonArray.put(jsonObject);

            String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");
            System.out.println(requestJson);

            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson);
            Request request = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/safaricom/accountbalance/v1/query")
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .addHeader("authorization", "Bearer fwu89P2Jf6MB1A2VJoouPg0BFHFM")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("postman-token", "2aa448be-7d56-a796-065f-b378ede8b136")
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }

        public String registerURL(String shortCode, String responseType, String confirmationURL, String validationURL) throws IOException, JSONException {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ShortCode", shortCode);
            jsonObject.put("ResponseType", responseType);
            jsonObject.put("ConfirmationURL", confirmationURL);
            jsonObject.put("ValidationURL", validationURL);


            jsonArray.put(jsonObject);

            String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");
            System.out.println(requestJson);

            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson);
            Request request = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/mpesa/c2b/v1/registerurl")
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .addHeader("authorization", "Bearer " + authenticate())
                    .addHeader("cache-control", "no-cache")
                    .build();

            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
            return response.body().toString();
        }

        //COMMENTED OUT FOR TEST PURPOSES

        public static void payViaMpesa() throws IOException, JSONException {
            // Mpesa m = new Mpesa("CktydtNoEgUQ9H2Jx1IShtkY28G1Bg6V", "zo1MIYc8i2FKvg20");
            Mpesa m = new Mpesa();
            //   m.authenticate();
            //  m.registerURL("600326", "Completed", "http://196.216.72.82:9000/confirmPayment", "http://196.216.72.82:9000/confirmPayment");
            //m.C2BSimulation("600326", "CustomerPayBillOnline", "1", "254708866922", "IESRMpesa");
            String token = m.authenticate();

            m.B2CRequest("emesccos", "tdH5WRbt2hFAPEAAZo70xVIzY3Gu", "BusinessPayment", "1", "322116", "254769954582", "This is a test", "http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BConfirmation", "http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation", "http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation");

/*
        m.B2BRequest("testapi","his","BVeDP3XWGFG+NCQri04jHp6c0rCajO1JAOccQ7Bsu/Mup3Rh2Gd9IHQEE0SeA1oBXAt/VBAL/cJP+VKU9qRF6voqCa0P1XG8pcv5hTZUcBkbbb8Qqvqn28+s/tBvsLXwsB4QaageFDDZgS6b6gbK1p7+UZ/hRYHL8WclTpYBrQGfhqKZxduh0bPWvK4rt+uqR3hdVlO0RdJSkcOVCVp+FxizPSk3nI6LFq14Jj2G0TwuQ4a13J/KVu5eeFG65gzE1NnIVouHKeBPz9b9xvove156aR16uxh4rBq5U6UAKC/kUhaJ0wOLTvb762CioudL87C6xaPVdTF4qcSD6jM4PA==","BusinessPayBill","1", "4",22,"600576","600000","This","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BConfirmation","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation");
*/
/*
        m.STKPushSimulation("174379","MTc0Mzc5YmZiMjc5ZjlhYTliZGJjZjE1OGU5N2RkNzFhNDY3Y2QyZTBjODkzMDU5YjEwZjc4ZTZiNzJhZGExZWQyYzkxOTIwMTcwODI0MTU1MDU1","20170824155055","CustomerPayBillOnline","1","254724513769","254724513769","174379","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation","sasas","asdasd");
*/
/*
        m.reversal("testapi","BVeDP3XWGFG+NCQri04jHp6c0rCajO1JAOccQ7Bsu/Mup3Rh2Gd9IHQEE0SeA1oBXAt/VBAL/cJP+VKU9qRF6voqCa0P1XG8pcv5hTZUcBkbbb8Qqvqn28+s/tBvsLXwsB4QaageFDDZgS6b6gbK1p7+UZ/hRYHL8WclTpYBrQGfhqKZxduh0bPWvK4rt+uqR3hdVlO0RdJSkcOVCVp+FxizPSk3nI6LFq14Jj2G0TwuQ4a13J/KVu5eeFG65gzE1NnIVouHKeBPz9b9xvove156aR16uxh4rBq5U6UAKC/kUhaJ0wOLTvb762CioudL87C6xaPVdTF4qcSD6jM4PA==","TransactionReversal","2121","2","22","4","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BConfirmation","Remarks","Ocassions");
*/
/*
        m.registerURL("600576","Completed","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation");
*/
/*
        System.out.println("Hello World!");
*/
/*
        m.balanceInquiry("testapi","AccountBalance","BVeDP3XWGFG+NCQri04jHp6c0rCajO1JAOccQ7Bsu/Mup3Rh2Gd9IHQEE0SeA1oBXAt/VBAL/cJP+VKU9qRF6voqCa0P1XG8pcv5hTZUcBkbbb8Qqvqn28+s/tBvsLXwsB4QaageFDDZgS6b6gbK1p7+UZ/hRYHL8WclTpYBrQGfhqKZxduh0bPWvK4rt+uqR3hdVlO0RdJSkcOVCVp+FxizPSk3nI6LFq14Jj2G0TwuQ4a13J/KVu5eeFG65gzE1NnIVouHKeBPz9b9xvove156aR16uxh4rBq5U6UAKC/kUhaJ0wOLTvb762CioudL87C6xaPVdTF4qcSD6jM4PA==", "600576","4","These","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BConfirmation","http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BConfirmation");
*/
/*
        m.STKPushTransactionStatus("174379","MTc0Mzc5YmZiMjc5ZjlhYTliZGJjZjE1OGU5N2RkNzFhNDY3Y2QyZTBjODkzMDU5YjEwZjc4ZTZiNzJhZGExZWQyYzkxOTIwMTcwODI0MTU1MDU1","20170824155055","ws_CO_27102017101215530");
*/
        }

    }
}
