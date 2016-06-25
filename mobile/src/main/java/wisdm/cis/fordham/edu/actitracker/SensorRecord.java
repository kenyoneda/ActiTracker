package wisdm.cis.fordham.edu.actitracker;

import java.io.Serializable;

/**
 * Class to hold SensorEvent data. Created because references to SensorEvent instances can be lost.
 */
public class SensorRecord implements Serializable {
    private long timestamp;
    private float[] values;

    private static final long serialVersionUID = 1L;

    public SensorRecord(long timestamp, float[] values) {
        this.timestamp = timestamp;
        this.values = values;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float[] getValues() {
        return values;
    }

    // Format to write files
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (float value : values) {
            sb.append(',');
            sb.append(value);
        }
        return timestamp + sb.toString();
    }
}