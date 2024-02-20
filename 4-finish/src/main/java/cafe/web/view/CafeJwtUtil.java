package cafe.web.view;

import java.io.Serializable;
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.websphere.security.social.UserProfileManager;

@SessionScoped
public class CafeJwtUtil implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    @ConfigProperty(name = "admin.group.id")
    private String ADMIN_GROUP_ID;

    public String getJwtToken() {
        return UserProfileManager.getUserProfile().getIdToken().compact();
    }

    public boolean isUserInAdminGroup() {
        @SuppressWarnings("unchecked")
            List<String> groups = UserProfileManager.getUserProfile().getIdToken().getClaims().getClaim("groups",
                                                                                                        List.class);

        return groups != null && groups.contains(ADMIN_GROUP_ID);
    }
}
