/*******************************************************************************
 * Copyright 2016 Charles University in Prague
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
package cz.cuni.mff.d3s.metaadaptation.modeswitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation;

/**
 * Adapts the annotated components in the same DEECo node by adding
 * non-deterministic mode transitions.
 * 
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class NonDeterministicModeSwitchingManager implements MAPEAdaptation {

	private boolean verbose = false;
	
	private double transitionProbability = 0.01;
	
	private int transitionPriority = 10;
	
	private final List<Transition> trainTransitions;
	
	private final List<Transition> plannedTransitions;
	
	private final ComponentManager componentManager;

	private Map<? extends Transition, Double> utility = null;
	
	private boolean initialized;
	
	
	public NonDeterministicModeSwitchingManager(ComponentManager componentManager, Map<? extends Transition, Double> utility){
		if(componentManager == null){
			throw new IllegalArgumentException(String.format(
					"The %s parameter is null.", "componentManager"));
		}
		this.componentManager = componentManager;
		
		// Check whether given components are valid
		for(Component component : componentManager.getComponents()){
			if(!isValidComponent(component)){
				throw new IllegalArgumentException(String.format(
						"Non-deterministic mode switching cannot be applied "
						+ "to the given component %s, it doesn't specify "
						+ "either mode chart.",
						component.getClass().getName()));
			}
		}
		
		if(utility != null){
			this.utility = utility;
			for(Transition t : utility.keySet()){
				System.out.println(String.format("%s: %f", t, utility.get(t)));
			}
		} else {
			this.utility = new HashMap<>();
		}
		
		trainTransitions = new ArrayList<>();
		plannedTransitions = new ArrayList<>();
		initialized = false;
	}
	

	public void setVerbosity(boolean verbosity){
		verbose = verbosity;
	}
	
	public void setTransitionProbability(double probability){
		transitionProbability = probability;
	}
	
	public void setTransitionPriority(int priority){
		transitionPriority = priority;
	}
	
	public void setTrainTransitions(List<Transition> trainTransitions){
		this.trainTransitions.clear();
		this.trainTransitions.addAll(trainTransitions);
	}
	
	private boolean isValidComponent(Component component) {
		ModeChart modeChart = component.getModeChart();
		return modeChart != null;
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#monitor()
	 */
	@Override
	public void monitor() {
		if(!trainTransitions.isEmpty() && !initialized && verbose){
			for(Component component : componentManager.getComponents()){
				printMissingTransitions(component.getModeChart());
			}
		}
		
		// TODO: record utility
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#analyze()
	 */
	@Override
	public boolean analyze() {
		// TODO: analyze utility
		return !initialized;
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#plan()
	 */
	@Override
	public void plan() {
		plannedTransitions.clear();
		
		if(!trainTransitions.isEmpty()) {
			plannedTransitions.addAll(trainTransitions);
		} else {
			// TODO: choose transition based on the utility
		}
		
		/*Transition bestTransition = null;
		double minUtility = Double.MAX_VALUE;
		for(Transition t : utility.keySet()){
			if(minUtility > utility.get(t)){
				minUtility = utility.get(t);
				bestTransition = t;
			}
		}
		if(bestTransition != null && !bestTransition.equals(addedTransition)){
			plannedFrom = bestTransition.getFrom();
			plannedTo = bestTransition.getTo();
		} else {
			plannedFrom = null;
			plannedTo = null;
		}*/		
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#execute()
	 */
	@Override
	public void execute() {
		if(plannedTransitions.isEmpty()){
			return;
		}
		
		for(Component component : componentManager.getComponents()){
			for(Transition plannedTransition : plannedTransitions){
				Transition addedTransition = component.getModeChart().addTransition(
						plannedTransition.getFrom(), plannedTransition.getTo(),
						new ProbabilisticGuard(transitionProbability));
				addedTransition.setPriority(transitionPriority);
				
				if(verbose){
					System.out.println(String.format("The transition %s added.",
							addedTransition));
				}
			}
		}
		initialized = true;
	}
	
	public void printMissingTransitions(ModeChart modeChart){
		Set<? extends Mode> modes = modeChart.getModes();
		Set<? extends Transition> transitions = modeChart.getTransitions();
		for(Mode from : modes){
			for(Mode to : modes){
				if(from.equals(to)){
					continue;
				}
				
				boolean found = false;
				for(Transition transition : transitions){
					if(transition.getFrom().equals(from) && transition.getTo().equals(to)){
						found = true;
						break;
					}
				}
				
				if(!found){
					System.out.println(String.format("MISSING TRANSITION: %s -> %s",
							from.toString(), to.toString()));
				}
			}
		}
	}
	
}
