package palsofpaulos.soundscape.common;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class Recording {

    private static final String TAG = "Recording Object";
    private static int lastId = 0;

    private String filePath;
    private File file;

    private int id;
    private String name;
    private byte[] data;

    private PlayAudioTask playTask = null;

    public Recording(String filePath) {
        this.filePath = filePath;
        this.file = new File(filePath);
        this.id = lastId++;
    }

    public Recording(InputStream inputStream, String pathName) {

        this.id = lastId++;
        this.filePath = pathName + id + ".pcm";
        this.file = new File(filePath);

        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            Log.d(TAG, "Done writing recording to PCM file!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* Getters and Setters */
    public String getFilePath() { return filePath; }

    public File getFile() { return file; }

    public int getId() { return id; }

    public PlayAudioTask getPlayTask() { return playTask; }

    // returns the length of the recording in seconds
    public long length() {
        return (file.length() / RecordingManager.SAMPLERATE);
    }

    public String lengthString() {
        long len = this.length();
        return String.format("%d:%d", len / 60, len % 60);
    }






    /* Playback Methods and Classes */

    public void play() {
        playTask = new PlayAudioTask();
        playTask.execute();
    }


    /* Play the recording and specify a listener with an
     * onFinished() callback method to call when the
     * recording has finished playing
     */
    public void play(PostPlayListener listener) {
        playTask = new PlayAudioTask(listener);
        playTask.execute();
    }

    public boolean isPlaying() {
        if (playTask != null) {
            return playTask.getStatus() == AsyncTask.Status.RUNNING;
        }
        return false;
    }

    public void pause() {
        if (playTask != null) {
            playTask.pause();
        }
    }

    public void stop() {
        if (playTask != null) {
            playTask.stop();
            playTask = null;
        }
    }


    /* Executing a PlayAudioTask will play the recording. If the
     * Task is instantiated with a postPlayListener then you can
     * specify a callback for when the recording has finished playing.
     */

    public class PlayAudioTask extends AsyncTask<Void, Void, Void> {

        private final PostPlayListener postPlayListener;

        private AudioTrack track;

        public PlayAudioTask() {
            this.postPlayListener = null;
        }

        public PlayAudioTask(PostPlayListener listener) {
            // The listener reference is passed in through the constructor
            this.postPlayListener = listener;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "Attempting to play audio at path " + filePath);
            // We keep temporarily filePath globally as we have only two sample sounds now..
            if (file == null) {
                if (filePath == null) {
                    Log.e(TAG, "Filepath error!");
                    return null;
                }
                file = new File(filePath);
            }

            // Reading the file..
            byte[] byteData = new byte[(int) file.length()];

            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                in.read(byteData);
                in.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Set and push to audio track..
            int intSize = android.media.AudioTrack.getMinBufferSize(RecordingManager.SAMPLERATE, RecordingManager.CHANNELS_OUT, RecordingManager.ENCODING);

            int bdlen = byteData.length;
            if (bdlen == 0) {
                return null;
            }
            track = new AudioTrack(AudioManager.STREAM_MUSIC, RecordingManager.SAMPLERATE, RecordingManager.CHANNELS_OUT, RecordingManager.ENCODING, byteData.length, AudioTrack.MODE_STATIC);
            if (track != null) {

                // Write the byte array to the track
                track.write(byteData, 0, byteData.length);
                track.play();
                track.setNotificationMarkerPosition(byteData.length/2);
                track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onPeriodicNotification(AudioTrack track) { }
                    @Override
                    public void onMarkerReached(AudioTrack track) {
                        Log.d(TAG, "stopping audio");
                        track.stop();
                        track.release();
                    }
                });
            }

            return null;
        }

        @Override
        protected void onCancelled(Void result) {
            if (track != null) {
                track.stop();
                track.release();
                playTask = null;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            playTask = null;
            if (postPlayListener != null) {
                postPlayListener.onFinished();
            }
        }

        public void pause() {
            Log.d(TAG, "Pausing Recording");
            if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
                track.pause();
            }
        }

        public void play() {
            Log.d(TAG, "Resuming recording");
            if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
                track.play();
            }
        }

        public void stop() {
            if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
                track.stop();
                track.release();
            }
        }
    }

    public interface PostPlayListener {
        void onFinished();
    }

}
