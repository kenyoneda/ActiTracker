package wisdm.cis.fordham.edu.actitracker;

import java.io.Serializable;

/**
 * Created by Ken on /31/0516.
 */
public class ThreeTupleRecord implements Serializable {
    private long timestamp;
    private float x;
    private float y;
    private float z;

    private static final long serialVersionUID = 1L;

    public ThreeTupleRecord(long timestamp, float x, float y, float z) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    @Override
    public String toString() {
        return timestamp + "," + x + "," + y + "," + z;
    }
}
