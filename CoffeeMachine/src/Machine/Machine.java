package Machine;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;

import CustomExceptions.BeverageNotSupportedException;
import CustomExceptions.OrderWhileRestockingException;
import CustomExceptions.OutletNotFreeException;

/*
 * This class simulates a coffee machine. It has 'n' outlets (named 1 to n) and supports a single use on each outlet in parallel. Hence it can handle 'n' orders in parallel.
 * Each of the outlet is independent of other and can serve any beverage supported by the machine. A user can order a beverage by providing its name.
 * On start, the machine class is instantiated by a json file which contains info about beverages served and the different ingredients used by beverages. 
 * Each ingredient is an instance of Ingredient class and is held by its name in a map in machine instance. As ingredient is shared among all the beverages, only one instance of it should be created. 
 * Each supported beverage is an instance of Beverage class and is held by the machine in a map by its name as key. 
 * It handles parallel order processing by using an ExecutorService which hold a fixed size thread pool of n threads;
 * 
 * The flow for executing a order for beverage is as follows: 
 * 1. Once a user places an order of beverage by its name, its first checked for sanity(supported beverage name, all outlets busy). 
 * 2. A runnable job is created and is submit to executor to process. The running orders count is incremented.
 * 3. Then it is checked whether the machine is not restocking at the time. This is checked by a boolean and time flag which is set when the machine starts restocking.
 *    If sufficient time has elapsed after it started restocking, the boolean sanity checker method will unset the boolean flag and allow the order to proceed, otherwise reject it.
 *    Sufficient time above is specified by a constant. I am using it as 2 seconds.
 * 4. the next check will be that all the required ingredients for the beverage are present in sufficient quantity. If any one of them is lacking, the order will be rejected. 
 *    In case if any of the required ingredient is currently not present in machine then also the order will be rejected. 
 * 5. If everything is favorable, the order is placed and appropriate amount of ingredients are deducted from their current quantity in machine. The thread goes to sleep for a fixed amount of time to simulate preparation time.
 * 6. The flow from ingredient sufficiency check to blocking outlet to deducting ingredients is done in a synchronous way to avoid inconsistency.  
 * 7. In case of any failure, appropriate custom exceptions are thrown which is then returned to user as a string message. In case of success, appropriate success message is returned to user.
 * 8. At the end of processing the order, the running order count is decremented. It marks the completion of the job.
 * 
 * At any time user can check if any ingredient is running low. this if found out by comparing their current quantity with their low threshold value. 
 * low threshold value is the higher of either 20% of their max_capacity or the maximum amount required by any kind of beverage. 
 * 
 * User can restock the ingredients either to their max_capacity or by passing an amount by which it want to add to the ingredient quantity in machine. 
 * User can either specify which ingredients they want to restock or they can restock all of them at once.
 * 
 * User can also add an previously unavailable ingredient by specifying its name and max_capacity. 
 * At any time user can check the current quantity of ingredients and the supported beverage names
 *
 */
public final class Machine implements CoffeeMachine {
	
	//We need only one instance of a machine. Implementing CoffeeMachine as a singleton 
	private static Machine currentMachine = null;
	
	//It keeps track of the outlets which are in process of preparing and serving beverage and cant take new order untill it finishes its current order.
//	private Set<Integer> busyOutlets;
	
	//It keeps track of current ingredients. Each ingredient is an object and its name is used as a key to hold it.
	private ConcurrentHashMap<String, Ingredient>ingredients;
	
	//Constant Preparation time for all beverages. using 5 seconds so as to simulate the machine busy flow.
	private static final int PREPARATION_TIME =5; // in seconds . 
	
	//This is the time taken for machine to restock. No order will be served while machine is restocking. 
	private static final int STOCKING_TIME = 2; // in seconds
	
	//The total outlets in the machine. Set while creating the machine. Its setter is not implemented so it cannot be changed from outside.
	private int TOTAL_OUTLETS;
	
	//This map contains a beverage object for each beverage type. It keeps track of ingredient requirement of each beverage type. 
	private Map<String, Beverage>beverages_types;
	
	private static ExecutorService executor; 
	private static volatile int running_order_count;

	//fields to keep track of time lapsed in restocking. 
	private Instant restocking_start_time;
	boolean isRestocking = false;
	
	//Singleton Implementation of machine. Once the machine is created, it should not alter its settings like max outlets and adding new beverage.
	public static Machine GetOrCreateMachine(String settings) {
		if (currentMachine == null) {
			currentMachine = new Machine(settings);
		}
		return currentMachine;
	}
	
