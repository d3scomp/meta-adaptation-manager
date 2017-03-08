package cz.cuni.mff.d3s.metaadaptation.correlation;

import cz.cuni.mff.d3s.metaadaptation.correlation.CorrelationLevel.DistanceClass;

public class DistancePair implements Comparable<DistancePair> {

	public final double distance;
	public final DistanceClass distanceClass;
	// Just for debug printing
	public final long timestamp;
	
	public DistancePair(double distance, DistanceClass distanceClass, long timestamp){
		this.distance = distance;
		this.distanceClass = distanceClass;
		this.timestamp = timestamp;
	}

	@Override
	public int compareTo(DistancePair other) {
		return Double.compare(this.distance, other.distance);
	}
}
