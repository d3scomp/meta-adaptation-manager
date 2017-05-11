package cz.cuni.mff.d3s.metaadaptation.correlation;

import static cz.cuni.mff.d3s.metaadaptation.correlation.ConnectorManager.COORD_FILTER_FIELD;
import static cz.cuni.mff.d3s.metaadaptation.correlation.ConnectorManager.MEMBER_FILTER_FIELD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation;
import cz.cuni.mff.d3s.metaadaptation.correlation.ConnectorManager.MediatedKnowledge;
import cz.cuni.mff.d3s.metaadaptation.correlation.CorrelationLevel.DistanceClass;;

public class CorrelationManager implements MAPEAdaptation {

	/**
	 * Specify whether to print the values being processed by the correlation computation.
	 */
	private boolean dumpValues = false;

	private boolean verbose = false;

	/**
	 * Time slot duration in milliseconds. Correlation of values is computed
	 * within these time slots.
	 */
	private static final long TIME_SLOT_DURATION = 1000; // TODO: configurable
	
	/**
	 * Components of the system.
	 */
	private final ComponentManager componentManager;
	
	/**
	 * The connectors of the system.
	 */
	private final ConnectorManager connectorManager;
	
	/**
	 * Caches the failures that has been already analyzed, to save time.
	 */
	private final Map<Component, Set<String>> analyzedFailures;
	
	private final Map<Component, Set<String>> handledFailures;
	
	/**
	 * Holds the history of knowledge of all the other components in the system.
	 *
	 * String - Label of a knowledge field of the component
	 * MetadataWrapper - knowledge field value together with its meta data
	 */
	public Map<Component, Map<String, List<CorrelationMetadataWrapper<? extends Object>>>> knowledgeHistoryOfAllComponents;

	/**
	 * Computed distance bounds that ensures the correlation between the data satisfies given confidence level.
	 * If the data are not correlated the value stored is Double.NaN.
	 * The bound applies to the distance of knowledge values identified by the first label in the LabelPair.
	 */
	public Map<LabelPair, BoundaryValueHolder> distanceBounds;

	/**
	 * Create an instance of the {@link CorrelationManager} that will hold
	 * a reference to the given system {@link EnsembleManager}.
	 * @param ensembleManager The system {@link EnsembleManager}.
	 */
	public CorrelationManager(ComponentManager componentManager, ConnectorManager connectorManager) {
		if(componentManager == null){
			throw new IllegalArgumentException(String.format("The \"%s\" argument is null.", "componentManager"));
		}
		if(connectorManager == null){
			throw new IllegalArgumentException(String.format("The \"%s\" argument is null.", "connectorManager"));
		}
		
		knowledgeHistoryOfAllComponents = new HashMap<>();
		analyzedFailures = new HashMap<>();
		handledFailures = new HashMap<>();
		distanceBounds = new HashMap<>();

		this.componentManager = componentManager;
		this.connectorManager = connectorManager;
	}
	

	public void setVerbosity(boolean verbosity){
		verbose = verbosity;
	}

	public void setDumpValues(boolean dumpValues){
		this.dumpValues = dumpValues;
	}

	/**
	 * For quick debugging.
	 */
	public void printHistory(){
		if(!dumpValues){
			return;
		}
		
		StringBuilder b = new StringBuilder(1024);
		b.append("Printing global history...\n");

		for (Component component : knowledgeHistoryOfAllComponents.keySet()) {

			b.append("Component " + component.toString() + "\n");

			Map<String, List<CorrelationMetadataWrapper<? extends Object>>> componentHistory =
					knowledgeHistoryOfAllComponents.get(component);
			if(componentHistory == null){
				System.out.println(String.format("Knowledge of %s not found.", component.toString()));
				continue;
			}
			for (String field : componentHistory.keySet()) {
				b.append("\t" + field + ":\n");

				b.append("\ttime: ");
				List<CorrelationMetadataWrapper<? extends Object>> values = componentHistory.get(field);
				for (CorrelationMetadataWrapper<? extends Object> value : values) {
					b.append(value.getTimestamp() + ", ");
				}
				b.delete(b.length()-2, b.length());
				b.append("\n\tvalues: ");
				for (CorrelationMetadataWrapper<? extends Object> value : values) {
					b.append(value.getValue() + ", ");
				}
				b.delete(b.length()-2, b.length());
				b.append("\n\n");
			}

		}

		b.append("Printing correlation bounds...\n");

		for(LabelPair labels : distanceBounds.keySet()){
			b.append(String.format("%s -> %s : %.2f\n",
					labels.getFirstLabel(),
					labels.getSecondLabel(),
					distanceBounds.get(labels).getBoundary()));
		}

		System.out.println(b.toString());
	}

