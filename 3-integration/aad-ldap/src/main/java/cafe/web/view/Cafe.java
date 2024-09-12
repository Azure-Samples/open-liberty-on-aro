package cafe.web.view;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import jakarta.security.enterprise.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import cafe.model.entity.Coffee;

@Named
@RequestScoped
public class Cafe implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

	private String baseUri;
	private transient Client client;

    @Inject
    private transient SecurityContext securityContext;

    @Inject
    private transient CafeRequestFilter filter;

	@NotNull
	@NotEmpty
	protected String name;
	@NotNull
	protected Double price;
	protected List<Coffee> coffeeList;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public List<Coffee> getCoffeeList() {
		this.getAllCoffees();
		return coffeeList;
	}

    public String getLoggedOnUser() {
        return securityContext.getCallerPrincipal().getName();
    }

    public boolean isDisabledForDeletion() {
        return !securityContext.isCallerInRole("admin");
    }

    public String getHostName() {
        return System.getenv("HOSTNAME");
    }

	@PostConstruct
	private void init() {
		try {
			HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();

			InetAddress inetAddress = InetAddress.getByName(request.getServerName());

            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            baseUri = context.getRequestScheme() + "://" + inetAddress.getHostName() + ":"
                + context.getRequestServerPort() + request.getContextPath() + "/rest/coffees";
            this.client = ClientBuilder.newBuilder().hostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                }).build().register(filter);
		} catch (IllegalArgumentException | NullPointerException | WebApplicationException | UnknownHostException ex) {
			logger.severe("Processing of HTTP response failed.");
			ex.printStackTrace();
		}
	}

	private void getAllCoffees() {
		this.coffeeList = this.client.target(this.baseUri).path("/").request(MediaType.APPLICATION_JSON)
            .get(new GenericType<List<Coffee>>() {
				});
	}

	public void addCoffee() throws IOException {
		Coffee coffee = new Coffee(this.name, this.price);
		this.client.target(baseUri).request(MediaType.APPLICATION_JSON).post(Entity.json(coffee));
		this.name = null;
		this.price = null;
		FacesContext.getCurrentInstance().getExternalContext().redirect("");
	}

	public void removeCoffee(String coffeeId) throws IOException {
		this.client.target(baseUri).path(coffeeId).request().delete();
		FacesContext.getCurrentInstance().getExternalContext().redirect("");
	}
}
