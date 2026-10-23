package com.freewill.predict;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Watches the phone lying face down and decides how it is picked up.
 *
 * Performance rule: the phone is placed face down with its BOTTOM pointing at the magician,
 * so its TOP points at the spectator. While it lies there the detector keeps a reference
 * of which world direction the top points to. When the phone is lifted and tilted past the
 * trigger angle, it reports two things:
 *   - top up or top down (from gravity along the phone's long axis)
 *   - whether the screen faces the spectator's side or the magician's side
 *     (screen normal compared with the stored "towards spectator" direction).
 */
public class PickupDetector implements SensorEventListener {

    public static final int FACE_SPECTATOR_TOP_UP = 0;
    public static final int FACE_SPECTATOR_TOP_DOWN = 1;
    public static final int FACE_MAGICIAN_TOP_UP = 2;
    public static final int FACE_MAGICIAN_TOP_DOWN = 3;

    public static String stateName(int s) {
        return (s == FACE_SPECTATOR_TOP_UP || s == FACE_SPECTATOR_TOP_DOWN)
                ? "Picked up by the spectator" : "Picked up by the magician";
    }

    public enum Phase { WAITING, ARMED, LIFTING, FIRED }

    public interface Listener {
        void onArmed();
        void onDisarmed();
        void onPickup(int state);
    }

    private static final float FACE_DOWN_Z = -8.3f;   // gravity z when lying screen-down
    private static final float LIFT_Z = -6.5f;        // left the table
    private static final long ARM_MS = 1200;

    private final SensorManager sm;
    private final Sensor gravitySensor;
    private final boolean gravityIsRaw;   // using the accelerometer, so low-pass it
    private final Sensor rotSensor;
    private final Listener listener;

    private float trigger = 7.0f;
    private final float[] g = new float[3];
    private final float[] rot = new float[9];
    private boolean haveRot;
    private final float[] towardSpectator = new float[2]; // world horizontal unit vector
    private boolean haveRef;

    private Phase phase = Phase.WAITING;
    private long faceDownSince = -1;
    private boolean running;
    private boolean rearmAfterFire;

    public PickupDetector(Context c, Listener l) {
        listener = l;
        sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        Sensor grav = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gravityIsRaw = grav == null;
        gravitySensor = grav != null ? grav : sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor r = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (r == null) r = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        rotSensor = r;
    }

    public boolean hasRotationSensor() { return rotSensor != null; }

    public void setTrigger(float t) { trigger = t; }

    /** If true, after firing it waits for the phone to go face down again and re-arms (test mode). */
    public void setRearmAfterFire(boolean b) { rearmAfterFire = b; }

    public void start() {
        if (running) return;
        running = true;
        reset();
        if (gravitySensor != null) sm.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        if (rotSensor != null) sm.registerListener(this, rotSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        if (!running) return;
        running = false;
        sm.unregisterListener(this);
    }

    public void reset() {
        phase = Phase.WAITING;
        faceDownSince = -1;
        haveRef = false;
    }

    public Phase getPhase() { return phase; }
    public float[] getGravity() { return g; }

    /** Which pickup state the phone would report right now (for the live tester). */
    public int currentState() {
        boolean topUp = g[1] > 0;
        boolean facesSpectator = facesSpectator();
        if (facesSpectator) return topUp ? FACE_SPECTATOR_TOP_UP : FACE_SPECTATOR_TOP_DOWN;
        return topUp ? FACE_MAGICIAN_TOP_UP : FACE_MAGICIAN_TOP_DOWN;
    }

    private boolean facesSpectator() {
        if (haveRot && haveRef) {
            // Screen normal in world coords = third column of the rotation matrix.
            float nx = rot[2], ny = rot[5];
            return nx * towardSpectator[0] + ny * towardSpectator[1] > 0;
        }
        // Fallback without a rotation sensor: assume a simple end-lift. Lifting the far (top)
        // end leaves the screen facing the spectator, top up; lifting the near end leaves it
        // facing the magician, top down.
        return g[1] > 0;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        int type = e.sensor.getType();
        if (type == Sensor.TYPE_GAME_ROTATION_VECTOR || type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rot, e.values);
            haveRot = true;
            return;
        }
        if (gravityIsRaw) {
            final float a = 0.8f;
            for (int i = 0; i < 3; i++) g[i] = a * g[i] + (1 - a) * e.values[i];
        } else {
            System.arraycopy(e.values, 0, g, 0, 3);
        }
        step();
    }

    private void step() {
        long now = SystemClock.uptimeMillis();
        boolean faceDown = g[2] < FACE_DOWN_Z;

        switch (phase) {
            case WAITING:
                if (faceDown) {
                    if (faceDownSince < 0) faceDownSince = now;
                    if (now - faceDownSince >= ARM_MS) {
                        captureReference();
                        phase = Phase.ARMED;
                        listener.onArmed();
                    }
                } else {
                    faceDownSince = -1;
                }
                break;

            case ARMED:
                if (faceDown) {
                    captureReference(); // keep the reference fresh while it rests
                } else if (g[2] > LIFT_Z) {
                    phase = Phase.LIFTING;
                }
                break;

            case LIFTING:
                if (faceDown) {             // put back down: still armed
                    phase = Phase.ARMED;
                    break;
                }
                if (Math.abs(g[1]) > trigger) {
                    int state = currentState();
                    phase = Phase.FIRED;
                    listener.onPickup(state);
                }
                break;

            case FIRED:
                if (rearmAfterFire && faceDown) {
                    faceDownSince = now;
                    phase = Phase.WAITING;
                    listener.onDisarmed();
                }
                break;
        }
    }

    private void captureReference() {
        if (!haveRot) return;
        // Phone's top direction in world coords = second column of the rotation matrix.
        float tx = rot[1], ty = rot[4];
        float len = (float) Math.sqrt(tx * tx + ty * ty);
        if (len < 0.5f) return; // not flat enough to trust
        towardSpectator[0] = tx / len;
        towardSpectator[1] = ty / len;
        haveRef = true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
