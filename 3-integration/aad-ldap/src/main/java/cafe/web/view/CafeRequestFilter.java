package cafe.web.view;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.logging.Logger;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.InvalidBuilderException;
import com.ibm.websphere.security.jwt.InvalidClaimException;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtException;

@SessionScoped
public class CafeRequestFilter implements Serializable, ClientRequestFilter {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private String jwtTokenString;
    
    public CafeRequestFilter() {
        try {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();

            jwtTokenString = JwtBuilder.create("jwtAuthUserBuilder").claim(Claims.SUBJECT, "javaee-cafe-rest-endpoints")
                .claim("upn", request.getUserPrincipal().getName())
                .claim("groups", request.isUserInRole("admin") ? "admin" : Collections.emptyList()).buildJwt()
                .compact();
        } catch (JwtException | InvalidBuilderException | InvalidClaimException e) {
            logger.severe("Creating JWT token failed.");
            e.printStackTrace();
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenString);
    }
}
