package io.liveoak.security.extension;

import io.liveoak.interceptor.service.InterceptorRegistrationHelper;
import io.liveoak.security.integration.AuthzServiceRootResource;
import io.liveoak.security.interceptor.AuthzInterceptor;
import io.liveoak.security.interceptor.AuthzInterceptorService;
import io.liveoak.security.service.AuthzConfigResourceService;
import io.liveoak.security.service.AuthzResourceService;
import io.liveoak.spi.LiveOak;
import io.liveoak.spi.client.Client;
import io.liveoak.spi.extension.ApplicationExtensionContext;
import io.liveoak.spi.extension.Extension;
import io.liveoak.spi.extension.SystemExtensionContext;
import io.liveoak.spi.resource.async.DefaultRootResource;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.io.File;

/**
 * @author Bob McWhirter
 */
public class SecurityExtension implements Extension {
    @Override
    public void extend(SystemExtensionContext context) throws Exception {
        context.mountPrivate(new DefaultRootResource(context.id()));

        // Install AuthzInterceptor
        ServiceTarget target = context.target();
        AuthzInterceptorService authzInterceptor = new AuthzInterceptorService();
        ServiceController<AuthzInterceptor> authzController = target.addService(LiveOak.interceptor("authz"), authzInterceptor)
                .addDependency(LiveOak.CLIENT, Client.class, authzInterceptor.clientInjector())
                .install();
        InterceptorRegistrationHelper.installInterceptor(target, authzController);
    }

    @Override
    public void extend(ApplicationExtensionContext context) throws Exception {
        String appId = context.application().id();

        ServiceTarget target = context.target();

        AuthzResourceService resource = new AuthzResourceService(context.resourceId());
        target.addService(LiveOak.resource(appId, context.resourceId()), resource)
                .addDependency(LiveOak.CLIENT, Client.class, resource.clientInjector())
                .install();

        AuthzConfigResourceService configResource = new AuthzConfigResourceService(context.resourceId());
        target.addService(LiveOak.adminResource(appId, context.resourceId()), configResource)
                .addDependency(LiveOak.resource(appId, context.resourceId()), AuthzServiceRootResource.class, configResource.rootResourceInjector())
                .install();

        context.mountPublic();
        context.mountPrivate();
    }

    @Override
    public void unextend(ApplicationExtensionContext context) throws Exception {

    }
}
