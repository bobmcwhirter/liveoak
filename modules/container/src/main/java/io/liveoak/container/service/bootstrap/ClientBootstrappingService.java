package io.liveoak.container.service.bootstrap;

import io.liveoak.client.DefaultClient;
import io.liveoak.container.service.ClientConnectorService;
import io.liveoak.container.service.ClientService;
import org.jboss.msc.service.*;

import static io.liveoak.spi.LiveOak.CLIENT;
import static io.liveoak.spi.LiveOak.server;

/**
 * @author Bob McWhirter
 */
public class ClientBootstrappingService implements Service<Void> {

    @Override
    public void start(StartContext context) throws StartException {
        ServiceTarget target = context.getChildTarget();

        ClientService client = new ClientService();
        target.addService(CLIENT, client)
                .install();

        ClientConnectorService clientConnector = new ClientConnectorService();
        target.addService(CLIENT.append("connect"), clientConnector)
                .addDependency(CLIENT, DefaultClient.class, clientConnector.clientInjector())
                .addDependency(server("local", false))
                .install();
    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }
}
