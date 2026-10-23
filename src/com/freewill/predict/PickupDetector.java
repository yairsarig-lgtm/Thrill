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
 *
 * Fail-safe: if the phone is lifted off the table but kept flat (no tilt) for about a second,
 * it reports FAILSAFE instead.
 */
public class PickupDetector implements SensorEventListener {

    public static final int FACE_SPECTATOR_TOP_UP = 0;
    public static final int FACE_SPECTATOR_TOP_DOWN = 1;
    public static final int FACE_MAGICIAN_TOP_UP = 2;
    public static final int FACE_MAGICIAN_TOP_DOWN = 3;
    public static final int FAILSAFE = 4;

    public static String stateName(int s) {
        if (s == FAILSAFE) return "Held flat: fail-safe";
        return (s == FACE_SPECTATOR_TOP_UP || s == FACE_SPECTATOR_TOP_DOWN)
                ? "Picked up by the spectator" : "Picked up by the magician";
    }

    public enum Phase { WAITING, ARMED, HELD, LIFTING, FIRED }

    public interface Listener {
        void onArmed();
        void onDisarmed();
        void onPickup(int state);
    }

    // All angles are the tilt away from lying flat, screen down.
    private static final float ARM_MAX_DEG = 10f;      // counts as lying face down
    private static final float REST_DEG = 4f;          // settled back down
    private static final float LIFT_DEG = 6f;          // one end has started to rise
    private static final float FLAT_DEG = 8f;          // still "flat" while held in the hand
    private static final long ARM_MS = 1200;
    private static final long HOLD_MS = 40;            // tilt must hold this long (ignores bumps)

    // Fail-safe tuning (m/s², motion with gravity removed)
    private static final float LIFT_UP_ACCEL = 0.6f;   // upward push that means "lifted off the table"
    private static final long FAILSAFE_MS = 1000;      // how long it must be held flat
    private static final long SETTLE_MS = 350;         // ignore the lift itself when judging the hold
    private static final float HAND_TREMOR = 0.05f;    // average motion that means "in a hand"
    private static final float STILL = 0.04f;          // average motion that means "on the table"

    private final SensorManager sm;
    private final Sensor gravitySensor;
    private final boolean gravityIsRaw;   // using the accelerometer, so low-pass it
    private final Sensor accelSensor;
    private final Sensor rotSensor;
    private final Listener listener;

    private float triggerDeg = 15f;
    private boolean failsafeEnabled = true;
    private long overSince = -1;
    private final float[] g = new float[3];
    private final float[] rot = new float[9];
    private boolean haveRot;
    private final float[] towardSpectator = new float[2]; // world horizontal unit vector
    private boolean haveRef;

    private float motion;          // smoothed size of movement (gravity removed)
    private float upAccel;         // latest upward acceleration
    private long heldSince = -1;
    private float tremorSum;
    private int tremorN;
    private long stillSince = -1;

    private Phase phase = Phase.WAITING;
    private long faceDownSince = -1;
    private boolean running;
    private boolean rearmAfterFire;

    public PickupDetector(Context c, Listener l) {
        listener = l;
        sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        Sensor grav = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gravityIsRaw = grav == null;
        accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gravitySensor = grav != null ? grav : accelSensor;
        Sensor r = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (r == null) r = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        rotSensor = r;
    }

    public boolean hasRotationSensor() { return rotSensor != null; }

    /** Tilt, in degrees from flat, at which the prediction is written. */
    public void setTrigger(float degrees) { triggerDeg = degrees; }

    public void setFailsafeEnabled(boolean b) { failsafeEnabled = b; }

    /** If true, after firing it waits for the phone to go face down again and re-arms (test mode). */
    public void setRearmAfterFire(boolean b) { rearmAfterFire = b; }

    public void start() {
        if (running) return;
        running = true;
        reset();
        if (gravitySensor != null) sm.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        if (accelSensor != null && accelSensor != gravitySensor) {
            sm.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
        }
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
        heldSince = -1;
        stillSince = -1;
        haveRef = false;
    }

