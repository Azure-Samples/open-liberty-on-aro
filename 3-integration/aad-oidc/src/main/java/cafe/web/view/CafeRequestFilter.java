package cafe.web.view;

import java.io.Serializable;

import jakarta.inject.Inject;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

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
