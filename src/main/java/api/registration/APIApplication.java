package api.registration;


import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;

public class APIApplication extends ResourceConfig {
   // @SuppressWarnings("deprecation")
	public APIApplication() {
        //加载Resource
        register(demo.HelloResource.class);
        // Logging.
    //    register(LoggingFilter.class);
        //invite business partners
        register(invitation.InvitationBP.class);
        //bp list
        register(invitation.BPList.class);
        //userManagement
        register(administration.UserManagement.class);
        //roles
        register(administration.Roles.class);
    } 
} 