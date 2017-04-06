/*******************************************************************************
 * Copyright 2017 Charles University in Prague
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *******************************************************************************/
package cz.cuni.mff.d3s.metaadaptation.modeswitchprops;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation;

/**
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class ModeSwitchPropsManager implements MAPEAdaptation {
	
	public static boolean verbose = false;
	
	public static boolean training = false;
	
	public static String trainProperty = null;

	public static double trainValue = 0.0;
	
	private final List<Component> components;
	
	private boolean initialized;
	
	/*public static double step = 0.1;
	
	private final Component component;
	private final ModeChart modeChart;
	
	private final Random random;
	
	private double utility;
	private double lastUtility;
	private Transition transition;
	private boolean increase;
	private String propertyName;*/
	
	public ModeSwitchPropsManager(List<Component> components) {
		if(components == null){
			throw new IllegalArgumentException(String.format("The %s argument is null.", components));
		}
		/*if(modeChart == null){
			throw new IllegalArgumentException(String.format("The %s argument is null.", modeChart));
		}*/
		
		this.components = components;
		initialized = false;
		//this.modeChart = modeChart;
		
		/*random = new Random();
		
		utility = 0;
		lastUtility = 0;
		transition = null;
		increase = false;
		propertyName = null;*/
	}
	
	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#monitor()
	 */
	@Override
	public void monitor() {
		//utility = component.getUtility();
		
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#analyze()
	 */
	@Override
	public boolean analyze() {

		return !initialized;
		//lastUtility = utility;
		//return utility < component.getUtilityThreshold();
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#plan()
	 */
	@Override
	public void plan() {
		/*if(transition == null){
			// Plan the first change made by this mechanism
			transition = getRandomTransition();
			increase = random.nextBoolean();
			propertyName = getRandomProperty(transition);
			if(propertyName == null){
				// If a transition without tunable parameter was selected take no action
				reset();
			}
			// Otherwise everything is planned
			return;*
		}
		
		if(utility < lastUtility){
			// If the utility decreased after the last change - roll back the last change
			increase = !increase;
			return;
		}
		
		// Utility improved
		// With 50% chance continue improving otherwise select new transition to modify
		if(random.nextBoolean()){
			// Select a new transition to modify
			transition = getRandomTransition();
			increase = random.nextBoolean();
			propertyName = getRandomProperty(transition);
			if(propertyName == null){
				// If a transition without tunable parameter was selected take no action
				reset();
			}
		}*/
		
		// Continue the same improvement as done the lase time
	}
	
	/*private Transition getRandomTransition(){
		Set<Transition> transitions = modeChart.getTransitions();
		int target = random.nextInt(transitions.size());
		Transition[] transitionArray = transitions.toArray(new Transition[]{});
		
		return transitionArray[target];
	}
	
	private String getRandomProperty(Transition transition){		
		Map<String, Double> properties = transition.getGuardParams();

		if(properties.isEmpty()){
			return null;
		}
			
		Set<String> propNames = properties.keySet();		
		int target = random.nextInt(propNames.size());
		String[] namesArray = propNames.toArray(new String[]{});
		
		return namesArray[target];
	}*/
	
	/*private void reset(){
		transition = null;
		increase = false;
		propertyName = null;
	}*/

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#execute()
	 */
	@Override
	public void execute() {
		if(trainProperty == null){
			return;
		}
		
		for(Component component : components){
			for(Transition transition : component.getModeChart().getTransitions()){
				for(String property : transition.getGuardParams().keySet()){
					if(property.equals(trainProperty)){
						transition.setGuardParam(property, trainValue);
					}
				}
			}
		}
		initialized = true;
		
		/*if(transition != null && propertyName != null){
			double value = transition.getGuardParams().get(propertyName);
			if(increase){
				value += step;
			} else {
				value -= step;
			}
			transition.setGuardParam(propertyName, value);
		}*/		
	}

}