	private Machine(String settings) {
		Gson gson = new Gson();
		try (FileReader reader = new FileReader(settings)){
			Object obj = gson.fromJson(reader, Object.class);
			createMachineFromParsedObject(obj);
			executor = Executors.newFixedThreadPool(TOTAL_OUTLETS);
			System.out.println(obj);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Custom function to parse the json settings file. This function takes care of initializing all required fields of machine.
	 * Sets the number of outlets of the machine. 
	 * for each ingredient in the "total_items_quantity" map of settings json, it creates an instance of Ingredient class and takes the quantity given as max capacity of that ingredient
	 * and also sets that as starting current quantity.
	 * creates an object of each type of beverage serviced by machine and stores them in a map. These objects handle the ingredient info of that beverage.
	 */
	private <V> void createMachineFromParsedObject(Object obj) {
		// TODO Auto-generated method stub
		Map<String, V>map = (Map<String, V>)((Map<String, V>) obj).get("machine");
		this.TOTAL_OUTLETS = ((Double)((Map<String, V>)map.get("outlets")).get("count_n")).intValue();
		
		Map<String, Double>ingredients_info = (Map<String, Double>)(map.get("total_items_quantity"));
		Iterator<Entry<String, Double>> ingredients_it = ingredients_info.entrySet().iterator();
//		this.ingredient_max_quantity = new HashMap<String, Integer>();
		this.ingredients = new ConcurrentHashMap<String, Ingredient>();
		while (ingredients_it.hasNext()) {
			Entry<String, Double>e = ingredients_it.next();
			Ingredient ingredient = new Ingredient(e.getKey(), e.getValue().intValue());
//			this.ingredient_max_quantity.put(e.getKey(), e.getValue().intValue());
			this.ingredients.put(e.getKey(), ingredient);
		}
		
		HashMap<String, Integer>ingredient_low_threshold= new HashMap<>();
		
		this.beverages_types = new HashMap<String,Beverage>();
		Map<String, Map<String, Double>>beverages = (Map<String, Map<String, Double>>)(map.get("beverages"));
		Iterator<String>beverages_it = beverages.keySet().iterator();
		while (beverages_it.hasNext()) {
			String beverage_name = beverages_it.next();
			Beverage bev = new Beverage(beverage_name);
			Map<String, Double>bev_map = beverages.get(beverage_name);
			Iterator<Entry<String, Double>> bev_it = bev_map.entrySet().iterator();
			while (bev_it.hasNext()) {
				Entry<String, Double>entry = bev_it.next();
				bev.addIngredient(entry.getKey(), entry.getValue().intValue());
				if (ingredient_low_threshold.containsKey(entry.getKey())) {
					int ing_val = ingredient_low_threshold.get(entry.getKey());
					ing_val = ing_val > entry.getValue().intValue()?ing_val:entry.getValue().intValue();
					ingredient_low_threshold.put(entry.getKey(), ing_val);
				}else {
					ingredient_low_threshold.put(entry.getKey(), entry.getValue().intValue());
				}
			}
			this.beverages_types.put(beverage_name, bev);
		}
		
//		this.busyOutlets = new HashSet<Integer>();
		
		adjustLowThresholdForIngredients(ingredient_low_threshold);
	}
	
	
	//Low threshold of an ingredient is set to the either 20% of max_capacity or minimum amount required to be able to serve any kind of beverage, whichever is higher. 
	private void adjustLowThresholdForIngredients(HashMap<String, Integer> ingredient_low_threshold) {
		Iterator<Entry<String, Integer>>it = ingredient_low_threshold.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer>e = it.next();
			if (this.ingredients.containsKey(e.getKey())) {
				this.ingredients.get(e.getKey()).adjustThreshold(e.getValue());
			}
		}
	}
	
	/*
	 * This is the machine interface method to place an order. It first call the sanity checker method. 
	 * Then increments the running order count and creates a new runnable object with the order to execute.
	 */ 
	public void placeOrder(String beverage) {
		try {
			verifyValidity(beverage);
			BeverageOrder order = new BeverageOrder(this, beverage);
			running_order_count++;
			executor.execute(order);
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
	}

	/*
	 * This is the main function of the machine. It first does some sanity checks like checking if any outlet is free, the machine is not restocking, the beverage is supported.
	 * It then calls to verify availability of ingredients and if order can be placed, proceed to call the prepare method of requested beverage object
	 * In case of any king of failure, it catches custom exceptions and return the error string to user.
	 * if successful, it then sleeps for 5 second so as to simulate the preparation of beverage. after completion, it also frees the outlet to be used again.
	 */
	 String getBeverage(String bev_name){
		 String result;
		 try {
			verifyAndPrepareBeverage(bev_name);
			System.out.println("machine is preparing to serve "+bev_name+". It will Take " + PREPARATION_TIME + " seconds\n");
			Thread.sleep(PREPARATION_TIME*1000);
			result = bev_name + " is prepared.";
		} catch(Exception e) {
			result = e.getMessage();
		}
		running_order_count--;
		return result;
	}
	
	/*
	 * Sanity checks: Rejects order if
	 * 1. If the beverage is unsupported. 
	 * 2. if the machine is currently restocking by giving the waiting time after which machine can be used again
	 * 3. if the No outlet is available.
	 */
	private void verifyValidity(String bev)throws Exception {
		if (!this.beverages_types.containsKey(bev)) {
			throw new BeverageNotSupportedException(bev);
		}
		if (isRestocking) {
			Instant now = Instant.now();
			Duration duration = Duration.between(restocking_start_time, now);
			if ((int)duration.toSeconds() >= STOCKING_TIME) {
				isRestocking = false;
			} else {
				int timeleft = (int) (STOCKING_TIME-duration.toSeconds());
				throw new OrderWhileRestockingException(timeleft);
			}
		}
		if (running_order_count >= TOTAL_OUTLETS) {
			throw new OutletNotFreeException();
		}
	}
	
	/*
	 * the method which actually checks if the ingredients are sufficient and available and then place the order to the corresponding beverage object.
	 *This is synchronized because we do not need multiple thread changing quantity of ingredients simultaneously. 
	 *This can cause an error where there is not sufficient ingredient left for a beverage for which we have committed the order.
	 */
	private synchronized void verifyAndPrepareBeverage(String bev_name) throws Exception {
		Beverage beverage = this.beverages_types.get(bev_name);
		if (beverage.verify(this.ingredients)) {
			beverage.prepare(this.ingredients);
		}
	}
	
	/*
	 * the custom restocking method restocks the ingredients given in input map by adding the amount given as their values in the map. Also set the starting time of the restocking operation.
	 */
	public synchronized void  restockIngredientsByAmount(Map<String, Integer> ingredients) {
		if (isRestocking) {
			System.out.println("Already restocking. Please wait for ingredients to run low again.");
			return;
		}
		restocking_start_time = Instant.now();
		isRestocking = true;
		Iterator<Entry<String, Integer>>it = ingredients.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer>e = it.next();
			if (this.ingredients.containsKey(e.getKey())) {
				this.ingredients.get(e.getKey()).restockByAmount(e.getValue());		
			}
		}
	}
	
	//This method restocks the given ingredients to their initial capacity. if the input is null, it restocks all the ingredients. It also set the starting time of restocking operation
	public synchronized void  fullRestockToInitialCapacity(String[] ingredients) {
		restocking_start_time = Instant.now();
		isRestocking = true;
		if (ingredients == null || ingredients.length == 0) {
			fullRestockForAll();
		} else {
			for (String ingredient: ingredients) {
				if (this.ingredients.containsKey(ingredient)) {
					this.ingredients.get(ingredient).restockToFullCapacity();
				}
			}
			System.out.println("Restocked following ingredients to their full capacity: ");
			System.out.println(ingredients+"\n");
		}
	}
	
	private void fullRestockForAll() {
		System.out.println("Restocking following ingredients to their max capacity: ");
		System.out.println(this.ingredients.keySet()+"\n");
		for (String ing: this.ingredients.keySet()) {
			this.ingredients.get(ing).restockToFullCapacity();
		}
	}
	
	//In case an ingredient is not added while setting the machine for the first time but is used for a beverage, this method can be used to add that ingredient. 
	public void addUnavailableIngredientWithMaxCapacity(String ingredient, int max_capacity) {
		Ingredient ing = new Ingredient(ingredient, max_capacity);
		this.ingredients.put(ingredient, ing);
	}
	
	/*
	 * This compares the current quantity of ingredients with their low threshold value.
	 * and if the current quantity is less, adds that ingredient's name, its current quantity and max capacity in a map to be sent as a result. 
	 */
	public Map<String, int[]> getIngredientsRunningLow() {
		Map<String, int[]>ingredientsRunningLow = new HashMap<>();
		Iterator<Entry<String, Ingredient>>itr = this.ingredients.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<String, Ingredient>e = itr.next();
			if (e.getValue().isRunningLow()) {
				ingredientsRunningLow.put(e.getKey(), new int[] {e.getValue().getCurrent_quantity(), e.getValue().getMax_quantity()});
			}
		}
		return ingredientsRunningLow;
		
	}
	
