package CustomExceptions;

public class NotEnoughIngredientException extends Exception {
	public NotEnoughIngredientException(String beverage, String ingredient, int quantity) {
		super(beverage + " can not be prepared because "+ ingredient + " is not sufficient. Need minimum " + quantity);
	}
}
