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
package cz.cuni.mff.d3s.metaadaptation.componentisolation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation;

/**
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class ComponentIsolationManager implements MAPEAdaptation {
	
	private class ComponentFaults {
		public final Component component;
		public final Set<String> faultyKnowledge;
		public final Set<Port> portsToRemove;
		
		public ComponentFaults(Component component, Set<String> faultyKnowledge){
			this.component = component;
			this.faultyKnowledge = faultyKnowledge;
			portsToRemove = new HashSet<>();
		}
	}
	
	private final ComponentManager components;
	private final Set<ComponentFaults> isolationCandidates;
	private boolean verbose;
	
	public ComponentIsolationManager(ComponentManager componentManager){
		if(componentManager == null){
			throw new IllegalArgumentException(String.format("The %s argument is null.", "componentManager"));
		}
		
		components = componentManager;
		isolationCandidates = new HashSet<>();
	}

	public void setVerbosity(boolean verbosity){
		verbose = verbosity;
	}
	

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#monitor()
	 */
	@Override
	public void monitor() {
		for(Component component : components.getComponents()){
			Set<String> faults = component.getFaultyKnowledge();
			if(!faults.isEmpty()){
				isolationCandidates.add(new ComponentFaults(component, faults));
				
				if(verbose){
					StringBuilder builder = new StringBuilder();
					for(String fault : faults){
						builder.append("\"").append(fault).append("\", ");
					}
					// delete the last comma in the list created above
					builder.setLength(builder.length() - 2);
					System.out.println(String.format("Faulty knowledge %s found in %s.",
							builder.toString(), component));
					
					System.out.println("\nKNOWLEDGE:");
					Map<String, Object> values = component.getKnowledge();
					for(String k : values.keySet()){
						System.out.println(k + " : " + values.get(k));
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#analyze()
	 */
	@Override
	public boolean analyze() {
		if(isolationCandidates.isEmpty()){
			return false;
		}
		
		boolean canPortBeRemoved = false;
		for(ComponentFaults faultyComponent : isolationCandidates){
			for(Port port : faultyComponent.component.getPorts()){
				if(!Collections.disjoint(port.getExposedKnowledge(),
						faultyComponent.faultyKnowledge)){
					faultyComponent.portsToRemove.add(port);
					canPortBeRemoved = true;

					if(verbose){
						System.out.println(String.format("Port %s in %s can be removed.",
								port, faultyComponent.component));
					}
				}
			}
		}
		
		return canPortBeRemoved;
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#plan()
	 */
	@Override
	public void plan() {		
		if(verbose){
			for(ComponentFaults faultyComponent : isolationCandidates){
				for(Port port : faultyComponent.portsToRemove){
					System.out.println(String.format("Port %s in %s will be removed.",
									port, faultyComponent.component));
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#execute()
	 */
	@Override
	public void execute() {
		for(ComponentFaults candidate : isolationCandidates){
			for(Port port : candidate.portsToRemove){
				candidate.component.removePort(port);
				if(verbose){
					System.out.println(String.format("Port %s in %s removed.",
							port, candidate.component));
				}
			}
		}
		isolationCandidates.clear();
	}
	
}
