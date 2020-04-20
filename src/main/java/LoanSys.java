package main.java;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.route;

public class LoanSys {

    final static ActorSystem system = ActorSystem.create("httpStream");
    final static Materializer material = ActorMaterializer.create(system);
    static final Logger log = LoggerFactory.getLogger(LoanSys.class);
    private final LoanSystemRoutes simpleRoutes;

    public static void main(String[] args) throws IOException, JSONException, ParseException {

        final Http http = Http.get(system);
        final LoanSys app = new LoanSys(system);
        final String host = "192.168.43.117";
        //final String host = "127.0.0.1";
        final int port = 807;


        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, material);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost(host, port), material);

       // MpesaServer.Mpesa.queryCRB("6","880000088","001",200,"1");
      //  MpesaServer.Mpesa.makeCRBQuery(5,"880000088","001",200,1);
      //  MpesaServer.Mpesa.payViaMpesa();

        System.out.println("The Server is running on " + host + ":" + port + "   Press RETURN to exit");
        System.in.read();

        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());
    }

    private LoanSys(final ActorSystem system) {
        simpleRoutes = new LoanSystemRoutes(system, material);
    }

    protected Route createRoute() {
        return route(simpleRoutes.createRoute());
    }
}
