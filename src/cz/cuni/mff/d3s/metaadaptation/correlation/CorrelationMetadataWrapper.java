package cz.cuni.mff.d3s.metaadaptation.correlation;

import java.io.Serializable;

/**
 * A wrapper for a knowledge field that adds the following metadata to that field:
 * <ul>
 * <li>timestamp - when the data has been taken.</li>
 * <li>operational - whether the sensor that provides the data is working.</li>
 * </ul>
 * @param <T> The type of the knowledge that is wrapped.
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 */
public class CorrelationMetadataWrapper<T> implements Serializable{
	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = -1190064726886324861L;
	
	/**
	 * The value of the wrapped knowledge field.
	 */
	private T value;
	/**
	 * The name of the wrapped knowledge field.
	 */
	private String name;
	/**
	 * The time when the knowledge value has been obtained.
	 */
	private long timestamp;
	/**
	 * Indicate whether the sensor that provides the knowledge field data works.
	 */
	private boolean operational;
	
	/**
	 * Wrap the knowledge field value into the {@link CorrelationMetadataWrapper}.
	 * The wrapper is created with the timestamp = 0 and operational = true.
	 * @param value The knowledge field value to be wrapped.
	 */
	public CorrelationMetadataWrapper(T value, String name){
		this(value, name, 0);
	}
	
	/**
	 * Wrap the knowledge field value into the {@link CorrelationMetadataWrapper}.
	 * The wrapper is created with the timestamp = 0 and operational = true.
	 * @param value The knowledge field value to be wrapped.
	 */
	public CorrelationMetadataWrapper(T value, String name, long time){
		this.value = value;
		this.name = name;
		timestamp = time;
		operational = true;
	}
	
	/**
	 * Get the knowledge field value.
	 * @return The knowledge field value.
	 */
	public T getValue(){
		return value;
	}
	
	/**
	 * Get the knowledge field name.
	 * @return The knowledge field name.
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Get the knowledge field value timestamp.
	 * @return The knowledge field value timestamp.
	 */
	public long getTimestamp(){
		return timestamp;
	}
	
	/**
	 * Set the knowledge field value with the given timestamp.
	 * @param value The knowledge field value.
	 * @param timestamp The knowledge field value timestamp.
	 */
	public void setValue(T value, long timestamp){
		this.value = value;
		this.timestamp = timestamp;
	}
	
	/**
	 * Indicates whether the sensor that provides the knowledge field value works.
	 * @return True if the sensor works. False it the sensor malfunctioned.
	 */
	public boolean isOperational(){
		return operational;
	}
	
	/**
	 * Set the {@link #operational} flag to false.
	 */
	public void malfunction(){
		operational = false;	
	}
	
	@Override
	public String toString() {
		return String.format("(%s:%s:%d:%s)", name, value, timestamp, operational ? "o" : "f");
	}

}
