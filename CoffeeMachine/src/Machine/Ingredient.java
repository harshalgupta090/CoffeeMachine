package Machine;

public class Ingredient {

	private String name;
	private int max_quantity;

	private int current_quantity;
	private int low_threshold_value;
	
	public Ingredient(String _name, int _max_quantity) {
		name = _name;
		max_quantity = _max_quantity;
		current_quantity = _max_quantity;
		low_threshold_value = max_quantity/5;
	}
	
	String getName() {
		return name;
	}
	
	int getLow_threshold_value() {
		return low_threshold_value;
	}

	void setLow_threshold_value(int low_threshold_value) {
		this.low_threshold_value = low_threshold_value;
	}

	int getMax_quantity() {
		return max_quantity;
	}

	int getCurrent_quantity() {
		return current_quantity;
	}
	
	void use (int quantity) {
		this.current_quantity -= quantity;
	}
	
	boolean isSufficient(int quantity) {
		return this.current_quantity >= quantity;
	}
	
	void restockToFullCapacity(){
		current_quantity = max_quantity;
	}
	
	void restockByAmount(int amount) {
		current_quantity+=amount;
	}
	
	void setNewCurrentCapacity(int amount) {
		current_quantity = amount;
	}
	
	void adjustThreshold(int amount) {
		if (amount > low_threshold_value) {
			low_threshold_value = amount;
		}
	}
	
	boolean isRunningLow() {
		return this.current_quantity < this.low_threshold_value;
	}
}