	@Override
	public void monitor() {
		for(Component c : componentManager.getComponents()){
			Map<String, List<CorrelationMetadataWrapper<?>>> memberKnowledgeHistory =
				knowledgeHistoryOfAllComponents.get(c);
			if (memberKnowledgeHistory == null) {
				memberKnowledgeHistory = new HashMap<>();
				knowledgeHistoryOfAllComponents.put(c, memberKnowledgeHistory);
			}
			
			Map<String, Object> knowledge = c.getKnowledge();
			for(String knowlegeField : knowledge.keySet()){
				Object o = knowledge.get(knowlegeField);
				// ignore fields that are not specified as CorrelationMetadataWrapper instances
				if (o instanceof CorrelationMetadataWrapper) {
					addFieldToHistory(memberKnowledgeHistory, (CorrelationMetadataWrapper<?>) o);
				}
			}
		}
	}
	
	/**
	 * Adds field to component field history.
	 * 
	 * @param histories
	 *            component field histories
	 * @param field
	 *            field value
	 */
	static private void addFieldToHistory(final Map<String, List<CorrelationMetadataWrapper<?>>> histories,
			final CorrelationMetadataWrapper<?> field) {
		List<CorrelationMetadataWrapper<?>> fieldHistory = histories.get(field.getName());
		if (fieldHistory == null) {
			fieldHistory = new ArrayList<>();
			histories.put(field.getName(), fieldHistory);
		}
		fieldHistory.add(field);
	}

	/**
	 * Check whether the new ensemble inferred by correlation is needed.
	 * 
	 * @returns true if the ensemble is needed. false otherwise.
	 */
	@Override
	public boolean analyze() {
		for(Component component : componentManager.getComponents()){
			Set<String> failedKnowledge = component.getFaultyKnowledge();
			if(!failedKnowledge.isEmpty()
					&& (!handledFailures.containsKey(component)
							|| !handledFailures.get(component).containsAll(failedKnowledge))){
				analyzedFailures.put(component, failedKnowledge); // TODO: check the condition
			}
		}
		
		return !analyzedFailures.isEmpty();
	}

	/**
	 * Method that measures the correlation between the data in the system
	 */
	@Override
	public void plan() {
		if(verbose){
			System.out.println("Correlation process started...");
		}

		for(LabelPair labels : getAllLabelPairs(knowledgeHistoryOfAllComponents)){
			List<DistancePair> distances = computeDistances(knowledgeHistoryOfAllComponents, labels);
			double boundary = getDistanceBoundary(distances, labels);
			if(verbose){
				System.out.println(String.format("%s -> %s", labels.getFirstLabel(), labels.getSecondLabel()));
				System.out.println(String.format("Boundary: %f", boundary));
			}
			if(distanceBounds.containsKey(labels)){
				// Update existing boundary (automatically handles "hasChanged" flag)
				distanceBounds.get(labels).setBoundary(boundary);
			} else {
				// Create new boundary value (by default "hasChanged" flag is true
				distanceBounds.put(labels, new BoundaryValueHolder(boundary));
			}
		}		
	}

