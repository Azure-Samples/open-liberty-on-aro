package cafe.web.view;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

@SessionScoped
public class CafeRequestFilter implements Serializable, ClientRequestFilter {

    private static final long serialVersionUID = 1L;

    @Inject
    private transient CafeJwtUtil jwtUtil;

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.getJwtToken());
    }
}
