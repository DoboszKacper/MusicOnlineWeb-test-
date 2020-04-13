package DatabaseConnection;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;

public class ConnectToCustomer extends AbstractVerticle {
    @Override
    public void start(){
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);
        discovery.getRecord(
                new JsonObject().put("name", "CustomerApi"), found -> {
                    if(found.succeeded()) {
                        Record match = found.result();
                        ServiceReference reference = discovery.getReference(match);
                        HttpClient client = reference.get();

                        client.getNow("/", response ->
                                response.bodyHandler(
                                        body ->
                                                System.out.println(body.toString())));
                    }else {
                        System.out.println("unsuccessful connection");
                    }
                });

    }
}
