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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation;
import cz.cuni.mff.d3s.metaadaptation.search.StateSpaceSearch;
import cz.cuni.mff.d3s.metaadaptation.search.annealing.TimeProgress;

/**
 * Adapts the annotated components in the same DEECo node by adding
 * non-deterministic mode transitions.
 * 
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class NonDeterministicModeSwitchingManager implements MAPEAdaptation {

	public static final double DEFAULT_ENERGY = 1;

	public static double startingNondeterminism = NonDetModeSwitchAnnealStateSpace.DEFAULT_STARTING_NONDETERMINISM;
	
	public static boolean verbose = false;
	
	
	private final Component managedComponent;
		
	public NonDetModeSwitchAnnealStateSpace stateSpace;
		
	public final long startTime;
	
	private final TimeProgress timer;

	public Double currentNonDeterminismLevel;
	public Double nextNonDeterminismLevel;
	
	public NonDetModeSwitchFitness evaluator;
	
	
	public NonDeterministicModeSwitchingManager(long startTime,
			NonDetModeSwitchFitness eval,
			TimeProgress timer, Component component)
					throws InstantiationException, IllegalAccessException {
		if(!isValidComponent(component)){
			throw new IllegalArgumentException(String.format(
					"Non-deterministic mode switching cannot be applied "
					+ "to the given component %s, it doesn't specify "
					+ "either mode chart or state space search.",
					component.getClass().getName()));
		}
		
		this.startTime = startTime;
		stateSpace = new NonDetModeSwitchAnnealStateSpace(startingNondeterminism);
		this.timer = timer;
		evaluator = eval;
		managedComponent = component;
		currentNonDeterminismLevel = startingNondeterminism;
	}
	
	private boolean isValidComponent(Component component) {
		ModeChart modeChart = component.getModeChart();
		if (modeChart == null) {
			return false;
		}
		
		StateSpaceSearch sss = component.getStateSpaceSearch();
		return sss != null;
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#monitor()
	 */
	@Override
	public void monitor() {
		long currentTime = timer.getTime();
		
		// Check whether to measure
		Mode mode = managedComponent.getModeChart().getCurrentMode();
		if(!mode.isFitnessComputed()){
			evaluator.restart();
			if(verbose){
				System.out.println(String.format("Non-deterministic mode switching "
					+ "skipping fitness monitor in state: %s",
					mode.getClass().getName()));
			}
			
			return;
		}
				
		// Measure the current fitness
		double energy = evaluator.getFitness(currentTime, managedComponent);
		
		// Store fitness value for the current non-determinism
		stateSpace.getState(currentNonDeterminismLevel).setEnergy(energy);
		
		if(verbose){
			System.out.println(String.format("Non-deterministic mode switching energy for the "
				+ "non-deterministic level %f at %d is %f",
				currentNonDeterminismLevel, currentTime, energy));
		}
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#analyze()
	 */
	@Override
	public boolean analyze() {
		long currentTime = timer.getTime();
		
		// Don't add non-determinism until the start time
		if(currentTime < startTime){
			return false;
		}
		
		// Stop the adaptation when the search is finished
		ModeChart modeChart = managedComponent.getModeChart();
		StateSpaceSearch sss = managedComponent.getStateSpaceSearch();	
		if(sss.isFinished(stateSpace.getState(currentNonDeterminismLevel))){
			return false;
		}
		
		// Don't plan and execute while in mode that excludes fitness computation
		Mode mode = modeChart.getCurrentMode();
		if(!mode.isFitnessComputed()){
			return false;
		}
		
		return true;
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#plan()
	 */
	@Override
	public void plan() {
		StateSpaceSearch sss = managedComponent.getStateSpaceSearch();			
		
		// Get the next state
		NonDetModeSwitchAnnealState nextState =
				(NonDetModeSwitchAnnealState) sss.getNextState(
						stateSpace.getState(currentNonDeterminismLevel));
		
		nextNonDeterminismLevel = nextState.getNondeterminism();		
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#execute()
	 */
	@Override
	public void execute() {
		ModeChart modeChart = managedComponent.getModeChart();
		if(!modeChart.isModified()){
			// Modify the mode chart if not yet modified
			addNondeterministicTransitions();
		}

		reconfigureModeChart(nextNonDeterminismLevel);
		currentNonDeterminismLevel = nextNonDeterminismLevel;
		// Restart the evaluator for next measurements with new probabilities
		evaluator.restart();

		// Notify that the probability has changed
		managedComponent.nonDeterminismLevelChanged(currentNonDeterminismLevel);
		
		if(verbose) {
			long currentTime = timer.getTime();
			String id = managedComponent.getId();
			System.out.println(String.format("Non-deterministic mode switching the "
				+ "non-deterministic level of component %s set to %f at %d",
				id, currentNonDeterminismLevel, currentTime));
		}
		
	}
	
	private void reconfigureModeChart(double nondeterminism){
		ModeChart modeChart = managedComponent.getModeChart();
		
		Map<Mode, Set<Transition>> dynamicTransitions = new HashMap<>();
		Map<Mode, Set<Transition>> staticTransitions = new HashMap<>();
		
		// Sort out dynamic and static transitions
		for(Transition transition : modeChart.getTransitions()){
			Mode mode = transition.getFrom();
			if(!dynamicTransitions.containsKey(mode)){
				dynamicTransitions.put(mode, new HashSet<>());
			}
			if(!staticTransitions.containsKey(mode)){
				staticTransitions.put(mode, new HashSet<>());
			}
			
			if(transition.isDynamic()){
				dynamicTransitions.get(mode).add(transition);
			} else {
				staticTransitions.get(mode).add(transition);
			}			
		}

		// Set probability to dynamic transitions
		for(Mode mode : dynamicTransitions.keySet()){
			int dynamicTransitionsCnt = dynamicTransitions.get(mode).size();
			for(Transition transition : dynamicTransitions.get(mode)){
				// +1 for one static transition (suppose they are exclusive
				transition.setProbability(nondeterminism / (dynamicTransitionsCnt + 1));
			}
		}

		// Suppose all static transitions are exclusive
		for(Mode mode : staticTransitions.keySet()){
			for(Transition transition : staticTransitions.get(mode)){
				transition.setProbability(1- nondeterminism);
			}
		}
	}
	
	private void addNondeterministicTransitions(){
		ModeChart modeChart = managedComponent.getModeChart();
		
		// Make full graph
		for(Mode from : modeChart.getModes())
		{
			// Don't add new outward transitions if the mode doesn't allow it
			if(!from.nonDeterministicOut()){
				continue;
			}
			
			// Identify missing transitions
			Set<Mode> allModes = modeChart.getModes();
			Set<Mode> missingModes = new HashSet<>(allModes);
			Set<Transition> fromModeTransitions = getTransitionsFrom(from);
			missingModes.removeAll(getTargetModes(fromModeTransitions));
			
			// Add missing transitions
			for(Mode to : missingModes){
				// Don't add new inward transitions if the mode doesn't allow it
				if(!to.nonDeterministicIn()){
					continue;
				}
								
				Transition transition = modeChart.createTransition(from, to, new TrueGuard());
				transition.setDynamic(true);
				
				if(modeChart.getTransitions().contains(transition)){
					throw new IllegalStateException(
							String.format("Transition \"%s\" -> \"%s\" already defined.",
							from, to));
				}
				modeChart.addTransition(transition);
			}
		}

		modeChart.setModified();
	}
	
	private Set<Transition> getTransitionsFrom(Mode mode){
		ModeChart modeChart = managedComponent.getModeChart();
		Set<Transition> allTransitions = modeChart.getTransitions();
		Set<Transition> fromModeTransitions = new HashSet<>();
		
		for(Transition transition : allTransitions){
			if(transition.getFrom() == mode){
				fromModeTransitions.add(transition);
			}
		}
		
		return fromModeTransitions;
	}
	
	private Set<Mode> getTargetModes(Set<Transition> transitions){
		Set<Mode> modes = new HashSet<>();
		for(Transition transition : transitions){
			modes.add(transition.getTo());
		}
		
		return modes;
	}
}
