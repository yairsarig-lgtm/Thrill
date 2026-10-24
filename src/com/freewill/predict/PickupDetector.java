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
 * so its TOP points at the spectator. When the phone is lifted and tilted past the trigger
 * angle it reports a {@link Pickup}:
 *   - facing: who the screen ends up facing (routine 1), from the rotation sensor and a
 *     "towards spectator" reference stored while the phone rests;
 *   - edge: which edge rose first, as seen from the magician's seat (routine 2);
 *   - paused: whether the phone was held still, slightly lifted, for the pause time
 *     before reaching the trigger angle (routine 2).
 */
public class PickupDetector implements SensorEventListener {

    public static final int FACE_SPECTATOR_TOP_UP = 0;
    public static final int FACE_SPECTATOR_TOP_DOWN = 1;
    public static final int FACE_MAGICIAN_TOP_UP = 2;
    public static final int FACE_MAGICIAN_TOP_DOWN = 3;

    /** Edges as seen from the magician's seat. Front = the spectator's side. */
    public static final int EDGE_FRONT = 0;
    public static final int EDGE_BACK = 1;
    public static final int EDGE_LEFT = 2;
    public static final int EDGE_RIGHT = 3;

    /** Everything known about one pickup. */
    public static final class Pickup {
        public final int facing;
        public final int edge;
        public final boolean paused;

        public Pickup(int facing, int edge, boolean paused) {
            this.facing = facing;
            this.edge = edge;
            this.paused = paused;
        }

        public boolean spectatorFacing() {
            return facing == FACE_SPECTATOR_TOP_UP || facing == FACE_SPECTATOR_TOP_DOWN;
        }
    }

    public static String stateName(int s) {
        return (s == FACE_SPECTATOR_TOP_UP || s == FACE_SPECTATOR_TOP_DOWN)
                ? "Picked up by the spectator" : "Picked up by the magician";
    }

    public static String edgeName(int edge, boolean paused) {
        switch (edge) {
            case EDGE_FRONT: return paused ? "Front edge · pause" : "Front edge";
            case EDGE_BACK: return paused ? "Back edge · pause" : "Back edge";
            case EDGE_LEFT: return "Left edge";
            default: return "Right edge";
        }
    }

    public enum Phase { WAITING, ARMED, LIFTING, FIRED }

    public interface Listener {
        void onArmed();
        void onDisarmed();
        void onPaused();
        void onPickup(Pickup p);
    }

    // Absolute tilt away from lying flat, screen down.
    private static final float ARM_MAX_DEG = 10f;      // counts as lying face down
    private static final float REST_DEG = 4f;          // settled back down
    private static final float LIFT_DEG = 6f;          // one end has started to rise
    private static final long ARM_MS = 1200;
    private static final long HOLD_MS = 40;            // tilt must hold this long (ignores bumps)

    // Lift measured from the resting position, so an uneven table doesn't matter.
    private static final float PAUSE_MIN_LIFT = 3f;    // edge visibly off the table
    private static final float PAUSE_TOLERANCE = 1.5f; // allowed drift while "still"
    private static final float REST_LIFT = 2f;         // back down: forget any pause

    private final SensorManager sm;
    private final Sensor gravitySensor;
    private final boolean gravityIsRaw;   // using the accelerometer, so low-pass it
    private final Sensor rotSensor;
    private final Listener listener;

    private float triggerDeg = 15f;
    private long pauseMs = 1000;
    private long overSince = -1;
    private final float[] g = new float[3];
    private final float[] rest = new float[3];
    private boolean haveRest;
    private final float[] rot = new float[9];
    private boolean haveRot;
    private final float[] towardSpectator = new float[2]; // world horizontal unit vector
    private boolean haveRef;

    private boolean paused;
    private float anchorLift;
    private long anchorSince = -1;

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

    /** Tilt, in degrees from flat, at which the prediction is written. */
    public void setTrigger(float degrees) { triggerDeg = degrees; }

