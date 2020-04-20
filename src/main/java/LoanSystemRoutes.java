package main.java;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static akka.http.javadsl.server.Directives.*;
import static main.java.DataSource.getConnection;
import static org.apache.http.HttpHeaders.USER_AGENT;

public class LoanSystemRoutes {

    private final ActorSystem system;
    private final Materializer material;
    private final ActorRef initiatePaymentActor;

    public static StringBuffer response = null;
    public static String loan_Limit = "";
    public static String message = "";
    public static String mpesaNumber = "";
    public static String amountToBeRepaid = "";
    public static String amountPaid = "";
    public static String interest = "";
    public static boolean repaid = false;
    public static String amountRemaining = "";
    public static final String apiUrl = "https://c7dfbe21.ngrok.io/MpesaStk/makeStkPayment";

    static final Logger log = LoggerFactory.getLogger(LoanSystemRoutes.class);

    public LoanSystemRoutes(ActorSystem actorSystem, Materializer materializer) {

        system = actorSystem;
        material = materializer;
        initiatePaymentActor = system.actorOf(InitiatePaymentActor.props(), "initiatePaymentActor");

    }

    public Route createRoute() {

        final Route login = entity(Jackson.unmarshaller(Login.class), userLogin -> {

            try {
                Connection databaseConnection = getConnection();

                String query = "SELECT * FROM dbo.LoanUserManagements WHERE username='" + userLogin.username + "' and password='" + userLogin.password + "'";
                PreparedStatement ps = databaseConnection.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    //  SELECT LAST(column_name) FROM table_name

                    String query1 = "SELECT  Amount FROM dbo.RepaymentMpesaTrans WHERE deviceIMEI ='" + userLogin.deviceIMEI + "'  ";
                    String queryTest = "SELECT TOP (Amount) ORDER BY ID DESC FROM dbo.RepaymentMpesaTrans WHERE deviceIMEI ='" + userLogin.deviceIMEI + "' ";


                    PreparedStatement preparedStmt = databaseConnection.prepareStatement(query1);
                    ResultSet resultSets = preparedStmt.executeQuery();

                    while (resultSets.next()) {

                        amountPaid = resultSets.getString("Amount");
                    }


                    String sql1 = "SELECT Amount, Interest FROM dbo.SuccessfulLoanApplications WHERE deviceIMEI = '" + userLogin.deviceIMEI + "' AND Disbursed = 0";


                    PreparedStatement preparedStmt1 = databaseConnection.prepareStatement(sql1);
                    ResultSet resultSets1 = preparedStmt1.executeQuery();

                    while (resultSets1.next()) {

                        amountToBeRepaid = resultSets1.getString("Amount");
                        interest = resultSets1.getString("Interest");
                    }

                    double remainingAmount = (Double.parseDouble(amountToBeRepaid) + Double.parseDouble(interest)) - Double.parseDouble(amountPaid);
                    amountRemaining = String.valueOf(remainingAmount);

                    log.warn("Amount To be repaid: " + amountToBeRepaid);
                    log.warn("Interest: " + interest);
                    log.warn("Outstanding Loan: " + amountRemaining);

                    message = "Success";

                    return complete(new JsonObject()
                            .put("response", "200")
                            .put("message", message)
                            .put("remainingAmount", amountRemaining).toString());

                } else {

                    return complete(new JsonObject()
                            .put("response", "200")
                            .put("message", message)
                            .put("remainingAmount", amountRemaining).toString());
                }


            } catch (Exception ex) {

                ex.printStackTrace();

                message = "Sorry, we cant seem to reach our Networks. Please try again later. ";

                return complete(new JsonObject()
                        .put("response", "200")
                        .put("message", message).toString());
            }

        });


