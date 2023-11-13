package com.example.music_player;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;
    ListView listView;
    String[] items;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth=FirebaseAuth.getInstance();
        button=findViewById(R.id.logout);
        textView=findViewById(R.id.user_detail);
        user = auth.getCurrentUser();
        if(user == null){
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }
        else{
            textView.setText(user.getEmail());
        }
        listView=findViewById(R.id.listViewSong);

        runtimePermission();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

    }


    public void runtimePermission(){
        Dexter.withContext(this).withPermission(Manifest.permission.READ_MEDIA_AUDIO)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        displaySongs();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();

                    }
                }).check();
    }

    public ArrayList<File> findSong(File file) {

        ArrayList arrayList = new ArrayList();

        File [] files = file.listFiles();

        if(files != null) {

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


        void displaySongs(){
        final ArrayList<File> mySongs = findSong(Environment.getExternalStorageDirectory());

        items = new String[mySongs.size()];
        for (int i =0;i<mySongs.size();i++){
            items[i]=mySongs.get(i).getName().toString().replace(".mp3","").replace(".wav","");

        }
        /*ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,items);
        listView.setAdapter(myAdapter);*/
        customAdapter customAdapter = new customAdapter();
        listView.setAdapter(customAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String songName = (String) listView.getItemAtPosition(i);
                    startActivity(new Intent(getApplicationContext(), PlayerActivity.class)
                            .putExtra("songs", mySongs)
                            .putExtra("songname", songName)
                            .putExtra("pos", i));
                }
            });



    }

    class customAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View myView = getLayoutInflater().inflate(R.layout.list_item,null);
            TextView textsong = myView.findViewById(R.id.txtsongname);
            textsong.setSelected(true);
            textsong.setText(items[i]);

            return myView;
        }
    }

}