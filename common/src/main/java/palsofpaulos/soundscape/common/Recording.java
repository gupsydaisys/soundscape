package palsofpaulos.soundscape.common;

import android.location.Location;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Date;


public class Recording {

    private static final String TAG = "Recording Object";
    private static final int NOTIFICATION_PERIOD = 500;

    private String filePath;
    private File file;

    private int id;
    private String name;
    private Date dateCreated;
    private Location location;

    private PlayAudioTask playTask;
    private boolean isPlaying;
    private int oldProgress = 0;
    private int rating = 5;

    private boolean isDeleted = false;
    private boolean isStopped = false;

    // Constructor used to rebuild a stored recording
    public Recording(String filePath, Location location, Date date) {
        this.location = location;
        this.dateCreated = date;
        this.filePath = filePath;
        this.file = new File(filePath);
        try {
            int idloc = filePath.indexOf("_id") + 3;
            if (idloc == -1) {
                throw new IOException("Path does not contain a valid recording: " + filePath);
            }
            this.id = Integer.valueOf(filePath.substring(idloc, filePath.length() - 4));
        }
        catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        // truncate file for huge recordings
        truncateFile();
    }

    // Constructor used to create a new recording from a data inputStream
    public Recording(InputStream inputStream, String pathName, Location location, Date date) {

        this.id = RecordingManager.lastId++;
        this.location = location;
        this.dateCreated = Calendar.getInstance().getTime();
        this.filePath = pathName + "_id" + id + ".pcm";
        this.file = new File(filePath);
        this.location = (location != null ? location : RecordingManager.DEFAULT_LOCATION);

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
        // truncate file for huge recordings
        truncateFile();
    }

    /* Getters and Setters */
    public String getFilePath() { return this.filePath; }

    public File getFile() { return this.file; }

    public int getId() { return this.id; }

    public String getName() { return this.name; }

    public int getRating() { return this.rating; }

    public boolean isPlaying() { return this.isPlaying; }

    public boolean isDeleted() { return this.isDeleted; }

    public PlayAudioTask getPlayTask() { return this.playTask; }

    public Location getLocation() { return this.location; }

    public Date getDate() { return this.dateCreated; }

    public String getDateString() {
        return RecordingManager.PRINT_DATE_FORMAT.format(this.dateCreated);
    }

    public String getDateStorageString() {
        return RecordingManager.STORE_DATE_FORMAT.format(this.dateCreated);
    }


    public int currentPlayTime() {
        return (int) ((oldProgress + playTask.getPlayHead()) / RecordingManager.SAMPLERATE);
    }

    // returns the length of the recording in seconds
    public int length() {
        return (int) (file.length() / 2 / RecordingManager.SAMPLERATE);
    }

    public int frameLength() {
        return (int) (file.length() / 2);
    }

    public String lengthString() {
        int len = this.length();
        int hrs = len / 3600;
        int min = (len % 3600) / 60;
        int sec = len % 60;
        if (hrs > 0) {
            return String.format("%d:%d:%02d", hrs, min, sec);
        }
        return String.format("%d:%02d", min, sec);
    }
    // limits the file to MAX_LENGTH
    private void truncateFile() {
        if (file.length() > RecordingManager.MAX_LENGTH) {
            try {
                new RandomAccessFile(filePath, "rwd").setLength(RecordingManager.MAX_LENGTH);
            }
            catch (Exception e) {
                Log.e(TAG, "Failed trying to truncate file");
            }
        }
    }

    public static void setLastId(int lastId) {
        RecordingManager.lastId = lastId;
    }

    public void setName(String name) { this.name = name; }

    public void setRating(int rating) { this.rating = rating;}

    public void setPlayHead(int progress) {
        if (playTask != null && playTask.track != null) {
            playTask.setPlayHead(progress);
        }
    }


    /* Playback Methods
     * --------------------------------------- */

    /* starts recording if it hasn't started
     * resumes playback if it was was paused */
    public void play() {
        if (isDeleted) {
            Log.e(TAG, "attempted to play deleted recording!");
        }
        if (playTask != null) {
            playTask.play();
        }
        else {
            playTask = new PlayAudioTask();
            playTask.execute();
        }
    }


