package cz.cuni.mff.d3s.metaadaptation.correlation;

import java.io.Serializable;

/**
 * Holds a pair of components IDs.
 * Two {@link ComponentPair}s can be compared and they are equal whenever
 * they contain the same components IDs (the ordering of which doesn't matter).
 * 
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 */
public class ComponentPair implements Serializable {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 14064809845208208L;
	
	/**
	 * The ID of the first component.
	 */
	public final Component component1;
	/**
	 * The ID of the second component.
	 */
	public final Component component2;
	
	/**
	 * Creates a new instance of {@link ComponentPair} for the given components IDs.
	 * The ordering of the IDs doesn't matter.
	 * @param component1Id The ID of the first component.
	 * @param component2Id The ID of the second component.
	 */
	public ComponentPair(Component component1, Component component2){
		if(component1 == null) throw new IllegalArgumentException(
				String.format("The \"%s\" argument is null.", "component1"));
		if(component2 == null) throw new IllegalArgumentException(
				String.format("The \"%s\" argument is null.", "component2"));
		
		this.component1 = component1;
		this.component2 = component2;
	}
	
	/**
	 * Two {@link ComponentPair}s are equal if they contain the same
	 * components IDs.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object otherPair){
		if(otherPair == null){
			return false;
		}
		if(!(otherPair instanceof ComponentPair)){
			return false;
		}
		
		ComponentPair other = (ComponentPair) otherPair;
		
		return (this.component1.equals(other.component1)
				&& this.component2.equals(other.component2))
				|| (this.component1.equals(other.component2)
						&& this.component2.equals(other.component1));
	}
	
	@Override
	public int hashCode() {
		Component smaller, bigger;
		if(component1.hashCode() < component2.hashCode()){
			smaller = component1;
			bigger = component2;
		} else {
			smaller = component2;
			bigger = component1;
		}
		return String.format("%s;%s", smaller, bigger).hashCode();
	}
}
