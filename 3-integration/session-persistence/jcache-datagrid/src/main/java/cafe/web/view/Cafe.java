package cafe.web.view;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
	private String baseUri;
	private transient Client client;

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

    public String getHostName() {
        return "true".equals(System.getenv("SHOW_HOST_NAME")) ? System.getenv("HOSTNAME") : "";
    }

	@PostConstruct
	private void init() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
            .getRequest();
        name = (String) request.getSession().getAttribute("coffeeName");
        price = (Double) request.getSession().getAttribute("coffeePrice");

        baseUri = "http://localhost:9080" + request.getContextPath() + "/rest/coffees";
        this.client = ClientBuilder.newBuilder().build();
	}

	private void getAllCoffees() {
		this.coffeeList = this.client.target(this.baseUri).path("/").request(MediaType.APPLICATION_JSON)
            .get(new GenericType<List<Coffee>>() {
				});
	}

	public void addCoffee() throws IOException {
		Coffee coffee = new Coffee(this.name, this.price);
		this.client.target(baseUri).request(MediaType.APPLICATION_JSON).post(Entity.json(coffee));
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
            .getRequest();
		request.getSession().setAttribute("coffeeName", this.name);
		request.getSession().setAttribute("coffeePrice", this.price);
		FacesContext.getCurrentInstance().getExternalContext().redirect("");
	}

	public void removeCoffee(String coffeeId) throws IOException {
		this.client.target(baseUri).path(coffeeId).request().delete();
		FacesContext.getCurrentInstance().getExternalContext().redirect("");
	}
}