	/**
	 * Deploys, activates and deactivates correlation ensembles based on the current
	 * correlation of the data in the system.
	 */
	@Override
	public void execute() {
		if(verbose){
			System.out.println("Correlation ensembles management process started...");
		}

		for(Component component : analyzedFailures.keySet()){
			for(String faultyKnowledge : analyzedFailures.get(component)){
				for(LabelPair labels : distanceBounds.keySet()){
					final String correlationFilter = labels.getFirstLabel();
					final String correlationSubject = labels.getSecondLabel();
					final BoundaryValueHolder distance = distanceBounds.get(labels);
					
					if(!correlationSubject.equals(faultyKnowledge)){
						// Consider only faulty knowledge
						continue;
					}
					
					final String connectorName = correlationFilter + "_" + correlationSubject;
					if (!distance.isValid()) {
						if(verbose){
							System.out.println(String.format("Undeploying ensemble %s",	connectorName));
						}
						// Undeploy the ensemble if the meta-adaptation is stopped or the correlation between the data is not reliable
						connectorManager.addConnector(null, new MediatedKnowledge(
								correlationFilter, correlationSubject));
					} else if (distance.hasChanged()) {
						// Re-deploy the ensemble only if the distance has changed since the last time and if it is valid						
						if(verbose){
							System.out.println(String.format("Deploying ensemble %s", connectorName));
						}
						DynamicConnector connector = connectorManager.addConnector(
								new Predicate<Map<String, Object>>(){

									@Override
									public boolean test(Map<String, Object> params) {
										Object memberFilter = params.get(MEMBER_FILTER_FIELD);
										Object coordFilter = params.get(COORD_FILTER_FIELD);
										
										return KnowledgeMetadataHolder.distance(correlationFilter, memberFilter, coordFilter) < distance.getBoundary();
									}
									
								},
								new MediatedKnowledge(correlationFilter, correlationSubject));
						// Add ports
						Set<String> connectorKnowledge = new HashSet<String>(Arrays.asList(
								new String[]{correlationFilter, correlationSubject}));
						connector.addPort(connectorKnowledge, Kind.Comsumer);
						connector.addPort(connectorKnowledge, Kind.Producer);
						component.addPort(connectorKnowledge);
						for(Component otherComponent : componentManager.getComponents()){
							if(otherComponent.equals(component)){
								// Consider only other components than the failed one
								continue;
							}
							otherComponent.addPort(connectorKnowledge);
						}
					
						// Mark the boundary as !hasChanged since the new value is used
						distanceBounds.get(labels).boundaryUsed();
					} else if(verbose){
						System.out.println(String.format(
								"Omitting deployment of ensemble %s since the bound hasn't changed (much).", connectorName));
					}
				}
			}
		}
	}

	/**
	 * Returns a list of all the pairs of labels that are common to both the specified components.
	 * All the pairs are inserted in both the possible ways [a,b] and [b,a].
	 * @param component1Id The ID of the first component.
	 * @param component2Id The ID of the second component.
	 * @return The list of all the pairs of labels that are common to both the specified components.
	 * All the pairs are inserted in both the possible ways [a,b] and [b,a].
	 */
	private List<LabelPair> getLabelPairs(
			Map<Component, Map<String, List<CorrelationMetadataWrapper<? extends Object>>>> history,
			ComponentPair components){
		List<LabelPair> labelPairs = new ArrayList<LabelPair>();

		Set<String> c1Labels = history.get(components.component1).keySet();
		Set<String> c2Labels = history.get(components.component2).keySet();

		// For all the label pairs
		for(String label1 : c1Labels){
			for(String label2 : c1Labels){
				if(label1.equals(label2)){
					// The pair mustn't contain one label twice
					continue;
				}
				if(c2Labels.contains(label1)
						&& c2Labels.contains(label2)){
					// Both the components has to contain both the labels
					labelPairs.add(new LabelPair(label1, label2));
				}
			}
		}

		return labelPairs;
	}

	/**
	 * Returns a set of all label pairs available among all the components in the system.
	 * @param history The history of knowledge of all the components in the system.
	 * @return The set of all label pairs available among all the components in the system.
	 */
	private Set<LabelPair> getAllLabelPairs(
			Map<Component, Map<String, List<CorrelationMetadataWrapper<? extends Object>>>> history){
		Set<LabelPair> labelPairs = new HashSet<LabelPair>();

		for(ComponentPair components : getComponentPairs(history.keySet()))
		{
			labelPairs.addAll(getLabelPairs(history, components));
		}

		return labelPairs;
	}

