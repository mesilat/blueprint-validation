package com.mesilat.vbp.impl;

import com.atlassian.annotations.security.XsrfProtectionExcluded;
import com.atlassian.mywork.model.Notification;
import com.atlassian.mywork.model.NotificationBuilder;
import com.atlassian.mywork.service.LocalNotificationService;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import static com.mesilat.vbp.Constants.PLUGIN_KEY;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/test")
@Scanned
public class TestResource {
    private static final Logger LOGGER = LoggerFactory.getLogger("extratrace");

    @ComponentImport
    private final LocalNotificationService notificationService;
    //@ComponentImport
    //private final UserAccessor userAccessor;

    @POST
    @Path("/notification")
    @XsrfProtectionExcluded
    // @Consumes(MediaType.APPLICATION_JSON) // prevents XSRF !
    public Response post() throws InterruptedException, ExecutionException {
        sendNotification("admin", "Тест", "Это тестовое сообщение");
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @POST
    @Path("/logger")
    @XsrfProtectionExcluded
    // @Consumes(MediaType.APPLICATION_JSON) // prevents XSRF !
    public Response logger() throws InterruptedException, ExecutionException {
        LOGGER.info("2 This is a test INFO message");
        LOGGER.debug("2 This is a test DEBUG message");
        LOGGER.trace("2 This is a test TRACE message");
        
        return Response.status(Response.Status.ACCEPTED).build();
    }

    /**
     * Create a single notification and send it to user
     * @param user the user who will receive the notification
     * @param title the title of the notification
     * @param message the body of the notification
     * @return the created notification
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private Notification sendNotification(final String user, final String title, final String message) throws InterruptedException, ExecutionException {
        Notification notification = notificationService.createOrUpdate(user, new NotificationBuilder()
            .application(PLUGIN_KEY) // a unique key that identifies your plugin
            .title("Message from your beloved administrator")
            .itemTitle(title)
            .description(message)
            .groupingId("vbp.notification") // a key to aggregate notifications
            .createNotification()).get();
    return notification;
}
    
    @Inject
    public TestResource(LocalNotificationService notificationService/*, UserAccessor userAccessor*/) {
        this.notificationService = notificationService;
        //this.userAccessor = userAccessor;
    }
}
