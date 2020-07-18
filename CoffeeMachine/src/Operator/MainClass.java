package Operator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import Machine.Machine;

/*
 * This class handles the testing and usage of the coffee machine. It starts with creating an instance of Machine class. Only one instance will ever be created in the lifetime of program.
 * it also provide the functional test cases for the main flows and different scenarios.
 * The machine is designed to handle n jobs in parallel and user can get result on any of the outlets. 
 */
public class MainClass {

	static Random random = new Random();
	static Machine myCoffeeMachine;
	static String[] beverages;
	static int outlet_count;
	
//	static class Order implements Runnable {
//
//		Machine machine;
//		String beverage;
//		int outlet;
//		
//		public Order(Machine _machine, String _name, int _outlet) {
//			machine = _machine;
//			beverage = _name;
//			outlet = _outlet;
//		}
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			String result = this.machine.getBeverage(beverage);
//			System.out.println(result);
//		}
//	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String settingsFilePath = "resources/settings.json";
		myCoffeeMachine = Machine.GetOrCreateMachine(settingsFilePath);	
		beverages = myCoffeeMachine.getBeverageOptions();
		outlet_count  = myCoffeeMachine.getOutletCount();
		fullRandomCaseTest();
//		runLargeParallelOrdersTest();
//		ingredientsRunningLowTest();
//		insufficientIngredientsTest();
//		restockIngredientsByAmountTest();
//		addUnvailableIngredientTest();
//		BeverageNotAvailableTest("masala_tea");
	}
	
	//Test case Functions;
	
	//It simulates a normal operation of machine based on user input. You can choose to order any beverage and see different scenarios.
	public static void fullRandomCaseTest() {
		System.out.println("Please Provide a number input for a beverage you want to have.");
		for (int i=0; i<beverages.length; i++) {
			System.out.print("input: " + (i+1) + " for " + beverages[i]+ " ");
		}
		System.out.println("\nInput: " + (beverages.length+1) + " to check status of all ingredients");
		System.out.println("Input: " + (beverages.length+2) + " to restock all ingredients");
		System.out.println("Input: 0 to exit.");
		
		int input;
		Scanner in = new Scanner(System.in);
		boolean loopbreak = false;
		while (true) {
			input = in.nextInt();
			if (input == 0) {
				in.close();
				break;
			}
			if (input == beverages.length+1) {
				myCoffeeMachine.fullRestockToInitialCapacity(null);
				continue;
			}
			if (input == beverages.length+2) {
				System.out.println(myCoffeeMachine.getAvailableIngredients());
				continue;
			}
			if (input > beverages.length+1 || input < 0) {
				System.out.println("this is not a valid option.");
				continue;
			}
			myCoffeeMachine.placeOrder(beverages[input-1]);
		}
	}
	
	//Run more orders parallel than num_outlets. Result should be that orders placed after all the outlets are busy should be rejected;
	public static void runLargeParallelOrdersTest() throws InterruptedException {
		System.out.println("\n\n\n Running runLargeParallelOrdersTest \n\n\n");
		Thread.sleep(2000);
		myCoffeeMachine.SetFullCapacityToInfinite();
		for (int j=0; j<3;j++) {
			for (int i=0; i<outlet_count+6; i++) {
				myCoffeeMachine.placeOrder(beverages[0]);
				Thread.sleep(random.nextInt(1000));
			}	
			System.out.println("Testing again. sleep to wait for all previous orders to complete");
			Thread.sleep(10000);
		}
	}
	
	/*
	 * This testcase try to simulate condition where a beverage is ordered and then a call is made to check if some ingredients are running low. 
	 * this is repeated until some ingredients start running low.
	 * Response should be a map containing the low ingredients, their current quantity and their max_capacity in machine.
	 * All the ingredients are then restocked to their full capacity to simulate the initial state.
	 * The above case is then run for all beverages for more exhaustive testing.
	 * It should be noted that it is possible for  beverages to continue to be prepared even after some of the required ingredients are running low. as running low does not mean insufficient. 
	 */
	public static void ingredientsRunningLowTest() throws InterruptedException {
		System.out.println("\n\n\n Running ingredientsRunningLowTest \n\n\n");
		Thread.sleep(2000);
		Map<String, int[]> lowIngredients = myCoffeeMachine.getIngredientsRunningLow();
		boolean ingredientNotFound = false;
		for (int i=0; i<beverages.length; i++) {
			while (lowIngredients.isEmpty()) {
				String result = myCoffeeMachine.placeOrderSerially(beverages[i]);
				if (result.contains(" not available")) {
					System.out.println(result+"\n");
					ingredientNotFound = true;
					break;
				}
				lowIngredients = myCoffeeMachine.getIngredientsRunningLow();
			}
			if (ingredientNotFound) {
				ingredientNotFound = false;
				continue;
			}
			System.out.println("The following ingredients are running low: ");
			for (Entry<String, int[]>e: lowIngredients.entrySet()) {
				System.out.println("Ingredient: " + e.getKey() + " Current Quantity: " + e.getValue()[0] + " Max Capacity: " + e.getValue()[1]);
			}
			System.out.println();
			System.out.println(myCoffeeMachine.placeOrderSerially(beverages[i]));
			myCoffeeMachine.fullRestockToInitialCapacity(null);
			lowIngredients.clear();
		}

	}
	
	//This test checks the case when there are insufficient ingredients of a beverage. It should stop serving and only should serve when the ingredients are restocked. 
	public static void insufficientIngredientsTest() throws InterruptedException {
		System.out.println("\n\n\n Running insufficientIngredientsTest \n\n\n");
		Thread.sleep(2000);
		boolean ingredientNotFound = false;
		for (int i=0; i<beverages.length; i++) {
			while (true) {
				String result = myCoffeeMachine.placeOrderSerially(beverages[i]);
				if (result.contains(" not sufficient")) {
					System.out.println(result+"\n");
					ingredientNotFound = true;
					break;
				}
			}
			if (ingredientNotFound) {
				ingredientNotFound = false;
				continue;
			}
			System.out.println();
			myCoffeeMachine.fullRestockToInitialCapacity(null);
			System.out.println(myCoffeeMachine.placeOrderSerially(beverages[i]));
			Thread.sleep(1000);
		}
	}
	
	//check to see what happens when a unsuported beverage is pased as a param to place order method. response should be order rejection with appropriate message.
	public static void BeverageNotAvailableTest(String unsupportedBeverage) throws InterruptedException {
		System.out.println("\n\n\n Running BeverageNotAvailableTest \n\n\n");
		Thread.sleep(2000);
		myCoffeeMachine.placeOrder(unsupportedBeverage);
	}
	
	/*
	 * this method tests the case where user is stocking the ingredients by a perticular amount.
	 * A beverage is orders until its ingredients are running low. 
	 * Once the restocking is done, we should again get the order successfully
	 */
	public static void restockIngredientsByAmountTest() throws InterruptedException {
		System.out.println("\n\n\n Running restockIngredientsByAmountTest \n\n\n");
		Thread.sleep(2000);
		Map<String, int[]> lowIngredients = myCoffeeMachine.getIngredientsRunningLow();
		boolean ingredientNotFound = false;
		for (int i=0; i<beverages.length; i++) {
			while (lowIngredients.isEmpty()) {
				String result = myCoffeeMachine.placeOrderSerially(beverages[i]);
				if (result.contains(" not available")) {
					System.out.println(result+"\n");
					ingredientNotFound = true;
					break;
				}
				System.out.println(result);
				lowIngredients = myCoffeeMachine.getIngredientsRunningLow();
			}
			if (ingredientNotFound) {
				ingredientNotFound = false;
				continue;
			}
			System.out.println("The following ingredients are running low: ");
			for (Entry<String, int[]>e: lowIngredients.entrySet()) {
				System.out.println("Ingredient: " + e.getKey() + " Current Quantity: " + e.getValue()[0] + " Max Capacity: " + e.getValue()[1]);
			}
			Map<String, Integer>refillingMap = new HashMap<>();
			for (Entry<String, int[]>e: lowIngredients.entrySet()) {
				int val = e.getValue()[1]-e.getValue()[0];
				refillingMap.put(e.getKey(), val);
				System.out.printf("Restocking %s by adding %d\n", e.getKey(), val);
			}
			myCoffeeMachine.restockIngredientsByAmount(refillingMap);
			System.out.println();
			System.out.printf("Trying again to get %s\n", beverages[i]);
			System.out.println(myCoffeeMachine.placeOrderSerially(beverages[i]));
			System.out.println();
			lowIngredients.clear();
		}
	}
	 
	/*
	 *  this function tests the case where an ingredient is missing in the available list but is required to create a beverage.
	 *  it calls place order on all the beverages in loop until we get to the one which cannot be created because of missing ingredient.
	 *  Then we add the missing ingredient.
	 *  We try to get the beverage again. it should get served successfully now.
	 */
	
	public static void addUnvailableIngredientTest() throws InterruptedException {
		System.out.println("\n\n\n Running addUnvailableIngredientTest \n\n\n");
		Thread.sleep(2000);
		for (String beverage: beverages) {
			String result = myCoffeeMachine.placeOrderSerially(beverage);
			if (result.contains("is not available")) {
				System.out.println(result);
				String[] strs = result.split(" ");
				String ing = strs[6];
				System.out.printf("adding Ingredient: %s. Total amount will be 100\n", ing);
				myCoffeeMachine.addUnavailableIngredientWithMaxCapacity(ing, 100);
				System.out.println("We should be able to get " + beverage);
				System.out.println(myCoffeeMachine.placeOrderSerially(beverage));
				break;
			}
		}
	}
}
