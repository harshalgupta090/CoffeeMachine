package CustomExceptions;

public class NotEnoughIngredientException extends Exception {
	public NotEnoughIngredientException(String beverage, String ingredient) {
		super(beverage + " can not be prepared because "+ ingredient + " is not sufficient.");
	}
}
