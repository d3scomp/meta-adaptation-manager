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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation;
import cz.cuni.mff.d3s.metaadaptation.UtilityHolder;
import cz.cuni.mff.d3s.metaadaptation.search.StateSpaceSearch;

/**
 * Adapts the annotated components in the same DEECo node by adding
 * non-deterministic mode transitions.
 * 
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class NonDeterministicModeSwitchingManager implements MAPEAdaptation {

//	public static final double DEFAULT_ENERGY = 1;

//	public static double startingNondeterminism = NonDetModeSwitchAnnealStateSpace.DEFAULT_STARTING_NONDETERMINISM;
	
	public static boolean verbose = false;
	
	public static double transitionProbability = 0.01;
	
	public static int transitionPriority = 10;
	
	public static boolean training = false;
	
	public static Mode trainFrom = null;
	
	public static Mode trainTo = null;
	
	public static File trainingOutput = null;
	
	
	private final List<Component> components;

	private Map<Transition, UtilityHolder> utility = null;
	
	private PrintWriter writer;
	
	private boolean trainingInitialized;

	private Mode plannedFrom;
	
	private Mode plannedTo;
	
		
//	public NonDetModeSwitchAnnealStateSpace stateSpace;
//		
//	public final long startTime;
//	
//	private final TimeProgress timer;
//
//	public Double currentNonDeterminismLevel;
//	public Double nextNonDeterminismLevel;
	
	
	public NonDeterministicModeSwitchingManager(List<Component> components, Map<Transition, UtilityHolder> utility)
					throws InstantiationException, IllegalAccessException, FileNotFoundException {
		if(components == null){
			throw new IllegalArgumentException(String.format(
					"The %s parameter is null.", "components"));
		}
		
		// Check whether given components aree valid
		for(Component component : components){
			if(!isValidComponent(component)){
				throw new IllegalArgumentException(String.format(
						"Non-deterministic mode switching cannot be applied "
						+ "to the given component %s, it doesn't specify "
						+ "either mode chart or state space search.",
						component.getClass().getName()));
			}
		}
		
		// Check whether the configuration of this meta-adaptation manager is correct
		if(training){
			if(trainFrom == null || trainTo == null){
				throw new IllegalStateException(String.format("The %s has to be set in the %s mode",
						"trainTransition", "training"));
			}
			if(trainingOutput == null){
				throw new IllegalStateException(String.format("The %s has to be set in the %s mode",
						"trainingOutput", "training"));
			}
		}
		
//		this.startTime = startTime;
//		stateSpace = new NonDetModeSwitchAnnealStateSpace(startingNondeterminism);
//		this.timer = timer;
		
		
		
		this.components = components;
		if(utility != null){
			this.utility = utility;
		} else {
			this.utility = new HashMap<>();
		}
		
		trainingInitialized = false;
		writer = new PrintWriter(trainingOutput);
		
//		currentNonDeterminismLevel = startingNondeterminism;
	}
	
	private boolean isValidComponent(Component component) {
		ModeChart modeChart = component.getModeChart();
		return modeChart != null;
		
//		StateSpaceSearch sss = component.getStateSpaceSearch();
//		return sss != null;
	}
	
	public void terminate(){
		if(training){
			writer.close();
		}
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#monitor()
	 */
	@Override
	public void monitor() {
		if(training && trainingInitialized){
			for(Component component : components){
				double u = component.getType().getUtility();
				writer.println(String.format("%f", u));
			}
			
			return;
		}
		
		if(!training){
			// TOOD: record utility
			
		}
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#analyze()
	 */
	@Override
	public boolean analyze() {
		if(training && !trainingInitialized){
			return true;
		}
		if(training){
			return false;
		}
		
		// TODO: analyze the utility of added transitions - decide on adding replacing transitions
		return true;
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#plan()
	 */
	@Override
	public void plan() {
		if(training) {
			plannedFrom = trainFrom;
			plannedTo = trainTo;
			return;
		}
		
		// TODO: plan some good transition to add		
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.jdeeco.adaptation.MAPEAdaptation#execute()
	 */
	@Override
	public void execute() {
		if(plannedFrom == null || plannedTo == null){
			System.err.println("EMS - planned transition is null.");
			return;
		}
		
		for(Component component : components){
			Transition t = component.getModeChart().addTransition(plannedFrom,
					plannedTo, new ProbabilisticGuard());
			t.setPriority(transitionPriority);
		}
		
		if(verbose){
			System.out.println(String.format("The transition from %s to %s added.",
					plannedFrom, plannedTo));
		}
		
		/*ModeChart modeChart = managedComponent.getModeChart();
		if(!modeChart.isModified()){
			// Modify the mode chart if not yet modified
			addNondeterministicTransitions();
		}

		reconfigureModeChart(nextNonDeterminismLevel);
		currentNonDeterminismLevel = nextNonDeterminismLevel;
		// Restart the evaluator for next measurements with new probabilities
		managedComponent.restartUtility();

		// Notify that the probability has changed
		managedComponent.nonDeterminismLevelChanged(currentNonDeterminismLevel);
		
		if(verbose) {
			long currentTime = timer.getTime();
			String id = managedComponent.getId();
			System.out.println(String.format("Non-deterministic mode switching the "
				+ "non-deterministic level of component %s set to %f at %d",
				id, currentNonDeterminismLevel, currentTime));
		}*/
		
	}
	
	/*private void reconfigureModeChart(double nondeterminism){
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
	}*/
}
