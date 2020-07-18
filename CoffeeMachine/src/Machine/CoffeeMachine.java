package Machine;

import java.util.Map;

public interface CoffeeMachine {

	public String getBeverage(String beverage, int outlet);
	public void restockIngredientsByAmount(Map<String, Integer> ingredients);
	public void fullRestockToInitialCapacity(String[] ingredients);
	public void addUnavailableIngredientWithMaxCapacity(String ingredient, int max_capacity);
	public Map<String, int[]> getIngredientsRunningLow();
	public String[] getBeverageOptions();
	public int getOutletCount();
	public Map<String, Integer>getAvailableIngredients();
}
