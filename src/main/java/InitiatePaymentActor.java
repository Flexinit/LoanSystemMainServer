package main.java;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContextExecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static main.java.DataSource.getConnection;

public class InitiatePaymentActor extends AbstractActor {


    public static Props props() {
        return Props.create(InitiatePaymentActor.class, InitiatePaymentActor::new);
    }

    //final Http http = Http.get(context().system());
    final ExecutionContextExecutor dispatcher = context().dispatcher();
    final Materializer materializer = ActorMaterializer.create(context());


    static public class LoanApplication {

        public final String firstName;
        public final String lastName;
        public final String phoneN;
        public final String deviceIMEI;
        public final String IDnumber;
        public final String dateOfBirth;
        public final String emalAddress;
        public final String gender;
        public final boolean phoneIsMine;
        public final boolean phoneIsNew;
        public final String howLongHaveUsedPhone;
        public final String averageIncome;
        public final boolean haveAjob;
        public final boolean selfEmployed;
        public final boolean student;
        public final boolean haveNoIncome;
        public final String purposeOfLoan;
        public final String kindOfExpenseForLoan;
        public final String descriptionForPurposeOfLoan;
        public final String descriptionForMainSourceOfIncome;
        public final String dateOfStartingJob;
        public final String latestHouseholdIncome;
        public final boolean anyOutstandingLoan;
        public final String password;


        public LoanApplication(String firstName,
                               String lastName,
                               String phoneN,
                               String deviceIMEI,
                               String IDnumber,
                               String dateOfBirth,
                               String gender,
                               String emailAddress,
                               boolean phoneIsMine,
                               boolean phoneIsNew,
                               String howLongHaveUsedPhone,
                               String averageIncome,
                               boolean haveAjob,
                               boolean selfEmployed,
                               boolean student,
                               boolean haveNoIncome,
                               String purposeOfLoan,
                               String kindOfExpenseForLoan,
                               String descriptionForMainSourceOfIncome,
                               String dateOfStartingJob,
                               String latestHouseholdIncome,
                               boolean anyOutstandingLoan,
                               String descriptionForPurposeOfLoan,
                               String password) {

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

    static final Logger logger = LoggerFactory.getLogger(InitiatePaymentActor.class);

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .match(LoanApplication.class, details -> {

                    //Post to the database
                    String insertSQL = "INSERT INTO dbo.UserRegistrations VALUES (?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    try {
                        Connection databaseConnection = getConnection();

                        PreparedStatement ps = databaseConnection.prepareStatement(insertSQL);
                        ps.setString(1, details.firstName); // where 1 is the parameter index
                        ps.setString(2, details.lastName);
                        ps.setString(3, details.deviceIMEI);
                        ps.setString(4, details.phoneN);
                        ps.setString(5, details.IDnumber);
                        ps.setString(6, details.dateOfBirth);
                        ps.setString(7, details.emalAddress);
                        ps.setString(8, details.gender);
                        ps.setBoolean(9, details.phoneIsMine);
                        ps.setBoolean(10, details.phoneIsNew);
                        ps.setString(11, details.howLongHaveUsedPhone);
                        ps.setString(12, details.averageIncome);
                        ps.setBoolean(13, details.haveAjob);
                        ps.setBoolean(14, details.selfEmployed);
                        ps.setBoolean(15, details.student);
                        ps.setBoolean(16, details.haveNoIncome);
                        ps.setString(17, details.purposeOfLoan);
                        ps.setString(18, details.kindOfExpenseForLoan);
                        ps.setString(19, details.descriptionForPurposeOfLoan);
                        ps.setBoolean(20, details.anyOutstandingLoan);

                        ps.executeUpdate();

                        //Insert Login credentials in User Table
                        String insertLoginsSQL = "INSERT INTO dbo.LoanUserManagements VALUES (?, ?)";
                        PreparedStatement prepStmt = databaseConnection.prepareStatement(insertLoginsSQL);
                        prepStmt.setString(1, details.IDnumber);
                        prepStmt.setString(2, details.password);
                        prepStmt.executeUpdate();


                    } catch (Exception e) {

                        e.printStackTrace();

                    }

                }).match(HttpResponse.class, equity -> {
                    logger.info("Response from  [ {} ]", equity.entity().toString());

                }).match(Throwable.class, t -> {
                    logger.info("Exception [ {} ]", t.getMessage());


                })
                .build();
    }
}
