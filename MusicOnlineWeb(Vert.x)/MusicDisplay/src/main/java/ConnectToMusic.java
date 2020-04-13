import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.*;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;
import io.vertx.servicediscovery.types.HttpEndpoint;

public class ConnectToMusic extends AbstractVerticle {

    private static final JsonObject FALLBACK_JSON = new JsonObject().put("error","Opps MusicApi is not responding at the moment...");

    ServiceDiscovery discovery;
    CircuitBreaker circuitBreaker;
    HttpClient client;
    public static void main(String[] args) {
        Vertx v= Vertx.vertx();
        v.deployVerticle(new ConnectToMusic());
    }

    @Override
    public void start(){
        discovery = ServiceDiscovery.create(vertx,new ServiceDiscoveryOptions()
        .setBackendConfiguration(new JsonObject()
                .put("host","localhost")
                .put("port",8082)
                ));

        CircuitBreakerOptions options = new CircuitBreakerOptions()
                .setMaxFailures(2)
                .setTimeout(2000)
                .setResetTimeout(5000)
                .setFallbackOnFailure(true);

        circuitBreaker = CircuitBreaker.create("CBreaker",vertx,options).halfOpenHandler(v->{
            if(client!= null){
                client.close();
                client = null;
            }
        });

        Router router = Router.router(vertx);

        router.get("/").handler(ctx -> getClient(client -> {
            if (client == null) {

                ctx.response()
                        .putHeader("content-type", "application/json")
                        .end(FALLBACK_JSON.encode());
            } else {
                circuitBreaker.executeWithFallback(f->
                        client.get("/",re->{
                  re.bodyHandler(buffer -> ctx.response().putHeader("content-type", "application/json").end(buffer));
                  f.complete();
                }).exceptionHandler(f::fail).end()
                        ,
                        v->{
                            ctx.response()
                                    .putHeader("content-type", "application/json")
                                    .end(FALLBACK_JSON.encode());
                            return null;
                        });
                    }
            }));


        ServiceDiscoveryRestEndpoint.create(router,discovery);

        router.route().handler(StaticHandler.create());

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8083,ar->{
                    if(ar.succeeded()){
                        System.out.println("Server is running");
                    }else{
                        System.out.println("Server Error");
                    }
                });
    }

    private void getClient(Handler<HttpClient> resultHandler){
        if(client != null){
            resultHandler.handle(client);
        }else {
            HttpEndpoint.getClient(discovery,new JsonObject().put("name","MusicApi").put("host","localhost").put("port",8082), ar->{
                resultHandler.handle(ar.result());
            });
        }
    }
}
