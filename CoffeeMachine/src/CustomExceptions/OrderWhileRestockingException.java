package CustomExceptions;

public class OrderWhileRestockingException extends Exception {
	
	public OrderWhileRestockingException(int timeleft) {
		super("Maching is restocking ingredients. Please wait for " + timeleft + " seconds");
	}
}