        final Route applyLoan = entity(Jackson.unmarshaller(UserRegistrations.class), UserRegistrations -> {

            //final MessageDispatcher dispatcher = system.dispatchers().lookup("my-thread-pool-dispatcher")
            //return completeWithFuture(CompletableFuture.supplyAsync(() -> {

            log.warn("Device IMEI: " + UserRegistrations.deviceIMEI + " Amount: " + UserRegistrations.amount);

            try {
                Connection databaseConnection = getConnection();

                String response = null;
                String sql = "SELECT phoneN FROM dbo.UserRegistrations WHERE deviceIMEI = ?";

                PreparedStatement ps = databaseConnection.prepareStatement(sql);
                ps.setString(1, UserRegistrations.deviceIMEI);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {

                    mpesaNumber = rs.getString("phoneN");

                    log.warn("Applicant Phone.: " + mpesaNumber);

                }


                if (rs.next()) {

                    //  disburseLoan(UserRegistrations.deviceIMEI, UserRegistrations.amount);

                }

                int percentageInterest = 14;
                String period = "30";
                double interest = (Integer.parseInt(UserRegistrations.amount) * 0.14);
                boolean disbursed = false;

                String formttedInterest = String.format("%.2f", interest);


                //Insert Login credentials in User Table
                String insertUserRegistrationsSQL = "INSERT INTO dbo.SuccessfulLoanApplications VALUES (?,?,?,?,?,?)";
                PreparedStatement prepStmt = databaseConnection.prepareStatement(insertUserRegistrationsSQL);
                prepStmt.setString(1, UserRegistrations.deviceIMEI);
                prepStmt.setString(2, UserRegistrations.amount);
                prepStmt.setString(3, formttedInterest);
                prepStmt.setBoolean(4, disbursed);
                prepStmt.setString(5, getCurrentDate());
                prepStmt.setString(6, period);

                prepStmt.executeUpdate();

                return complete(new JsonObject()
                        .put("response", "200")
                        .put("message", "Successful. Your Loan will be disbursed to: " + mpesaNumber + "\n" +
                                "Interest: " + formttedInterest + "\n" +
                                "Period: " + period + " days"
                        ).toString());

            } catch (Exception ex) {

                ex.printStackTrace();
            }
            return null;
            //  }));

        });


        final Route repayLoan = entity(Jackson.unmarshaller(UserRegistrations.class), UserRegistrations -> {
            log.warn("Device IMEI: " + UserRegistrations.deviceIMEI + " Amount: " + UserRegistrations.amount);

            try {
                Connection databaseConnection = getConnection();


                String response = null;
                String sql = "SELECT phoneN FROM dbo.UserRegistrations WHERE deviceIMEI = ?";
                PreparedStatement ps = databaseConnection.prepareStatement(sql);
                ps.setString(1, UserRegistrations.deviceIMEI);
                ResultSet rs = ps.executeQuery();


                while (rs.next()) {

                    mpesaNumber = rs.getString("phoneN");


                }
                log.warn("Applicant Phone.: " + mpesaNumber);


                String sql1 = "SELECT Amount, Interest FROM dbo.SuccessfulLoanApplications WHERE deviceIMEI = ? AND Disbursed = 0";
                PreparedStatement ps1 = databaseConnection.prepareStatement(sql1);
                ps1.setString(1, UserRegistrations.deviceIMEI);
                ResultSet resultSetLoanRep = ps1.executeQuery();


                while (resultSetLoanRep.next()) {

                    amountToBeRepaid = resultSetLoanRep.getString("Amount");
                    interest = resultSetLoanRep.getString("Interest");

                }

                if (amountToBeRepaid == null) {

                    return complete(new JsonObject()
                            .put("response", "200")
                            .put("message", "Sorry. You have no outstanding loan with us."
                            ).toString()
                    );
                }

                log.warn("Applicant AmountToBeRepaid.: " + amountToBeRepaid);
                log.warn("Applicant InterestToBePaid.: " + interest);

                int random = (int) (Math.random() * 50 + 1);


                String transId = String.valueOf(random);

                repaid = makeMpesaAPIRequest(mpesaNumber, UserRegistrations.amount, apiUrl, UserRegistrations.deviceIMEI, transId);


                if (resultSetLoanRep.next()) {

                    makeMpesaAPIRequest(mpesaNumber, UserRegistrations.amount, apiUrl, UserRegistrations.deviceIMEI, transId);
                }


                String sqlStatement = "SELECT  Amount FROM dbo.RepaymentMpesaTrans WHERE TransactionID ='" + transId + "' ";
                PreparedStatement preparedStmt = databaseConnection.prepareStatement(sqlStatement);
                ResultSet resultSets = preparedStmt.executeQuery();

                while (resultSets.next()) {

                    amountPaid = rs.getString("Amount");
                }


                double remainingAmount = (Double.parseDouble(amountToBeRepaid) + Double.parseDouble(interest)) - Double.parseDouble(UserRegistrations.amount);
                String amountRemaining = String.valueOf(remainingAmount);


                if (repaid) {

                    String insertLoginsSQL = "INSERT INTO dbo.LoanRepayments VALUES (?,?,?,?,?)";
                    PreparedStatement prepStmt = databaseConnection.prepareStatement(insertLoginsSQL);
                    prepStmt.setString(1, UserRegistrations.deviceIMEI);
                    prepStmt.setString(2, amountPaid);
                    prepStmt.setString(3, amountRemaining);
                    prepStmt.setString(4, getCurrentDate());
                    prepStmt.setString(5, transId);

                    prepStmt.executeUpdate();
                    message = "Success";


                    return complete(new JsonObject()
                            .put("response", "200")
                            .put("message", "Please wait. Your request is being processed."
                            ).toString());

                } else {

                    return complete(new JsonObject()
                            .put("response", "200")
                            .put("message", "Loan Repayment Unsuccessful"
                            ).toString());
                }

            } catch (Exception ex) {

                ex.printStackTrace();
            }
            return complete(new JsonObject()
                    .put("response", "200")
                    .put("message", "Loan Repayment Unsuccessful"
                    ).toString());

        });