    /** How long the phone must be held still, slightly lifted, to count as a pause. */
    public void setPauseMs(long ms) { pauseMs = ms; }

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
        haveRest = false;
        clearPause();
    }

    public Phase getPhase() { return phase; }
    public float[] getGravity() { return g; }
    public float getTilt() { return tilt(); }
    public boolean isPaused() { return paused; }

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
        // Fallback without a rotation sensor: assume a simple end-lift.
        return g[1] > 0;
    }

    /**
     * Which edge has risen. Lying face down with the top toward the spectator, the phone's
     * +y axis points at the spectator and its +x axis at the magician's LEFT. The gravity
     * reading along an axis grows when that end of the phone goes up.
     */
    private int currentEdge() {
        float dx = g[0] - (haveRest ? rest[0] : 0f);
        float dy = g[1] - (haveRest ? rest[1] : 0f);
        if (Math.abs(dy) >= Math.abs(dx)) return dy > 0 ? EDGE_FRONT : EDGE_BACK;
        return dx > 0 ? EDGE_LEFT : EDGE_RIGHT;
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

    /** Degrees the phone is tilted away from lying flat on its screen (0 = flat face down). */
    private float tilt() {
        float n = norm(g);
        if (n < 1f) return 90f;
        double c = Math.max(-1.0, Math.min(1.0, -g[2] / n));
        return (float) Math.toDegrees(Math.acos(c));
    }

    /** Degrees the phone has moved away from where it rested when armed. */
    private float lift() {
        if (!haveRest) return tilt();
        float a = norm(g), b = norm(rest);
        if (a < 1f || b < 1f) return 90f;
        double c = (g[0] * rest[0] + g[1] * rest[1] + g[2] * rest[2]) / (a * b);
        return (float) Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, c))));
    }

    private static float norm(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private void clearPause() {
        paused = false;
        anchorSince = -1;
    }

    private void trackPause(long now, float t) {
        float l = lift();
        if (l < REST_LIFT) {               // resting: no pause yet
            clearPause();
            return;
        }
        if (paused || l < PAUSE_MIN_LIFT || t >= triggerDeg) {
            anchorSince = -1;
            return;
        }
        if (anchorSince < 0 || Math.abs(l - anchorLift) > PAUSE_TOLERANCE) {
            anchorLift = l;
            anchorSince = now;
        } else if (now - anchorSince >= pauseMs) {
            paused = true;
            listener.onPaused();
        }
    }

    private void step() {
        long now = SystemClock.uptimeMillis();
        float t = tilt();

        switch (phase) {
            case WAITING:
                if (t < ARM_MAX_DEG) {
                    if (faceDownSince < 0) faceDownSince = now;
                    if (now - faceDownSince >= ARM_MS) {
                        captureReference();
                        System.arraycopy(g, 0, rest, 0, 3);
                        haveRest = true;
                        clearPause();
                        phase = Phase.ARMED;
                        listener.onArmed();
                    }
                } else {
                    faceDownSince = -1;
                }
                break;

            case ARMED:
                trackPause(now, t);
                if (t < REST_DEG && lift() < REST_LIFT) {
                    captureReference(); // keep the references fresh while it rests
                    for (int i = 0; i < 3; i++) rest[i] = 0.95f * rest[i] + 0.05f * g[i];
                } else if (t > LIFT_DEG) {
                    phase = Phase.LIFTING;
                    overSince = -1;
                }
                break;

            case LIFTING:
                trackPause(now, t);
                if (t < REST_DEG) {         // put back down: still armed
                    phase = Phase.ARMED;
                    break;
                }
                if (t >= triggerDeg) {
                    if (overSince < 0) overSince = now;
                    if (now - overSince >= HOLD_MS) {
                        Pickup p = new Pickup(currentState(), currentEdge(), paused);
                        phase = Phase.FIRED;
                        listener.onPickup(p);
                    }
                } else {
                    overSince = -1;
                }
                break;

            case FIRED:
                if (rearmAfterFire && t < ARM_MAX_DEG) {
                    faceDownSince = now;
                    haveRest = false;
                    clearPause();
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
