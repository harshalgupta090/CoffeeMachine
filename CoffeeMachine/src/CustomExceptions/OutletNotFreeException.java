package CustomExceptions;

public class OutletNotFreeException extends Exception {

	public OutletNotFreeException(int outlet) {
		super("Outlet " + outlet + " is busy serving. Please choose an available outlet or wait for outlet to get free");
	}
}
