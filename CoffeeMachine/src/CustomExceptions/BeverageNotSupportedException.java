package CustomExceptions;

public class BeverageNotSupportedException extends Exception {
	public BeverageNotSupportedException(String bev) {
		super("Beverage: "+bev+" is not served by the machine.");
	}
}
