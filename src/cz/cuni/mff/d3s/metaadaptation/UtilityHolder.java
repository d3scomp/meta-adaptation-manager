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

/**
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class UtilityHolder {
	private double utilitySum;
	private int utilityCnt;
	
	public UtilityHolder(){
		utilitySum = 0;
		utilityCnt = 0;
	}
	
	public UtilityHolder(double utilitySum, int utilityCnt){
		this.utilitySum = utilitySum;
		this.utilityCnt = utilityCnt;
	}
	
	public void addMeasurement(double measurement){
		utilitySum += measurement;
		utilityCnt++;
	}
	
	public double getUtility(){
		if(utilityCnt == 0){
			return 0;
		}
		
		return utilitySum / (double) utilityCnt;
	}
}
