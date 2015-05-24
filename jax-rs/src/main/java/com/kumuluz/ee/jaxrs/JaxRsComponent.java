package com.kumuluz.ee.jaxrs;

import com.kumuluz.ee.common.Component;
import com.kumuluz.ee.common.KumuluzServer;
import com.kumuluz.ee.common.utils.ClassUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;

/**
 * @author Tilen
 */
public class JaxRsComponent implements Component {

    private Logger log = Logger.getLogger(JaxRsComponent.class.getSimpleName());

    private KumuluzServer server;

    @Override
    public void init(KumuluzServer server) {

        this.server = server;
    }

    @Override
    public void load() {

        List<Class<?>> jaxRsApplications = ClassUtils.getClassesWithAnnotation(ApplicationPath
                .class);

        log.info("Scanning for JAX-RS applications");

        for (Class<?> jaxRsApp : jaxRsApplications) {

            log.info("Initiating JAX-RS application: " + jaxRsApp.getCanonicalName());

            Map<String, String> parameters = new HashMap<>();
            parameters.put("javax.ws.rs.Application", jaxRsApp.getCanonicalName());

            String pattern = jaxRsApp.getAnnotation(ApplicationPath.class).value();

            if (pattern.endsWith("/")) pattern += "*";
            else if (!pattern.endsWith("/*")) pattern += "/*";

            server.registerServlet(org.glassfish.jersey.servlet.ServletContainer.class,
                    pattern, parameters);
        }
    }

    @Override
    public String getComponentName() {

        return "JAX-RS";
    }

    @Override
    public String getImplementationName() {

        return "Jersey";
    }
}
