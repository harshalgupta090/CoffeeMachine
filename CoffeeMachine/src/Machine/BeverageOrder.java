package Machine;

public class BeverageOrder implements Runnable {

	private Machine machine;
	private String beverage;
	public BeverageOrder(Machine _machine, String _beverage) {
		machine = _machine;
		beverage = _beverage;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println(machine.getBeverage(beverage));
		
	}

}
