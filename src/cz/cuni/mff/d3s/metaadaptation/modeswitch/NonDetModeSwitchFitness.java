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

/**
 * A function that computes the fitness of the system to guide
 * the system in the non-deterministic mode switching search.
 * 
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public interface NonDetModeSwitchFitness {
	
	/**
	 * The measured fitness of the system.
	 * @param currentTime The current simulation time.
	 * @param knowledgeValues Values of the required knowledge fields,
	 * 		specified by {@link #getKnowledgeNames()}.
	 * @return The measured fitness of the system.
	 */
	double getFitness(long currentTime, Component component);
	
	/**
	 * Forget so far measured values and start new measurement.
	 * Called when the system starts to operate with new parameters
	 * of non-deterministic mode switching.
	 */
	void restart();
}
