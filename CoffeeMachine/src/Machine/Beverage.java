package Machine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import CustomExceptions.IngredientNotAvailableException;
import CustomExceptions.NotEnoughIngredientException;

/*
 *A generic beverage class. The object of this class stores the ingredient requirement info of their beverage type. 
 *This class provides functionality to check whether the current qunatity in the machine of required ingredients for the beverage is sufficient or not. 
 *It also provides functionality to deduct the required amount of ingredient quantity from their current quantity in machine when an order is successfully executed. 
 */

public class Beverage {
	private Map<String, Integer>required_ingredients;
	private final String name;
	
	public Beverage(String _name) {
		this.required_ingredients = new HashMap<>();
		this.name = _name;
	}
	
	public void addIngredient(String ing, int quan) {
		this.required_ingredients.put(ing, quan);
	}
	
	//this method takes care of deducting the required amount of ingredients used from their current quantity in the machine. 
	//It is only called if the order is finally committed to be completed after all checks.
	void prepare(Map<String, Ingredient> availableIngredients) {
		// TODO Auto-generated method stub
		Iterator<Entry<String, Integer>>it = this.required_ingredients.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer>ing = it.next();
			availableIngredients.get(ing.getKey()).use(ing.getValue());
		}
	}

	//checks if all the required ingredients are available and sufficient to prepare this beverage. Throws exception otherwise.
	boolean verify(Map<String, Ingredient>ingredients) throws Exception{
		Iterator<Entry<String, Integer>>it = this.required_ingredients.entrySet().iterator();
		boolean isPossible = true;
		while (it.hasNext()) {
			Entry<String, Integer>ing = it.next();
			if (!ingredients.containsKey(ing.getKey())) {
				throw new IngredientNotAvailableException(this.name, ing.getKey());
			} else if (!ingredients.get(ing.getKey()).isSufficient(ing.getValue())) {
				throw new NotEnoughIngredientException(this.name, ing.getKey(), ing.getValue());
			}
		}
		return isPossible;
	}
	
	
}
