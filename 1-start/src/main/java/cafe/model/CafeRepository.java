package cafe.model;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

import cafe.model.entity.Coffee;

@ApplicationScoped
public class CafeRepository {

	private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
	private List<Coffee> coffeeList = null;
	private AtomicLong counter;
	
	public CafeRepository() {
	    coffeeList = new ArrayList<Coffee>();
	    counter = new AtomicLong();
	    
	    persistCoffee(new Coffee("Coffee 1", 10.0));
	    persistCoffee(new Coffee("Coffee 2", 20.0));
	}
	
	public List<Coffee> getAllCoffees() {
		logger.log(Level.INFO, "Finding all coffees.");

		return coffeeList;
	}

	public Coffee persistCoffee(Coffee coffee) {
		logger.log(Level.INFO, "Persisting the new coffee {0}.", coffee);
		
		coffee.setId(counter.incrementAndGet());
		coffeeList.add(coffee);
		return coffee;
	}

	public void removeCoffeeById(Long coffeeId) {
		logger.log(Level.INFO, "Removing a coffee {0}.", coffeeId);
		
		coffeeList.removeIf(coffee -> coffee.getId().equals(coffeeId));
	}

	public Coffee findCoffeeById(Long coffeeId) {
		logger.log(Level.INFO, "Finding the coffee with id {0}.", coffeeId);
		
		return coffeeList.stream().filter(coffee -> coffee.getId().equals(coffeeId)).findFirst().get();
	}
}
