package com.example.music_player;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;

public class OnlineActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button button, button_offline;
    TextView textView;
    FirebaseUser user;
    ListView listView;
    String[] items,urls;
    FirebaseStorage storage;
    StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_player);

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        auth = FirebaseAuth.getInstance();
        button = findViewById(R.id.logout);
        button_offline = findViewById(R.id.offline);
        textView = findViewById(R.id.user_detail);
        user = auth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            textView.setText(user.getEmail());
        }

        listView = findViewById(R.id.listViewSong_online);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        button_offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        displaySongs();
    }

    public ArrayList<File> findSong(File file) {
        ArrayList arrayList = new ArrayList();

        File[] files = file.listFiles();

        if (files != null) {
            for (File singlefile : files) {
                if (singlefile.isDirectory() && !singlefile.isHidden()) {
                    arrayList.addAll(findSong(singlefile));
                } else {
                    if (singlefile.getName().endsWith(".wav")) {
                        arrayList.add(singlefile);
                    } else if (singlefile.getName().endsWith(".mp3")) {
                        arrayList.add(singlefile);
                    }
                }
            }
        }
        return arrayList;
    }

    void displaySongs() {
        // Get a reference to the "music" folder in Firebase Storage
        StorageReference musicRef = storageRef.child("music");

        // Listens for changes to the items in the "music" folder
        musicRef.listAll().addOnSuccessListener(listResult -> {
            ArrayList<String> songNames = new ArrayList<>();
            ArrayList<StorageReference> songRefs = new ArrayList<>();
            ArrayList<String> httpsSongRefs = new ArrayList<>(); // Array to store HTTPS references

            for (StorageReference item : listResult.getItems()) {
                // You may add further checks if needed
                if (item.getName().endsWith(".mp3") || item.getName().endsWith(".wav")) {
                    songRefs.add(item);
                    songNames.add(item.getName().replace(".mp3", "").replace(".wav", ""));

                    // Get the download URL for the selected song
                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        String httpsSongRef = uri.toString();
                        httpsSongRefs.add(httpsSongRef); // Add HTTPS reference to the array

                        // Check if this is the last item being processed
                        if (httpsSongRefs.size() == songRefs.size()) {
                            // All URLs have been fetched, now update the UI
                            updateUI(songRefs, songNames, httpsSongRefs);
                        }
                    }).addOnFailureListener(e -> {
                        // Handle failure to get download URL
                        Log.e("OnlineActivity", "Failed to get download URL: " + e.getMessage());
                    });
                }
            }
        });
    }

    private void updateUI(ArrayList<StorageReference> songRefs, ArrayList<String> songNames, ArrayList<String> httpsSongRefs) {
        items = songNames.toArray(new String[0]);
        urls = httpsSongRefs.toArray(new String[0]);
        customAdapter customAdapter = new customAdapter(songRefs);
        listView.setAdapter(customAdapter);
        customAdapter.notifyDataSetChanged();
        Log.d("Debug", "Urls array size: " + urls.length);
        Log.d("Debug", "Items array size: " + items.length);
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            StorageReference selectedSongRef = songRefs.get(i);
            String songName = songNames.get(i);
            String httpsSongRef = httpsSongRefs.get(i); // Get the HTTPS reference

            // Start OnlinePlayerActivity with HTTPS reference, songName, and items array
            Intent playerIntent = new Intent(getApplicationContext(), OnlinePlayerActivity.class);
            playerIntent.putExtra("selectedSongRef", httpsSongRef);
            playerIntent.putExtra("songname", songName);
            playerIntent.putExtra("pos", i);
            playerIntent.putExtra("items", items); // Pass the items array
            playerIntent.putExtra("urls", urls); // Pass the HTTPS references array
            startActivity(playerIntent);
        });
    }




    class customAdapter extends BaseAdapter {
        ArrayList<StorageReference> songRefs;

        customAdapter(ArrayList<StorageReference> songRefs) {
            this.songRefs = songRefs;
        }

        @Override
        public int getCount() {
            Log.d("customAdapter", "getCount: " + songRefs.size());
            return songRefs.size();
        }

        @Override
        public Object getItem(int position) {
            Log.d("customAdapter", "getItem: " + position);
            return songRefs.get(position);
        }

        @Override
        public long getItemId(int position) {
            Log.d("customAdapter", "getItemId: " + position);
            return position;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View myView = getLayoutInflater().inflate(R.layout.list_item, null);
            TextView textsong = myView.findViewById(R.id.txtsongname);
            textsong.setSelected(true);

            // Get the actual StorageReference and extract the file name
            StorageReference songRef = songRefs.get(i);
            String songName = songRef.getName().replace(".mp3", "").replace(".wav", "");
            Log.d("customAdapter", "getView: " + songName);
            textsong.setText(songName);

            return myView;
        }
    }
}
