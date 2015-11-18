package palsofpaulos.soundscape.common;

import android.media.AudioFormat;
import android.media.MediaRecorder;

public class RecordingManager {
    public static final int SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int SAMPLERATE = 20000;
    public static final int CHANNELS_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int MAX_LENGTH = 20 * (60 * SAMPLERATE); // max 20 minutes

    private RecordingManager() {
    }
}