        final Route initiatePayment = entity(Jackson.unmarshaller(PersonalDetails.class), details -> {


            try {

                //Check if user exists
                Connection databaseConnection = getConnection();

                String sql = "SELECT * FROM dbo.UserRegistrations WHERE deviceIMEI = ?";

                PreparedStatement ps = databaseConnection.prepareStatement(sql);
                ps.setString(1, details.deviceIMEI);
                ResultSet rs = ps.executeQuery();


                if (rs.next()) {

                    message = "Sorry, you are already registered";
                    log.warn("User Exists ");


                } else {

                    initiatePaymentActor.tell(new InitiatePaymentActor.LoanApplication(details.firstName, details.lastName, details.phoneN, details.deviceIMEI,
                            details.IDnumber, details.dateOfBirth, details.gender, details.emalAddress, details.phoneIsMine, details.phoneIsNew,
                            details.howLongHaveUsedPhone, details.averageIncome, details.haveAjob, details.selfEmployed, details.student,
                            details.haveNoIncome, details.purposeOfLoan, details.kindOfExpenseForLoan, details.descriptionForMainSourceOfIncome,
                            details.dateOfStartingJob, details.latestHouseholdIncome, details.anyOutstandingLoan, details.descriptionForPurposeOfLoan, details.password), ActorRef.noSender());


                    log.info("Transaction Details for the user from Loan Applicant First Name: [ {} ] and LastName [ {} ] password [ {} ]",
                            details.firstName, details.lastName);

                    System.out.println("++++++++Transaction Details for the user from Loan Applicant++++++++  \n" +
                            details.firstName + " " + details.lastName + "\n" +
                            "----------------------------------------");


                    String str = new Date().getTime() + " " + details.firstName + "\n\n" +
                            details.firstName + "\n\n" +
                            details.lastName + "\n\n" +
                            details.phoneN + "\n\n" +
                            details.deviceIMEI + "\n\n" +
                            details.IDnumber + "\n\n" +
                            details.dateOfBirth + "\n\n" +
                            details.gender + "\n\n" +
                            details.emalAddress + "\n\n" +
                            details.phoneIsMine + "\n\n" +
                            details.phoneIsNew + "\n\n" +
                            details.howLongHaveUsedPhone + "\n\n" +
                            details.averageIncome + "\n\n" +
                            details.haveAjob + "\n\n" +
                            details.selfEmployed + "\n\n" +
                            details.student + "\n\n" +
                            "Have No Income: " + details.haveNoIncome + "\n\n" +
                            details.purposeOfLoan + "\n\n" +
                            details.kindOfExpenseForLoan + "\n\n" +
                            "Have Outstanding Loan: " + details.anyOutstandingLoan + "\n\n" +
                            details.descriptionForPurposeOfLoan + "\n\n";


                    if (details.anyOutstandingLoan) {

                        message = "Sorry, You do not qualify for this Loan";
                        loan_Limit = "0";


                    } else if (details.haveNoIncome) {

                        message = "Sorry, You do not qualify for this Loan";
                        loan_Limit = "0";


                    } else {

                        int lowerLimit = Integer.parseInt(details.averageIncome.substring(0, 5).replace(",", ""));
                        int upperLimit = Integer.parseInt(details.averageIncome.substring(9, 14).replace(",", ""));
                        int caculated_loan_limit = (lowerLimit + upperLimit) / 2;

                        log.warn("LowerLimit: " + lowerLimit);
                        log.warn("UpperLimit: " + upperLimit);

                        loan_Limit = String.valueOf(caculated_loan_limit);
                        message = "Loan Application Successful. Please wait for your Loan Application to be Approved. Your Loan Limit is: " + loan_Limit;


                    }


                    BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\User\\Documents\\LOAN_SYSTEM_LOGS\\Loan_system_logs.txt", true));
                    writer.append(' ');
                    writer.append(str + "\n\n\nResponse: " + message);

                    writer.close();

                    //   message = disburseLoan("+25477166007", "2");
                    message = "Your Loan Application is Successful. Your Loan Limit is " + loan_Limit;
                    message = "Your Loan Application is Successful. Your Loan Limit is " + loan_Limit;
                }

                return complete(new JsonObject()

                        .put("response", "200")
                        .put("loan_limit", loan_Limit)
                        .put("message", message).toString());

            } catch (Exception ex) {

                ex.printStackTrace();

                //An Error Occurs
                return complete(new JsonObject()
                        .put("response", "200")
                        .put("message", "Sorry, Your Loan Couldn't be processed. Please Contact Customer Care.").toString());

            }

        });

