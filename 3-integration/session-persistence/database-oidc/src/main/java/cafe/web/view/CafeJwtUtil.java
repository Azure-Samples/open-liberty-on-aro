package cafe.web.view;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

import com.ibm.websphere.security.social.UserProfileManager;

@SessionScoped
public class CafeJwtUtil implements Serializable {

    private static final long serialVersionUID = 1L;
    private String jwtToken;
    private List<String> groups;

    @SuppressWarnings("unchecked")
    public CafeJwtUtil() {
        if (UserProfileManager.getUserProfile() != null && UserProfileManager.getUserProfile().getIdToken() != null) {
            jwtToken = UserProfileManager.getUserProfile().getIdToken().compact();
            groups = UserProfileManager.getUserProfile().getIdToken().getClaims().getClaim("groups", List.class);
            groups = groups == null ? Collections.emptyList() : groups;

            setJwtAttr(jwtToken);
            setJwtGroupsClaimAttr(groups);
        } else {
            jwtToken = getJwtAttr();
            groups = getJwtGroupsClaimAttr();
        }
    }

    public String getJwt() {
        return jwtToken;
    }

    public List<String> getJwtGroupsClaim() {
        return groups;
    }

    public static void setJwtAttr(String jwtString) {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
            .getRequest();

        request.getSession().setAttribute("jwtToken", jwtString);
    }

    public static String getJwtAttr() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
            .getRequest();

        return (String) request.getSession().getAttribute("jwtToken");
    }

    public static void setJwtGroupsClaimAttr(List<String> groups) {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
            .getRequest();

        request.getSession().setAttribute("groups", String.join(",", groups));
    }

    public static List<String> getJwtGroupsClaimAttr() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
            .getRequest();

        return Arrays.asList(((String) request.getSession().getAttribute("groups")).split(","));
    }
}
