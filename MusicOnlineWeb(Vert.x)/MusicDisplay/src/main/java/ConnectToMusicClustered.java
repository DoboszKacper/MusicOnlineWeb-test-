import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class ConnectToMusicClustered extends AbstractVerticle {
    public static void main(String[] args) {
        ClusterManager mgr = new HazelcastClusterManager();
        VertxOptions options = new VertxOptions().setClusterManager(mgr);
        Vertx.clusteredVertx(options, vertxAsyncResult -> {
            if(vertxAsyncResult.succeeded()){
                Vertx vertx = vertxAsyncResult.result();
                vertx.deployVerticle(new ConnectToMusicClustered());
            }else {                System.out.println("Cluster Failed");
            }
        });
    }
    @Override
    public void start(){
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);
        discovery.getRecord(
                new JsonObject().put("name", "MusicApi"), found -> {
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
