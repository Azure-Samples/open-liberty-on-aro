package cafe.web.view;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.InvalidBuilderException;
import com.ibm.websphere.security.jwt.InvalidClaimException;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtException;
import com.ibm.websphere.security.openidconnect.PropagationHelper;
import com.ibm.websphere.security.openidconnect.token.IdToken;

@SessionScoped
public class CafeJwtUtil implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private String jwtTokenString;
    private List<String> groups;

    @Inject
    @ConfigProperty(name = "admin.group.id")
    private String ADMIN_GROUP_ID;

    @SuppressWarnings("unchecked")
    public CafeJwtUtil() {
        try {
            IdToken idToken = PropagationHelper.getIdToken();
            groups = idToken.getClaim("groups") != null ? (List<String>) idToken.getClaim("groups")
                    : Collections.emptyList();

            jwtTokenString = JwtBuilder.create("jwtAuthUserBuilder").claim(Claims.SUBJECT, "javaee-cafe-rest-endpoints")
                    .claim("upn", idToken.getClaim("preferred_username")).claim("groups", groups).buildJwt().compact();
        } catch (JwtException | InvalidBuilderException | InvalidClaimException e) {
            logger.severe("Creating JWT token failed.");
            e.printStackTrace();
        }
    }

    public String getJwtToken() {
        return jwtTokenString;
    }

    public boolean isUserInAdminGroup() {
        return groups.contains(ADMIN_GROUP_ID);
    }
}