	//provides names of all the beverages this machine can serve.
	public String[] getBeverageOptions() {
		String[] result = new String[this.beverages_types.size()];
		Iterator<String>it = this.beverages_types.keySet().iterator();
		int i=0;
		while (it.hasNext()) {
			result[i++]=it.next();
		}
		return result;
	}

	//provide number of outlets in this machine.
	public int getOutletCount() {
		// TODO Auto-generated method stub
		return this.TOTAL_OUTLETS;
	}
	
	
	//provides names of all available ingredients in machine along with their current capacity.
	public Map<String, Integer>getAvailableIngredients(){
		Map<String, Integer>availableIngredients = new HashMap<>();
		for (String key: this.ingredients.keySet()) {
			availableIngredients.put(key, this.ingredients.get(key).getCurrent_quantity());
		}
		return availableIngredients;
	}
	
	//this is a method used to test running multiple parallel orders. It sets current quantity to Int_max value for all ingredients. This should never be exposed to user in real scenario.
	public void SetFullCapacityToInfinite() {
		for (Entry<String, Ingredient>e: this.ingredients.entrySet()) {
			e.getValue().setNewCurrentCapacity(Integer.MAX_VALUE);
		}
	}
	
	//method to test different use cases which requires serial flow. Not required in production type implementation.
	public String placeOrderSerially(String beverage) {
		String result;
		try {
			verifyValidity(beverage);
			running_order_count++;
			result = this.getBeverage(beverage);
		} catch (Exception e){
			result = e.getMessage();
		}	
		return result;
	}
	
}