    public Phase getPhase() { return phase; }
    public float[] getGravity() { return g; }
    public float getMotion() { return motion; }

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
        if (type == Sensor.TYPE_ACCELEROMETER) {
            if (gravityIsRaw) {
                final float a = 0.8f;
                for (int i = 0; i < 3; i++) g[i] = a * g[i] + (1 - a) * e.values[i];
            }
            trackMotion(e.values);
            if (!gravityIsRaw) return;   // gravity events drive step() when they exist
        } else {
            System.arraycopy(e.values, 0, g, 0, 3);
        }
        step();
    }

    /** Movement with gravity removed: its size, and how much of it points upward. */
    private void trackMotion(float[] raw) {
        float lx = raw[0] - g[0], ly = raw[1] - g[1], lz = raw[2] - g[2];
        float mag = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        motion = 0.85f * motion + 0.15f * mag;
        float gn = (float) Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
        // The accelerometer's gravity reading points up, so this is upward acceleration.
        upAccel = gn < 1f ? 0f : (lx * g[0] + ly * g[1] + lz * g[2]) / gn;
        if (phase == Phase.HELD && heldSince > 0
                && SystemClock.uptimeMillis() - heldSince > SETTLE_MS) {
            tremorSum += mag;
            tremorN++;
        }
    }

    /** Degrees the phone is tilted away from lying flat on its screen (0 = flat face down). */
    private float tilt() {
        float n = (float) Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
        if (n < 1f) return 90f;
        double c = Math.max(-1.0, Math.min(1.0, -g[2] / n));
        return (float) Math.toDegrees(Math.acos(c));
    }

    public float getTilt() { return tilt(); }

    private void step() {
        long now = SystemClock.uptimeMillis();
        float t = tilt();

        switch (phase) {
            case WAITING:
                if (t < ARM_MAX_DEG) {
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
                if (t > LIFT_DEG) {
                    phase = Phase.LIFTING;
                    overSince = -1;
                } else if (failsafeEnabled && upAccel > LIFT_UP_ACCEL) {
                    phase = Phase.HELD;           // lifted straight up, still flat
                    heldSince = now;
                    tremorSum = 0;
                    tremorN = 0;
                } else if (t < REST_DEG) {
                    captureReference(); // keep the reference fresh while it rests
                }
                break;

            case HELD:
                if (t > LIFT_DEG) {               // it's a normal pickup after all
                    phase = Phase.LIFTING;
                    overSince = -1;
                    break;
                }
                if (t > FLAT_DEG) break;
                if (now - heldSince >= FAILSAFE_MS) {
                    float avg = tremorN > 0 ? tremorSum / tremorN : 0f;
                    if (avg > HAND_TREMOR) {
                        phase = Phase.FIRED;
                        stillSince = -1;
                        listener.onPickup(FAILSAFE);
                    } else {
                        phase = Phase.ARMED;      // it was only nudged on the table
                    }
                }
                break;

            case LIFTING:
                if (t < REST_DEG) {         // put back down: still armed
                    phase = Phase.ARMED;
                    break;
                }
                if (t >= triggerDeg) {
                    if (overSince < 0) overSince = now;
                    if (now - overSince >= HOLD_MS) {
                        int state = currentState();
                        phase = Phase.FIRED;
                        stillSince = -1;
                        listener.onPickup(state);
                    }
                } else {
                    overSince = -1;
                }
                break;

            case FIRED:
                // Test mode: re-arm only once it is lying still on the table again.
                if (rearmAfterFire && t < ARM_MAX_DEG && motion < STILL) {
                    if (stillSince < 0) stillSince = now;
                    if (now - stillSince > 600) {
                        faceDownSince = now;
                        phase = Phase.WAITING;
                        listener.onDisarmed();
                    }
                } else {
                    stillSince = -1;
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
