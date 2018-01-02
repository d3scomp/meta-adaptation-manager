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

import java.util.ArrayList;
import java.util.List;

import cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation;

/**
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class ModeSwitchPropsManager implements MAPEAdaptation {
	
	private boolean verbose = false;

	private final List<PropertyValue> trainProperties;
	
	private final List<PropertyValue> plannedProperties;
	
	private final ComponentManager componentManager;
	
	private boolean initialized;
	
	
	public ModeSwitchPropsManager(ComponentManager componentManager) {
		if(componentManager == null){
			throw new IllegalArgumentException(String.format("The %s argument is null.", componentManager));
		}
		
		this.componentManager = componentManager;
		
		// Check whether given components are valid
		for(Component component : componentManager.getComponents()){
			if(!isValidComponent(component)){
				throw new IllegalArgumentException(String.format(
						"Mode switching property adjustment cannot be applied "
						+ "to the given component %s, it doesn't specify "
						+ "either mode chart.",
						component.getClass().getName()));
			}
		}
		
		trainProperties = new ArrayList<>();
		plannedProperties = new ArrayList<>();
		initialized = false;
	}
	
	public void setVerbosity(boolean verbosity){
		verbose = verbosity;
	}
	
	public void setTrainProperties(List<PropertyValue> trainProperties){
		this.trainProperties.clear();
		this.trainProperties.addAll(trainProperties);
	}

	private boolean isValidComponent(Component component) {
		ModeChart modeChart = component.getModeChart();
		return modeChart != null;
	}
	
	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#monitor()
	 */
	@Override
	public void monitor() {
		if(!trainProperties.isEmpty() && !initialized && verbose){
			for(PropertyValue property : trainProperties){
				System.out.println(String.format("Training property: %s = %f",
						property.property, property.value));
			}
		}
		
		// TODO: record utility	
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#analyze()
	 */
	@Override
	public boolean analyze() {
		// TODO: analyze utility
		return !initialized;
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#plan()
	 */
	@Override
	public void plan() {
		plannedProperties.clear();
		
		if(!trainProperties.isEmpty()) {
			plannedProperties.addAll(trainProperties);
		} else {
			// TODO: choose property based on the utility
		}
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#execute()
	 */
	@Override
	public void execute() {
		if(plannedProperties.isEmpty()){
			return;
		}
		
		for(Component component : componentManager.getComponents()){
			for(Transition transition : component.getModeChart().getTransitions()){
				for(String property : transition.getGuardParams().keySet()){
					for(PropertyValue plannedProperty : plannedProperties){
						if(property.equals(plannedProperty.property)){
							transition.setGuardParam(property, plannedProperty.value);
							if(verbose){
								System.out.println(String.format(
									"The guard property %s in %s in %s set to %f.",
									property, transition, component, plannedProperty.value));
							}
						}
					}
				}
			}
		}
		initialized = true;
	}

}
