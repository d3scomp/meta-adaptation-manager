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

import java.util.HashSet;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation;

/**
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class ComponentIsolationManager implements MAPEAdaptation {
	
	private class ComponentPortPair {
		public final Component component;
		public final Port port;
		
		public ComponentPortPair(Component component, Port port){
			this.component = component;
			this.port = port;
		}
		
		public void removePort(){
			component.removePort(port);
		}
	}
	
	private final ComponentManager components;
	private final Set<ComponentPortPair> isolationCandidates;
	
	public ComponentIsolationManager(ComponentManager componentManager){
		if(componentManager == null){
			throw new IllegalArgumentException(String.format("The %s argument is null.", "componentManager"));
		}
		
		components = componentManager;
		isolationCandidates = new HashSet<>();
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#monitor()
	 */
	@Override
	public void monitor() {
		for(Component component : components.getComponents()){
			for(Port port : component.getPorts())
			component.monitorHealth(port);
		}
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#analyze()
	 */
	@Override
	public boolean analyze() {
		boolean defectDetected = false;
		for(Component component : components.getComponents()){
			for(Port port : component.getPorts()){
				boolean health = component.getHealth(port);
				if(health == false){
					defectDetected = true;
				}
			}
		}
		
		return defectDetected;
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#plan()
	 */
	@Override
	public void plan() {
		for(Component component : components.getComponents()){
			for(Port port : component.getPorts()){
				boolean health = component.getHealth(port);
				if(health == false){
					isolationCandidates.add(new ComponentPortPair(component, port));
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see cz.cuni.mff.d3s.metaadaptation.MAPEAdaptation#execute()
	 */
	@Override
	public void execute() {
		for(ComponentPortPair candidate : isolationCandidates){
			candidate.removePort();
		}
		isolationCandidates.clear();
	}
	
	
}
