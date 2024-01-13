package com.example.music_player;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
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
    String[] items,urls;
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

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateSeekBar() {
        new Thread(() -> {
            while (isThreadRunning) {
                try {
                    synchronized (this) {
                        if (mediaPlayer != null && !isFinishing()) {
                            int currentposition = mediaPlayer.getCurrentPosition();

                            // Use runOnUiThread to update UI elements on the main thread
                            runOnUiThread(() -> {
                                if (mediaPlayer != null && !isFinishing()) {
                                    seekmusic.setProgress(currentposition);
                                    String currentTime = createTime(currentposition);
                                    txtsstart.setText(currentTime);
                                }
                            });
                        }
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    // Handle IllegalStateException if needed
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
        urls = getIntent().getStringArrayExtra("urls");
        position = getIntent().getIntExtra("pos", 0);
        Log.d("Debug", "Urls array size: " + urls.length);
        Log.d("Debug", "Items array size: " + items.length);
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
        mediaPlayer = new MediaPlayer();
        sname = getIntent().getStringExtra("songname");
        selectedSongRef = getIntent().getStringExtra("selectedSongRef");

        txtsname.setSelected(true);
        txtsname.setText(sname);
        prepareMediaPlayer(selectedSongRef);


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
                // Increment position to play the next song
                position++;
                if (position >= items.length) {
                    position = 0; // Wrap around to the first song if reached the end
                }

                playSelectedSong();
            }
        });

        btnprev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();
                mediaPlayer.release();
                // Decrement position to play the previous song
                position--;
                if (position < 0) {
                    position = items.length - 1; // Wrap around to the last song if reached the beginning
                }

                playSelectedSong();
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
            mediaPlayer.setDataSource(songUrl);
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
    }

    private void playSelectedSong() {


        Intent intent = new Intent(OnlinePlayerActivity.this, OnlinePlayerActivity.class);
        intent.putExtra("items", items);
        intent.putExtra("urls", urls);
        intent.putExtra("pos", position);
        intent.putExtra("songname", items[position]);
        intent.putExtra("selectedSongRef", urls[position]);

        // Stop the current activity and start a new one with the selected song
        finish();
        startActivity(intent);
    }

    public void startAnimation(View view)
    {
        ObjectAnimator animator = ObjectAnimator.ofFloat(imageView, "rotation", 0f,360f);
        animator.setDuration(1000);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animator);
        animatorSet.start();
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