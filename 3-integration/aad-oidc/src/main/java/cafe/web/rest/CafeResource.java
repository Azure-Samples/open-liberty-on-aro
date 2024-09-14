package cafe.web.rest;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import cafe.model.CafeRepository;
import cafe.model.entity.Coffee;

@Path("coffees")
public class CafeResource {

	@Inject
	private CafeRepository cafeRepository;

    @Inject
    @ConfigProperty(name = "admin.group.id")
    private String ADMIN_GROUP_ID;

    @Inject
    private JsonWebToken jwtPrincipal;
    
	@GET
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public List<Coffee> getAllCoffees() {
		return this.cafeRepository.getAllCoffees();
	}

	@POST
	@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })	
	public Coffee createCoffee(Coffee coffee) {
		return this.cafeRepository.persistCoffee(coffee);
	}

	@GET
	@Path("{id}")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Coffee getCoffeeById(@PathParam("id") Long coffeeId) {
		return this.cafeRepository.findCoffeeById(coffeeId);
	}

	@DELETE
	@Path("{id}")
	public void deleteCoffee(@PathParam("id") Long coffeeId) {
        if (!this.jwtPrincipal.getGroups().contains(ADMIN_GROUP_ID)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        
		this.cafeRepository.removeCoffeeById(coffeeId);
	}
}