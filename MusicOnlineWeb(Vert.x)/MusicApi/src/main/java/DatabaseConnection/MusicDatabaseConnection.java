package DatabaseConnection;

import Models.Artist;
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
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.types.RedisDataSource;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import java.util.ArrayList;
import java.util.List;

public class MusicDatabaseConnection extends AbstractVerticle {
    private static MySQLConnectOptions connectOptions;
    private static MySQLPool client;
    public ServiceDiscovery discovery;
    public Record musicRecord;

    public static void main(String[] args) {
        Vertx v = Vertx.vertx();
        v.deployVerticle(new MusicDatabaseConnection());
    }

    public MusicDatabaseConnection() {

        //Connect to the database
        connectOptions = new MySQLConnectOptions()
                .setPort(3308)
                .setHost("localhost")
                .setDatabase("artists")
                .setUser("root")
                .setPassword("123");

        //Pool options
        PoolOptions poolOptions = new PoolOptions();

        //Create the client pool
        client = MySQLPool.pool(connectOptions,poolOptions);
    }

    @Override
    public void start(Future<Void> future) {

        //Creating server and router
        Router router = Router.router(vertx);

        //GET
        Route getAll = router
                .get("/")
                .handler(this::SelectAllArtists);
        getAll.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetAll unavailable code:"+statusCode);
        });


        discovery = ServiceDiscovery.create(vertx,
                new ServiceDiscoveryOptions(new JsonObject()
                    .put("host","localhost")
                        .put("port","8082")
                ));

        HttpServer server = vertx.createHttpServer();
        server
                .requestHandler(router)
                .listen(8082, "localhost", ar -> {
                    if (ar.succeeded()) {
                        discovery.publish(HttpEndpoint.createRecord("MusicApi","localhost",8082,"/"),published->{
                            if(published.succeeded()){
                                musicRecord = published.result();
                                future.complete();
                                System.out.println("Successful published");
                            }else{
                                System.out.println("Unpublished");
                                future.failed();

                            }
                        });
                        System.out.println("API is successful running " + 8082);
                    } else {
                        System.out.println("Api can't be run " + 8082);
                        future.fail(ar.cause());
                    }
                });
    }

    public void SelectAllArtists(RoutingContext routingContext) {
        client.query("SELECT * FROM artists",res->{
            if(res.succeeded()){
                RowSet<Row> result = res.result();
                List<Artist> list = new ArrayList<>();
                JsonArray jsonHolidays = new JsonArray(list);
                for (Row row : result) {
                    Artist artist = new Artist();
                    artist.setId(row.getInteger(0));
                    artist.setName(row.getString(1));

                    list.add(artist);
                }
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(200)
                        .end(jsonHolidays.encodePrettily());
            }else{
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }
}
