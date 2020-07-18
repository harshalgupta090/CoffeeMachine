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
 * To simulate a machine where each outlet is independent, i have opted for a design where parallel orders are simulated by giving orders in different thread. 
 * I am using an executor service to place multiple orders at the same time.
 */
public class MainClass {

	static ExecutorService executor;
	static Random random = new Random();
	static Machine myCoffeeMachine;
	static String[] beverages;
	static int outlet_count;
	
	static class Order implements Runnable {

		Machine machine;
		String beverage;
		int outlet;
		
		public Order(Machine _machine, String _name, int _outlet) {
			machine = _machine;
			beverage = _name;
			outlet = _outlet;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			String result = this.machine.getBeverage(beverage, outlet);
			System.out.println(result);
		}
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String settingsFilePath = "resources/settings.json";
		myCoffeeMachine = Machine.GetOrCreateMachine(settingsFilePath);	
		beverages = myCoffeeMachine.getBeverageOptions();
		outlet_count  = myCoffeeMachine.getOutletCount();
//		fullRandomCaseTest();
//		runLargeParallelOrdersTest();
//		getRunningLowIngredientsTest();
//		restockIngredientsByAmountTest();
//		addUnvailableIngredientTest();
		BeverageNotAvailableTest("masala_tea");
	}
	
	//Test case Functions;
	public static void fullRandomCaseTest() {
		executor = Executors.newFixedThreadPool(outlet_count);
		System.out.println("Please Provide a number for a beverage you want to have.");
		for (int i=0; i<beverages.length; i++) {
			System.out.print("input: " + (i+1) + " for " + beverages[i]+ " ");
		}
		System.out.println("\nInput 0 to exit.");
		
		int input;
		Scanner in = new Scanner(System.in);
		while (true) {
			input = in.nextInt();
			if (input == 0) {
				in.close();
				break;
			}
			if (input > beverages.length) {
				System.out.println("this is not a valid option.");
				continue;
			}
			int outlet = random.nextInt(outlet_count)+1;
			Order order = new Order(myCoffeeMachine, beverages[input-1], outlet);
			executor.execute(order);
		}
	}
	
	//Run more orders parallel than num_outlets. Result should be that orders placed after all the outlets are busy should be rejected;
	public static void runLargeParallelOrdersTest() throws InterruptedException {
		executor = Executors.newFixedThreadPool(outlet_count+1);
		myCoffeeMachine.SetFullCapacityToInfinite();
		for (int i=0; i<outlet_count+1; i++) {
			Order order = new Order(myCoffeeMachine, beverages[0], (i%outlet_count)+1);
			executor.execute(order);
		}
	}
	
	public static void getRunningLowIngredientsTest() {
		Map<String, int[]> lowIngredients = myCoffeeMachine.getIngredientsRunningLow();
		for (int i=0; i<beverages.length; i++) {
			while (lowIngredients.isEmpty()) {
				String result = myCoffeeMachine.getBeverage(beverages[i], 1);
				if (result.contains(" not available")) {
					i++;
				}
				System.out.println(result+"\n");
				lowIngredients = myCoffeeMachine.getIngredientsRunningLow();
			}
			System.out.println("The following ingredients are running low: ");
			for (Entry<String, int[]>e: lowIngredients.entrySet()) {
				System.out.println("Ingredient: " + e.getKey() + " Current Quantity: " + e.getValue()[0] + " Max Capacity: " + e.getValue()[1]);
			}
			System.out.println();
			myCoffeeMachine.fullRestockToInitialCapacity(null);
			lowIngredients.clear();
		}

	}
	
	public static void BeverageNotAvailableTest(String unsupportedBeverage) {
		System.out.println(myCoffeeMachine.getBeverage(unsupportedBeverage, 1));
	}
	
	public static void restockIngredientsByAmountTest() {
		Map<String, int[]> lowIngredients = myCoffeeMachine.getIngredientsRunningLow();
		boolean ingredientNotFound = false;
		for (int i=0; i<beverages.length; i++) {
			while (lowIngredients.isEmpty()) {
				String result = myCoffeeMachine.getBeverage(beverages[i], 1);
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
			Map<String, Integer>refillingMap = new HashMap<>();
			for (Entry<String, int[]>e: lowIngredients.entrySet()) {
				int val = e.getValue()[1]-e.getValue()[0];
				refillingMap.put(e.getKey(), val);
				System.out.printf("Restocking %s by adding %d\n", e.getKey(), val);
			}
			myCoffeeMachine.restockIngredientsByAmount(refillingMap);
			System.out.println();
			System.out.printf("Trying again to get %s\n", beverages[i]);
			System.out.println(myCoffeeMachine.getBeverage(beverages[i], 1));
			System.out.println();
			lowIngredients.clear();
		}
	}
	 
	public static void addUnvailableIngredientTest() {
		for (String beverage: beverages) {
			String result = myCoffeeMachine.getBeverage(beverage, 1);
			if (result.contains("is not available")) {
				System.out.println(result);
				String[] strs = result.split(" ");
				String ing = strs[6];
				System.out.printf("adding Ingredient: %s. Total amount will be 100\n", ing);
				myCoffeeMachine.addUnavailableIngredientWithMaxCapacity(ing, 100);
				System.out.println("We should be able to get " + beverage);
				System.out.println(myCoffeeMachine.getBeverage(beverage, 1));
				break;
			}
		}
	}
}