    /* Play the recording and specify a listener with an
     * onFinished() callback method to call when the
     * recording has finished playing */
    public void play(PlayListener listener) {
        if (isDeleted) {
            Log.e(TAG, "attempted to play deleted recording!");
        }
        if (playTask != null) {
            playTask.play();
        }
        else {
            oldProgress = 0;
            playTask = new PlayAudioTask(listener);
            playTask.execute();
        }
    }

    public void pause() {
        if (playTask != null) {
            playTask.pause();
        }
    }

    public void stop() {
        if (playTask != null) {
            playTask.stop();
            oldProgress = 0;
        }
    }

    public void delete() {
        isDeleted = true;
        stop();
        file.delete();
    }


    /**
     * An asynchronous task to handle playing the recording.
     *
     * Executing a PlayAudioTask will play the recording. If the
     * Task is instantiated with a postPlayListener then you can
     * specify a callback for when the recording has finished playing.
     */

    private class PlayAudioTask extends AsyncTask<Void, Void, Void> {

        private final PlayListener playListener;

        private AudioTrack track;

        public PlayAudioTask() {
            this.playListener = new PlayListener() {
                @Override
                public void onUpdate(int progress) {
                    // do nothing
                }
                @Override
                public void onFinished() {
                    // do nothing
                }
            };
        }

        public PlayAudioTask(PlayListener listener) {
            // The listener reference is passed in through the constructor
            this.playListener = listener;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "Attempting to play audio at path " + filePath);
            isPlaying = true;

            if (file.length() == 0) {
                cancel(true);
            }

            // Read the file
            int byteLength = (int) file.length();
            byte[] byteData = new byte[byteLength];
            FileInputStream in;
            try {
                in = new FileInputStream(file);
                in.read(byteData);
                in.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            track = new AudioTrack(AudioManager.STREAM_MUSIC, RecordingManager.SAMPLERATE, RecordingManager.CHANNELS_OUT, RecordingManager.ENCODING, byteLength, AudioTrack.MODE_STATIC);

            // Write the byte array to the track
            track.write(byteData, 0, byteLength);
            track.setNotificationMarkerPosition(byteLength / 2);
            track.setPositionNotificationPeriod(NOTIFICATION_PERIOD);
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onPeriodicNotification(AudioTrack track) {
                    if (track.getState() == AudioTrack.STATE_INITIALIZED) {
                        playListener.onUpdate(oldProgress + track.getPlaybackHeadPosition());
                    }
                }

                @Override
                public void onMarkerReached(AudioTrack track) {
                    Log.d(TAG, "stopping audio");
                    track.stop();
                    track.release();
                    isPlaying = false;
                    playTask = null;
                    playListener.onFinished();
                }
            });
            track.play();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... progress) {
        }

        @Override
        protected void onCancelled(Void result) {
            if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
                track.stop();
                track.release();
            }
            isPlaying = false;
            playTask = null;
            playListener.onFinished();
        }


        // Called once audio playback has been initialized
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        private void pause() {
            Log.d(TAG, "Pausing Recording");
            if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
                isPlaying = false;
                track.pause();
            }
        }

        private void play() {
            Log.d(TAG, "Resuming recording");
            if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
                isPlaying = true;
                track.play();
            }
        }

        private void stop() {
            Log.d(TAG, "Stopping recording");
            if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
                track.stop();
                track.release();
                track = null;
            }
            isPlaying = false;
            playTask = null;
            playListener.onFinished();
        }

        private int getPlayHead() {
            if (track == null) {
                return 0;
            }
            return track.getPlaybackHeadPosition();
        }

        private void setPlayHead(int position) {
            if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
                oldProgress = position;
                if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop();
                    track.reloadStaticData();
                    track.setPlaybackHeadPosition(position);
                    track.setNotificationMarkerPosition((int) file.length() / 2 - position);
                    track.play();
                }
                else {
                    track.stop();
                    track.reloadStaticData();
                    track.setPlaybackHeadPosition(position);
                    track.setNotificationMarkerPosition((int) file.length() / 2 - position);
                }
            }
            else {
                Log.e(TAG, "Tried to move playhead while track was not ready!");
            }
        }
    }

    public interface PlayListener {

        void onUpdate(final int progress);
        void onFinished();
    }

}
