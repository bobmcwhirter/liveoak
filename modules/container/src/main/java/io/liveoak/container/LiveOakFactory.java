/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container;

import io.liveoak.container.service.bootstrap.*;
import io.liveoak.spi.LiveOak;
import org.jboss.logging.Logger;
import org.jboss.msc.service.*;
import org.jboss.msc.value.ImmediateValue;
import org.vertx.java.core.Vertx;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;

import static io.liveoak.spi.LiveOak.*;

/**
 * Bootstrapping <code>main()</code> method.
 *
 * @author Bob McWhirter
 * @author Ken Finnigan
 */
public class LiveOakFactory {

    private static final Logger log = Logger.getLogger(LiveOakFactory.class);

    public static LiveOakSystem create() throws Exception {
        return create(null, null, null);
    }

    public static LiveOakSystem create(ServiceContainer serviceContainer, ServiceTarget serviceTarget) throws Exception {
        return new LiveOakFactory(null, null, null, null, serviceContainer, serviceTarget ).createInternal();
    }

    public static LiveOakSystem create(Vertx vertx) throws Exception {
        return create(null, null, vertx);
    }

    public static LiveOakSystem create(File configDir, File applicationsDir) throws Exception {
        return create(configDir, applicationsDir, null);
    }

    public static LiveOakSystem create(File configDir, File applicationsDir, Vertx vertx) throws Exception {
        return new LiveOakFactory(configDir, applicationsDir, vertx, "localhost").createInternal();
    }

    public static LiveOakSystem create(File configDir, File applicationsDir, Vertx vertx, String bindAddress) throws Exception {
        return new LiveOakFactory(configDir, applicationsDir, vertx, bindAddress).createInternal();
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------

    private LiveOakFactory(File configDir, File applicationsDir, Vertx vertx, String bindAddress) {
        this( configDir, applicationsDir, vertx, bindAddress, ServiceContainer.Factory.create() );
    }

    private LiveOakFactory(File configDir, File applicationsDir, Vertx vertx, String bindAddress, ServiceContainer serviceContainer) {
        this( configDir, applicationsDir, vertx, bindAddress, serviceContainer, serviceContainer.subTarget() );
    }

    private LiveOakFactory(File configDir, File applicationsDir, Vertx vertx, String bindAddress, ServiceContainer serviceContainer, ServiceTarget serviceTarget) {
        this.configDir = configDir;
        System.err.println( "configDir: " + this.configDir );
        this.appsDir = applicationsDir;
        System.err.println( "appsDir: " + this.appsDir );
        this.bindAddress = bindAddress;
        System.err.println( "bindAddress: " + this.bindAddress );
        this.serviceContainer = serviceContainer;
        System.err.println( "serviceContainer: " + this.serviceContainer );
        this.serviceTarget = serviceTarget;
        System.err.println( "serviceTarget: " + this.serviceTarget );

        this.serviceTarget.addListener(new AbstractServiceListener<Object>() {
            @Override
            public void transition(ServiceController<?> controller, ServiceController.Transition transition) {
                if (transition.getAfter().equals(ServiceController.Substate.START_FAILED)) {
                    log.errorf(controller.getStartException(), "Unable to start service: %s", controller.getName());
                    controller.getStartException().printStackTrace();
                }
            }
        });

        this.stabilityMonitor = new StabilityMonitor();
        this.serviceTarget.addMonitor( this.stabilityMonitor );
    }

    public LiveOakSystem createInternal() throws Exception {

        LiveOakSystem system = new LiveOakSystem(serviceContainer);
        serviceContainer.addService(LIVEOAK, new ValueService<LiveOakSystem>(new ImmediateValue<>(system)))
                .install();

        ServiceName name = LiveOak.LIVEOAK.append( "factory" );

        if ( this.appsDir == null )  {
            this.appsDir = Files.createTempDirectory( "liveoak-apps" ).toFile();
        }

        TenancyBootstrappingService tenancy = new TenancyBootstrappingService();
        this.serviceTarget.addService(name.append("tenancy"), tenancy)
                .addInjection(tenancy.applicationsDirectoryInjector(), this.appsDir.getAbsolutePath())
                .install();

        ValueService<InetSocketAddress> socketBinding = new ValueService<InetSocketAddress>( new ImmediateValue<>( new InetSocketAddress( "localhost", 8080 )));
        this.serviceTarget.addService(LiveOak.SOCKET_BINDING, socketBinding )
                .install();
        this.serviceTarget.addService(name.append("servers"), new ServersBootstrappingService()).install();
        this.serviceTarget.addService(name.append("codecs"), new CodecBootstrappingService()).install();
        this.serviceTarget.addService(name.append("client"), new ClientBootstrappingService()).install();

        ExtensionsBootstrappingService extensions = new ExtensionsBootstrappingService();
        this.serviceTarget.addService(name.append("extensions"), extensions)
                .addInjection(extensions.extensionsDirectoryInjector(), new File(this.configDir, "extensions").getAbsolutePath())
                .install();

        this.serviceTarget.addService(LiveOak.SERVICE_REGISTRY, new ValueService<ServiceRegistry>(new ImmediateValue<>(this.serviceContainer) ) )
                .install();

        this.serviceTarget.addService(name.append("vertx"), new VertxBootstrappingService())
                .install();

        this.stabilityMonitor.awaitStability();
        return (LiveOakSystem) this.serviceContainer.getService(LIVEOAK).awaitValue();
    }

    protected void prolog() {
        serviceContainer.addService(SERVICE_REGISTRY, new ValueService<ServiceRegistry>(new ImmediateValue<>(serviceContainer)))
                .install();

        serviceContainer.addService(SERVICE_CONTAINER, new ValueService<ServiceRegistry>(new ImmediateValue<>(serviceContainer)))
                .install();

    }

    private final File configDir;
    private File appsDir;
    private final String bindAddress;
    private final ServiceContainer serviceContainer;
    private final ServiceTarget serviceTarget;
    private final StabilityMonitor stabilityMonitor;

}
