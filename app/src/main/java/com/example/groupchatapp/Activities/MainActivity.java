package com.example.groupchatapp.Activities;

import android.content.Intent;
import android.content.pm.PackageManager;

import android.location.Location;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.groupchatapp.Adapters.AllGroupsAdapter;
import com.example.groupchatapp.Models.Group;
import com.example.groupchatapp.LoginManager;
import com.example.groupchatapp.OnLoggedIn;
import com.example.groupchatapp.R;
import com.example.groupchatapp.Utils;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Toolbar mToolbar;

    private RecyclerView m_GroupList;
    private DatabaseReference m_GroupsRef;
    private AllGroupsAdapter m_GroupsAdapter;
    private final ArrayList<Group> groupsToDisplay = new ArrayList<>();

    private LoginManager m_LoginManager;

    private OnLoggedIn m_OnLoggedInListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("GroupChat");


        m_GroupList = findViewById(R.id.chats_list);
        m_GroupsAdapter = new AllGroupsAdapter(groupsToDisplay, this);
        m_GroupList.setLayoutManager(new LinearLayoutManager(this));

        m_GroupList.setAdapter(m_GroupsAdapter);

        m_LoginManager = LoginManager.getInstance();

        if (!m_LoginManager.IsLoggedIn()) {

            initLoggedInListener();
            m_LoginManager.Login(m_OnLoggedInListener);
        }

    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void SendUserToSettingsActivity() {
        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.main_logout_option) {
            m_LoginManager.Logout();
            SendUserToLoginActivity();
        } else if (item.getItemId() == R.id.main_settings_option) {
            SendUserToSettingsActivity();
        } else if (item.getItemId() == R.id.main_find_friends_option) {
            SendUserToFindFriendsActivity();
        } else if (item.getItemId() == R.id.main_Create_Group_option) {
            SendUserToCreateGroupActivity();

        } else if (item.getItemId() == R.id.map_option) {
            SendUserToMapsActivity();
        } else if (item.getItemId() == R.id.main_my_groups_option) {

            if (m_LoginManager.getLocationManager().isLocationOn()) {
                SendUserToMyGroupsActivity();
            } else {
                Toast.makeText(this, "Turn on Location!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void SendUserToMyGroupsActivity() {
        Intent myGroupsIntent = new Intent(MainActivity.this, MyGroupsActivity.class);
        startActivity(myGroupsIntent);
    }

    private void SendUserToCreateGroupActivity() {
        Intent createGroupIntent = new Intent(MainActivity.this, CreateGroupActivity.class);
        startActivity(createGroupIntent);
    }

    private void SendUserToFindFriendsActivity() {

        Intent findFriendsIntent = new Intent(MainActivity.this, FindFriendsActivity.class);
        startActivity(findFriendsIntent);
    }


    private void SendUserToMapsActivity()
    {

        Intent mapIntent = new Intent(MainActivity.this,MapsActivity.class);
        mapIntent.putExtra("groups",groupsToDisplay);
        startActivity(mapIntent);
    }


    private void initLoggedInListener() {
        m_OnLoggedInListener = new OnLoggedIn() {
            @Override
            public void onSuccess() {
                m_LoginManager.getLocationManager().CheckPermissionLocation(MainActivity.this);
            }

            @Override
            public void onStart() {

            }

            @Override
            public void onFailure() {

            }

        };
    }

    public void OnGroupRefProvide() {

        groupsToDisplay.clear();
        m_GroupsAdapter.notifyDataSetChanged();

        String countryCode = m_LoginManager.getLocationManager().getCountryCode();
        m_GroupsRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(countryCode);

        m_GroupsRef.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Group groupToAdd = dataSnapshot.getValue(Group.class);

                if (!m_LoginManager.getLoggedInUser().getValue().getGroupsId().containsKey(groupToAdd.getGid())) {
                    groupsToDisplay.add(dataSnapshot.getValue(Group.class));
                    m_GroupsAdapter.notifyDataSetChanged();
                }
            }


            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                Group changedGroup = dataSnapshot.getValue(Group.class);

                int indexToChange = Utils.findIndexOfGroup(groupsToDisplay, changedGroup);

                if (!m_LoginManager.getLoggedInUser().getValue().getGroupsId().containsKey(changedGroup.getGid())) {

                    if (indexToChange == -1) {
                        groupsToDisplay.add(changedGroup);
                    } else {
                        groupsToDisplay.set(indexToChange, changedGroup);
                    }
                } else {
                    if (indexToChange != -1) {
                        groupsToDisplay.remove(indexToChange);
                        }
                    }

                    m_GroupsAdapter.notifyDataSetChanged();
                }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Group groupToRemove = dataSnapshot.getValue(Group.class);

                int indexToRemove = Utils.findIndexOfGroup(groupsToDisplay, groupToRemove);
                if (indexToRemove != -1) {
                    groupsToDisplay.remove(indexToRemove);
                    m_GroupsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                m_LoginManager.getLocationManager().createLocationManagerAndListener(); //App can use location!
                m_LoginManager.getLocationManager().getCurrentLocation();
            } else {
                //Can't use the app message.
                //For Using the app you need to go to setting and enable location permissions to the app.
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

