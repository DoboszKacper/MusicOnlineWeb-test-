package DatabaseConnection;

import Models.Customer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import java.util.ArrayList;
import java.util.List;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

public class CustomerDatabaseConnection extends AbstractVerticle {
    private static MySQLConnectOptions connectOptions;
    private static MySQLPool client;
    public Record publishedRecord;
    public ServiceDiscovery discovery;

    public static void main(String[] args) {
        Vertx v = Vertx.vertx();
        v.deployVerticle(new CustomerDatabaseConnection(),stringAsyncResult -> {
            v.deployVerticle(new ConnectToCustomer());
        });
    }

    public CustomerDatabaseConnection(){

        //Connect to the database
        connectOptions = new MySQLConnectOptions()
                .setPort(3309)
                .setHost("localhost")
                .setDatabase("customers")
                .setUser("root")
                .setPassword("123");


        //Pool options
        PoolOptions poolOptions = new PoolOptions();

        //Create the client pool
        client = MySQLPool.pool(connectOptions,poolOptions);
    }

    @Override
    public void start(Future<Void> future) {
        //Create server and router
        Router router = Router.router(vertx);

        //GET
        Route getAll = router
                .get("/")
                .handler(this::SelectAllCustomers);
        getAll.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetAll unavailable code:" + statusCode);
        });

        discovery = ServiceDiscovery.create(vertx,new ServiceDiscoveryOptions(new JsonObject()
                .put("host","localhost")
                .put("port","8081")
        ));
        //Create Server
        HttpServer server = vertx.createHttpServer();
        server
                .requestHandler(router)
                .listen(8081, "localhost", ar -> {
                    if (ar.succeeded()) {
                        discovery.publish(HttpEndpoint.createRecord("CustomerApi","localhost",8081,"/"),published->{
                            if(published.succeeded()){
                                publishedRecord = published.result();
                                future.complete();
                                System.out.println("Successful published");
                            }else{
                                System.out.println("Unpublished");
                                future.failed();

                            }
                        });
                        System.out.println("API is successful running " + 8081);
                    } else {
                        System.out.println("Api can't be run " + 8081);
                        future.fail(ar.cause());
                    }
                });




    }

    public void SelectAllCustomers(RoutingContext routingContext) {

        client.query("SELECT * FROM customers",res->{
            if(res.succeeded()){
                List<Customer> customers = new ArrayList<>();
                JsonArray jsonUsers = new JsonArray(customers);
                RowSet<Row> result = res.result();
                for (Row row : result) {
                    Customer customer = new Customer();
                    customer.setName(row.getString(1));
                    customer.setLastName(row.getString(2));
                    customer.setAge(row.getInteger(3));
                    customer.setPhoneNumber(row.getString(4));
                    customer.setId(row.getInteger(0));
                    customers.add(customer);
                }
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(200)
                        .end(jsonUsers.encodePrettily());
            }else{
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }
}
