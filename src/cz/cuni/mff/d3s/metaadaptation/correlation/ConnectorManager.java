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
package cz.cuni.mff.d3s.metaadaptation.correlation;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public interface ConnectorManager {

	public final static String MEMBER_FILTER_FIELD = "memberFilter";
	public final static String COORD_FILTER_FIELD = "coordFilter";
	
	public class MediatedKnowledge {
		public final String correlationFilter;
		public final String correlationSubject;
		
		public MediatedKnowledge(String correlationFilter, String correlationSubject){
			this.correlationFilter = correlationFilter;
			this.correlationSubject = correlationSubject;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if(obj == null || !(obj instanceof MediatedKnowledge)){
				return false;
			}
			MediatedKnowledge other = (MediatedKnowledge) obj;
			return this.correlationFilter.equals(other.correlationFilter)
					&& this.correlationSubject.equals(other.correlationSubject);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return correlationFilter.hashCode() + 17 * correlationSubject.hashCode();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("%s_%s", correlationFilter, correlationSubject);
		}
	}
	
	public Set<DynamicConnector> getConnectors();
	public void addConnector(Predicate<Map<String, Object>> filter, MediatedKnowledge mediatedKnowledge);
}