	/**
	 * Returns a list of all the pairs of components IDs from the given set of
	 * components IDs. The ordering of the components in the pair doesn't matter,
	 * therefore no two pairs with inverse ordering of the same two components
	 * are returned. As well as no pair made of a single component is returned.
	 * @param components The set of components.
	 * @return The list of pairs of components IDs.
	 */
	private List<ComponentPair> getComponentPairs(Set<Component> components){
		List<ComponentPair> componentPairs = new ArrayList<>();

		Component[] componentArr = components.toArray(new Component[components.size()]);
		for(int i = 0 ; i < componentArr.length; i++){
			for(int j = i+1; j < componentArr.length; j++){
				componentPairs.add(new ComponentPair(componentArr[i], componentArr[j]));
			}
		}

		return componentPairs;
	}

	/** Get all the components containing the given pair of knowledge fields.
	 * @param history The history of knowledge of all the components in the system.
	 * @param labels The pair knowledge fields required the components to have.
	 * @return All the components containing the given pair of knowledge fields.
	 */
	private Set<Component> getComponents(
			Map<Component, Map<String, List<CorrelationMetadataWrapper<? extends Object>>>> history,
			LabelPair labels){

		Set<Component> components = new HashSet<>(history.keySet());

		for(Component component : history.keySet()){
			if(!history.get(component).keySet().contains(labels.getFirstLabel())
					|| !history.get(component).keySet().contains(labels.getSecondLabel())){
				// If the component doesn't contain both the specified knowledge fields remove it
				components.remove(component);
			}
		}

		return components;
	}

	/**
	 * Returns a list of knowledge values identified by given labels from given components.
	 * @param history The history of knowledge of all the components in the system.
	 * @param components A pair of components containing the given pair of knowledge fields.
	 * @param labels The pair knowledge fields the values will be extracted from.
	 * @return The list of knowledge values identified by given labels from given components.
	 */
	private List<KnowledgeQuadruple> extractKnowledgeHistory(
			Map<Component, Map<String, List<CorrelationMetadataWrapper<? extends Object>>>> history,
			ComponentPair components,
			LabelPair labels){

		List<KnowledgeQuadruple> knowledgeVectors = new ArrayList<>();
		List<CorrelationMetadataWrapper<? extends Object>> c1Values1 = new ArrayList<>(
				history.get(components.component1).get(labels.getFirstLabel()));
		List<CorrelationMetadataWrapper<? extends Object>> c1Values2 = new ArrayList<>(
				history.get(components.component1).get(labels.getSecondLabel()));
		List<CorrelationMetadataWrapper<? extends Object>> c2Values1 = new ArrayList<>(
				history.get(components.component2).get(labels.getFirstLabel()));
		List<CorrelationMetadataWrapper<? extends Object>> c2Values2 = new ArrayList<>(
				history.get(components.component2).get(labels.getSecondLabel()));

		KnowledgeQuadruple values = getMinCommonTimeSlotValues(
				c1Values1, c1Values2, c2Values1, c2Values2);
		if(values == null && verbose){
			System.out.println(String.format("Correlation for [%s:%s]{%s -> %s} Skipped",
					components.component1, components.component2,
					labels.getFirstLabel(), labels.getSecondLabel()));
		}
		long timeSlot = -1;
		while(values != null){
			timeSlot = values.timeSlot;
			knowledgeVectors.add(values);

			if(verbose){
				System.out.println(String.format("Correlation for [%s:%s]{%s -> %s}(%d)",
					components.component1, components.component2,
					labels.getFirstLabel(), labels.getSecondLabel(), timeSlot));
			}
			
			removeEarlierValuesForTimeSlot(c1Values1, timeSlot);
			removeEarlierValuesForTimeSlot(c1Values2, timeSlot);
			removeEarlierValuesForTimeSlot(c2Values1, timeSlot);
			removeEarlierValuesForTimeSlot(c2Values2, timeSlot);
			values = getMinCommonTimeSlotValues(c1Values1, c1Values2, c2Values1, c2Values2);
		}

		return knowledgeVectors;
	}

