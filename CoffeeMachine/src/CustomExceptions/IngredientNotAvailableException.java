package CustomExceptions;

public class IngredientNotAvailableException extends Exception {
	public IngredientNotAvailableException(String beverage, String ingredient) {
		super(beverage + " can not be prepared because "+ ingredient + " is not available.");
	}
}
