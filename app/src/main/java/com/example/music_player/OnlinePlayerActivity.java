package com.example.music_player;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;

public class OnlinePlayerActivity extends AppCompatActivity {

    Button btnplay, btnnext, btnprev, btnff, btnfr;
    TextView txtsname, txtsstart, txtsstop;
    SeekBar seekmusic;
    FirebaseStorage storage;
    StorageReference storageRef;
    ImageView imageView;
    private boolean isThreadRunning = true;
    String sname;
    MediaPlayer mediaPlayer;
    String selectedSongRef; // Updated to store StorageReference URL
    int position;
    String[] items;
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isThreadRunning = false; // Stop the updateSeekBar thread
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }


    private void updateSeekBar() {
        new Thread(() -> {
            while (isThreadRunning) {
                try {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int currentposition = mediaPlayer.getCurrentPosition();

                        // Use runOnUiThread to update UI elements on the main thread
                        runOnUiThread(() -> {
                            if (mediaPlayer != null) {
                                seekmusic.setProgress(currentposition);
                                String currentTime = createTime(currentposition);
                                txtsstart.setText(currentTime);
                            }
                        });
                    }
                    Thread.sleep(500);
                } catch (InterruptedException | IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getSupportActionBar().setTitle("Now Playing");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        items = getIntent().getStringArrayExtra("items");
        position = getIntent().getIntExtra("pos", 0);

        if (position < 0 || position >= items.length) {
            position = 0;
        }
        btnprev = findViewById(R.id.btnprev);
        btnnext = findViewById(R.id.btnnext);
        btnplay = findViewById(R.id.playbtn);
        btnff = findViewById(R.id.btnff);
        btnfr = findViewById(R.id.btnfr);
        txtsname = findViewById(R.id.txtsn);
        txtsstart = findViewById(R.id.txtsstart);
        txtsstop = findViewById(R.id.txtsstop);
        seekmusic = findViewById(R.id.seekbar);
        imageView = findViewById(R.id.imageview);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();


        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        sname = getIntent().getStringExtra("songname");
        selectedSongRef = getIntent().getStringExtra("selectedSongRef");

        txtsname.setSelected(true);
        txtsname.setText(sname);

        storageRef.child(selectedSongRef).getDownloadUrl().addOnSuccessListener(uri -> {
            String songUrl = uri.toString();
            Log.d("OnlinePlayerActivity", "HTTP URL: " + songUrl);
            prepareMediaPlayer(songUrl); // Move this line here
        }).addOnFailureListener(exception -> {
            // Handle any errors
            Log.e("OnlinePlayerActivity", "Failed to get download URL", exception);
        });

        seekmusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Handle start tracking touch
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Handle stop tracking touch
            }
        });

        seekmusic.getProgressDrawable().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        seekmusic.getThumb().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);


        // Use setDataSource to stream the audio directly without downloading
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(selectedSongRef);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                seekmusic.setMax(mp.getDuration());
                updateSeekBar();
                String endTime = createTime(mp.getDuration());
                txtsstop.setText(endTime);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("MediaPlayer", "Error: " + what + ", Extra: " + extra);
                return false;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }



        btnplay.setOnClickListener(view -> {
            if (mediaPlayer.isPlaying()) {
                btnplay.setBackgroundResource(R.drawable.ic_play);
                mediaPlayer.pause();
            } else {
                btnplay.setBackgroundResource(R.drawable.ic_pause);
                mediaPlayer.start();
                updateSeekBar(); // Start updating seek bar after resuming
            }
        });

        //next listener

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                btnplay.setBackgroundResource(R.drawable.ic_play);
                btnnext.performClick();
            }
        });

        int audiosessionId = mediaPlayer.getAudioSessionId();
        btnnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();
                mediaPlayer.release();
                position = getNextPosition(position);
                storageRef.child(getNextSongRef(position)).getDownloadUrl().addOnSuccessListener(uri -> {
                    String songUrl = uri.toString();
                    Log.d("OnlinePlayerActivity", "HTTP URL: " + songUrl);
                    prepareMediaPlayer(songUrl); // Prepare and play the next song
                }).addOnFailureListener(exception -> {
                    // Handle any errors
                    Log.e("OnlinePlayerActivity", "Failed to get download URL", exception);
                });
            }
        });

        btnprev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();
                mediaPlayer.release();
                position = getPreviousPosition(position);
                storageRef.child(getPreviousSongRef(position)).getDownloadUrl().addOnSuccessListener(uri -> {
                    String songUrl = uri.toString();
                    Log.d("OnlinePlayerActivity", "HTTP URL: " + songUrl);
                    prepareMediaPlayer(songUrl); // Prepare and play the previous song
                }).addOnFailureListener(exception -> {
                    // Handle any errors
                    Log.e("OnlinePlayerActivity", "Failed to get download URL", exception);
                });
            }
        });




        btnff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 10000);
                }
            }
        });

        btnfr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 10000);
                }
            }
        });
    }
    private void prepareMediaPlayer(String songUrl) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(songUrl);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                seekmusic.setMax(mp.getDuration());
                updateSeekBar(); // Call updateSeekBar after successful preparation

                String endTime = createTime(mp.getDuration());
                txtsstop.setText(endTime);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("MediaPlayer", "Error: " + what + ", Extra: " + extra);
                return false;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private int getNextPosition(int currentPosition) {
        // Implement your logic to get the next position
        // For example, incrementing the position and handling the end of the list
        return (currentPosition + 1) % items.length;
    }

    private int getPreviousPosition(int currentPosition) {
        // Implement your logic to get the previous position
        // For example, decrementing the position and handling the beginning of the list
        return (currentPosition - 1 + items.length) % items.length;
    }

    private String getNextSongRef(int currentPosition) {
        // Implement your logic to get the StorageReference for the next song
        // For example, getting the StorageReference URL based on the next position
        return items[getNextPosition(currentPosition)];
    }

    private String getPreviousSongRef(int currentPosition) {
        // Implement your logic to get the StorageReference for the previous song
        // For example, getting the StorageReference URL based on the previous position
        return items[getPreviousPosition(currentPosition)];
    }

    private String createTime(int duration) {
        String time = "";
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;

        time += min + ":";

        if (sec < 10) {
            time += "0";
        }
        time += sec;

        return time;
    }
}