	/** Returns a matrix of distances and distance classes for given knowledge fields among all the components.
	 * @param history The history of knowledge of all the components in the system.
	 * @param labels The pair knowledge fields the values will be extracted from.
	 * @return The matrix of distances and distance classes for given knowledge fields among all the components.
	 */
	private List<DistancePair> computeDistances(
			Map<Component, Map<String, List<CorrelationMetadataWrapper<? extends Object>>>> history,
			LabelPair labels){

		List<KnowledgeQuadruple> knowledgeVectors = new ArrayList<>();

		Set<Component> components = getComponents(history, labels);
		List<ComponentPair> componentPairs = getComponentPairs(components);
		for(ComponentPair componentPair : componentPairs){
			knowledgeVectors.addAll(extractKnowledgeHistory(history, componentPair, labels));
		}

		List<DistancePair> distancePairs = new ArrayList<>();

		for(KnowledgeQuadruple knowledge : knowledgeVectors){
			// Consider only operational fields
			if(knowledge.c1Value1.isOperational() && knowledge.c2Value1.isOperational()
					&& knowledge.c1Value2.isOperational() && knowledge.c2Value2.isOperational()){
				double distance = KnowledgeMetadataHolder.distance(
						labels.getFirstLabel(),
						knowledge.c1Value1.getValue(),
						knowledge.c2Value1.getValue());
				DistanceClass distanceClass = KnowledgeMetadataHolder.classifyDistance(
						labels.getSecondLabel(),
						knowledge.c1Value2.getValue(),
						knowledge.c2Value2.getValue());
				distancePairs.add(new DistancePair(distance, distanceClass, knowledge.c1Value1.getTimestamp()));
			}
		}

		if (dumpValues) {
			StringBuilder b = new StringBuilder();
			b.append("Computed distances\n");
			fillDistances(distancePairs, b);
			System.out.println(b.toString());
		}

		return distancePairs;
	}

	/**
	 * Returns the distance boundary of the knowledge identified by the first label in the given labels,
	 * that ensures the satisfaction of confidence level by the correlation of the knowledge identified by the labels.
	 * Double.NaN if returned if the confidence level can't be satisfied.
	 * @param distancePairs A list of distances of the knowledge labeled by the first label and distance
	 * classes of the knowledge labeled by the second label.
	 * @param labels The labels identifying the knowledge.
	 * @return The distance boundary of the knowledge identified by the first label in the given labels,
	 * that ensures the satisfaction of confidence level by the correlation of the knowledge identified by the labels.
	 * Double.NaN if returned if the confidence level can't be satisfied.
	 */
	private double getDistanceBoundary(List<DistancePair> distancePairs, LabelPair labels){
		// Sort the data by the distance of first knowledge field
		Collections.sort(distancePairs);
		if(dumpValues) {
			StringBuilder b = new StringBuilder();
			b.append("Sorted distances\n");
			fillDistances(distancePairs, b);
			System.out.println(b.toString());
		}
		// Count the correlation for all the distances based on all smaller distances than the computed one
		List<Double> correlations = new ArrayList<>(Collections.nCopies(distancePairs.size(), Double.NaN));
		int closeCnt = 0;
		for(int i = 0; i < distancePairs.size(); i++){
			if(distancePairs.get(i).distanceClass == DistanceClass.Close){
				closeCnt++;
			}
			double corr = ((double) closeCnt) / ((double) i);
			correlations.set(i, corr);
		}
		// Find the greatest distance that satisfies the correlation level
		double confidenceLevel = KnowledgeMetadataHolder.getConfidenceLevel(labels.getSecondLabel());
		for(int i = distancePairs.size() - 1; i >= 0; i--){
			if(correlations.get(i) >= confidenceLevel){
				return distancePairs.get(i).distance;
			}
		}

		return Double.NaN;
	}