        return route(

                path("initiatePayment", ()
                        -> initiatePayment),

                path("login", ()
                        -> login),

                path("applyForLoan", ()
                        -> applyLoan),

                path("repayLoan", ()
                        -> repayLoan));
    }


    //Send Give Loan to Customer
    private static boolean makeMpesaAPIRequest(String phoneNumber, String amount, String POST_URL, String deviceIMEI, String TransID) {
        try {


            final String POST_PARAMS = "phoneNumber=" + phoneNumber + "&amount=" + amount + "&deviceIMEI=" + deviceIMEI + "&TransID=" + TransID;


            URL obj = new URL(POST_URL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);

            // For POST only - START
            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();
            os.write(POST_PARAMS.getBytes());
            os.flush();
            os.close();
            // For POST only - END

            int responseCode = con.getResponseCode();
            log.warn("POST Response Code :: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) { //success
                repaid = true;

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // print result
                log.warn("Response: " + response.toString() + "POST request not worked");

                // System.out.println(response.toString());
            } else {
                log.warn("ResponseCode: " + responseCode + "  POST request not worked");
                // log.warn("Response: " + response.toString() + "POST request not worked");


                repaid = false;
            }


            // message = "Your Loan has been approved.";
            return repaid;

        } catch (Exception ex) {

            ex.printStackTrace();
            repaid = false;
            //message = "Your Loan has NOT been approved.";
            return repaid;
        }
    }


    private static class Login {

        final String deviceIMEI;
        final String username;
        final String password;

        @JsonCreator
        Login(@JsonProperty("deviceIMEI") String deviceIMEI,
              @JsonProperty("username") String username,
              @JsonProperty("password") String password) {

            this.deviceIMEI = deviceIMEI;
            this.username = username;
            this.password = password;

        }

    }

    private static class UserRegistrations {

        final String deviceIMEI;
        final String amount;

        @JsonCreator
        UserRegistrations(@JsonProperty("deviceIMEI") String deviceIMEI,
                          @JsonProperty("amount") String amount) {

            this.deviceIMEI = deviceIMEI;
            this.amount = amount;

        }

    }

    private static class PersonalDetails {

        final String firstName;
        final String lastName;
        final String phoneN;
        final String deviceIMEI;
        final String IDnumber;
        final String dateOfBirth;
        final String emalAddress;
        final String gender;
        final boolean phoneIsMine;
        final boolean phoneIsNew;
        final String howLongHaveUsedPhone;
        final String averageIncome;
        final boolean haveAjob;
        final boolean selfEmployed;
        final boolean student;
        final boolean haveNoIncome;
        final String purposeOfLoan;
        final String kindOfExpenseForLoan;
        final String descriptionForPurposeOfLoan;
        final String descriptionForMainSourceOfIncome;
        final String dateOfStartingJob;
        final String latestHouseholdIncome;
        final boolean anyOutstandingLoan;
        final String password;

        @JsonCreator
        PersonalDetails(@JsonProperty("FirstName") String firstName,
                        @JsonProperty("LastName") String lastName,
                        @JsonProperty("MobileNumber") String phoneN,
                        @JsonProperty("deviceIMEI") String deviceIMEI,
                        @JsonProperty("IDNumber") String IDnumber,
                        @JsonProperty("DateOfBirth") String dateOfBirth,
                        @JsonProperty("Gender") String gender,
                        @JsonProperty("EmailAddress") String emailAddress,
                        @JsonProperty("phoneIsMine") boolean phoneIsMine,
                        @JsonProperty("phoneIsNew") boolean phoneIsNew,
                        @JsonProperty("howLongHaveUsedPhone") String howLongHaveUsedPhone,
                        @JsonProperty("averageIncome") String averageIncome,
                        @JsonProperty("haveAjob") boolean haveAjob,
                        @JsonProperty("selfEmployed") boolean selfEmployed,
                        @JsonProperty("student") boolean student,
                        @JsonProperty("haveNoIncome") boolean haveNoIncome,
                        @JsonProperty("purposeOfLoan") String purposeOfLoan,
                        @JsonProperty("kindOfExpenseForLoan") String kindOfExpenseForLoan,
                        @JsonProperty("descriptionForMainSourceOfIncome") String descriptionForMainSourceOfIncome,
                        @JsonProperty("dateOfStartingJob") String dateOfStartingJob,
                        @JsonProperty("latestHouseholdIncome") String latestHouseholdIncome,
                        @JsonProperty("anyOutstandingLoan") boolean anyOutstandingLoan,
                        @JsonProperty("password") String password,
                        @JsonProperty("descriptionForPurposeOfLoan") String descriptionForPurposeOfLoan) {

            this.firstName = firstName;
            this.lastName = lastName;
            this.phoneN = phoneN;
            this.deviceIMEI = deviceIMEI;
            this.IDnumber = IDnumber;
            this.dateOfBirth = dateOfBirth;
            this.emalAddress = emailAddress;
            this.gender = gender;
            this.phoneIsMine = phoneIsMine;
            this.phoneIsNew = phoneIsNew;
            this.howLongHaveUsedPhone = howLongHaveUsedPhone;
            this.averageIncome = averageIncome;
            this.haveAjob = haveAjob;
            this.selfEmployed = selfEmployed;
            this.student = student;
            this.haveNoIncome = haveNoIncome;
            this.purposeOfLoan = purposeOfLoan;
            this.kindOfExpenseForLoan = kindOfExpenseForLoan;
            this.descriptionForPurposeOfLoan = descriptionForPurposeOfLoan;
            this.descriptionForMainSourceOfIncome = descriptionForMainSourceOfIncome;
            this.dateOfStartingJob = dateOfStartingJob;
            this.latestHouseholdIncome = latestHouseholdIncome;
            this.anyOutstandingLoan = anyOutstandingLoan;
            this.password = password;

        }
    }

    public static String getCurrentDate() {

        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date dateobj = new Date();
        System.out.println(df.format(dateobj));

        return df.format(dateobj);
    }

}
