package CustomExceptions;

public class OutletNotFreeException extends Exception {

	public OutletNotFreeException() {
		super("All Outlets are busy serving. Please wait for an outlet to get free");
	}
}