	/**
	 * Fill the given StringBuilder with the given values.
	 * For debug print purposes.
	 * @param distancePairs The values to be filled into the builder.
	 * @param builder The StringBuilder to be filled.
	 */
	private void fillDistances(List<DistancePair> distancePairs, StringBuilder builder){
		builder.append("time: ");
		for(DistancePair dp : distancePairs)
		{
			builder.append(dp.timestamp + ", ");
		}
		builder.delete(builder.length()-2, builder.length());
		builder.append("\n");
		builder.append("distance: ");
		for(DistancePair dp : distancePairs)
		{
			builder.append(String.format(Locale.ENGLISH, "%.1f, ", dp.distance));
		}
		builder.delete(builder.length()-2, builder.length());
		builder.append("\n");
		builder.append("class: ");
		for(DistancePair dp : distancePairs)
		{
			builder.append(dp.distanceClass + ", ");
		}
		builder.delete(builder.length()-2, builder.length());
		builder.append("\n");
	}

	/**
	 * Provides a quadruple of values with the smallest common time slot.
	 * @param c1Values1 List of values of component 1 for label 1.
	 * @param c1Values2 List of values of component 1 for label 2.
	 * @param c2Values1 List of values of component 2 for label 1.
	 * @param c2Values2 List of values of component 2 for label 2.
	 * @return A quadruple of values with the smallest common time slot.
	 */
	private KnowledgeQuadruple getMinCommonTimeSlotValues(
			List<CorrelationMetadataWrapper<? extends Object>> c1Values1,
			List<CorrelationMetadataWrapper<? extends Object>> c1Values2,
			List<CorrelationMetadataWrapper<? extends Object>> c2Values1,
			List<CorrelationMetadataWrapper<? extends Object>> c2Values2){
		// Supposing that c1Values1 are sorted by timestamps
		for(CorrelationMetadataWrapper<? extends Object> c1Value1 : c1Values1){
			long timeSlot = c1Value1.getTimestamp() / TIME_SLOT_DURATION;
			CorrelationMetadataWrapper<? extends Object> c1Value2 =
					getFirstValueForTimeSlot(c1Values2, timeSlot);
			CorrelationMetadataWrapper<? extends Object> c2Value1 =
					getFirstValueForTimeSlot(c2Values1, timeSlot);
			CorrelationMetadataWrapper<? extends Object> c2Value2 =
					getFirstValueForTimeSlot(c2Values2, timeSlot);
			if(c1Value2 != null && c2Value1 != null && c2Value2 != null){
				return new KnowledgeQuadruple(c1Value1, c1Value2,
											  c2Value1, c2Value2, timeSlot);
			}
		}
		return null;
	}

	/**
	 * Returns the first value within the given time slot.
	 * @param values The list of values from which the required value is extracted.
	 * @param timeSlot The required time slot for the extracted value.
	 * @return The first value within the given time slot.
	 */
	private CorrelationMetadataWrapper<? extends Object> getFirstValueForTimeSlot(
			List<CorrelationMetadataWrapper<? extends Object>> values, long timeSlot){
		CorrelationMetadataWrapper<? extends Object> earliestValue = null;
		for(CorrelationMetadataWrapper<? extends Object> value : values){
			long valueTimeSlot = value.getTimestamp() / TIME_SLOT_DURATION;
			if(valueTimeSlot == timeSlot){
				if(earliestValue == null
						|| earliestValue.getTimestamp() > value.getTimestamp()){
					earliestValue = value;
				}
			}
		}

		return earliestValue;
	}

	/**
	 * Removes all the values that have belong to the specified time slot or any preceding,
	 * from the given list of values.
	 * @param values The list of values from which the specified values will be removed.
	 * @param timeSlot The time slot for which (and for all preceding) the values will
	 * be removed.
	 */
	private void removeEarlierValuesForTimeSlot(
			List<CorrelationMetadataWrapper<? extends Object>> values, long timeSlot){
		List<CorrelationMetadataWrapper<? extends Object>> toRemove = new ArrayList<>();
		for(CorrelationMetadataWrapper<? extends Object> value : values){
			long valueTimeSlot = value.getTimestamp() / TIME_SLOT_DURATION;
			if(valueTimeSlot <= timeSlot){
				toRemove.add(value);
			}
		}
		values.removeAll(toRemove);
	}

}
