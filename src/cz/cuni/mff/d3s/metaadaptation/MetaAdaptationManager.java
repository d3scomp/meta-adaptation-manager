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
package cz.cuni.mff.d3s.metaadaptation;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class MetaAdaptationManager {
		
	public List<MAPEAdaptation> adaptations;
	
	public MetaAdaptationManager(){
		adaptations = new ArrayList<>();		
	}
	
	public void addAdaptation(MAPEAdaptation adaptation){
		adaptations.add(adaptation);
	}
	
	/**
	 * Run the MAPE loop of all registered {@link MAPEAdaptation}s.
	 */
	public void reason() {
		for(MAPEAdaptation adaptation : adaptations){
			adaptation.monitor();
			boolean isApplicable = adaptation.analyze();
			if(isApplicable){
				adaptation.plan();
				adaptation.execute();
			}
		}
		
	}
}
