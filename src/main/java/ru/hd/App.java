package ru.hd;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hd.gui.view.MainView;

import java.io.File;

@Theme(value = "main-theme", variant = Lumo.DARK)
public class App implements AppShellConfigurator {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

        WebAppContext context = getWebAppContext();

        ServletHolder vaadinServlet = new ServletHolder(VaadinServlet.class);
        vaadinServlet.setInitParameter("ui", MainView.class.getName());
        context.addServlet(vaadinServlet, "/*");

        server.setHandler(context);
        server.start();
        logger.info("Server started!");
        server.join();

    }

    private static WebAppContext getWebAppContext() {
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setResourceBase(new File("target/classes/META-INF/VAADIN/webapp").getAbsolutePath());

        // Отключаем устаревшие слушатели
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*");
        context.setConfigurationDiscovered(true);

        context.setConfigurations(new Configuration[]{
                new JettyWebXmlConfiguration(),
                new AnnotationConfiguration(),
                new WebInfConfiguration(),
                new MetaInfConfiguration()
        });
        return context;
    }
}