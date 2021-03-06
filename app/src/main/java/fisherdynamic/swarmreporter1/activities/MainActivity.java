package fisherdynamic.swarmreporter1.activities;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import fisherdynamic.swarmreporter1.R;
import fisherdynamic.swarmreporter1.SecretConstants;
import fisherdynamic.swarmreporter1.models.SwarmNotification;
import fisherdynamic.swarmreporter1.models.SwarmReport;
import fisherdynamic.swarmreporter1.utilityClasses.Utilities;
import fisherdynamic.swarmreporter1.viewHolders.FirebaseClaimViewHolder;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationRequest mLocationRequest;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private Query swarmReportQuery;
    private FirebaseRecyclerAdapter mFirebaseAdapter;
    private Double currenLatitude;
    private Double currentLongitude;
    private String userName;
    private String userId;
    private String photoUrl;
    private String passedUserProfileURL;
    private FirebaseAuth auth;
    private View hView;
    private NavigationView navigationView;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private GeoFire geoFire;
    private GeoQuery geoQuery;
    private ArrayList<String> swarmReportIds = new ArrayList<>();
    private ArrayList<SwarmReport> swarmReports = new ArrayList<>();
    private String claimCheckKey;
    private FirebaseAuth.AuthStateListener authListener;

    @Bind(R.id.claimRecyclerView) RecyclerView claimRecyclerView;
    @Bind(R.id.greetingTextView) TextView greetingTextView;
    @Bind(R.id.progressBarForRecyclerView) ProgressBar progressBarForRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        userName = mSharedPreferences.getString("userName", null);
        userId = mSharedPreferences.getString("userId", null);
        if (userName != null && userId != null && !userName.equals("") && !userId.equals("")) {
            greetingTextView.setText("Unclaimed swarms near " + userName + ":");
            clearCurrentUserNode(userId);
        }

        setUpBlankAdapter(); //TODO check whether this is necessary

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        hView = navigationView.getHeaderView(0);

        navigationView.setNavigationItemSelectedListener(this);

        ImageView profileImageView = (ImageView) hView.findViewById(R.id.profileImageView);
        photoUrl = mSharedPreferences.getString("photoUrl", null);
        if(photoUrl != null && !photoUrl.equals("")){
            Log.d("personal", "photoUrl inside picasso is " + photoUrl);
            Picasso.with(this)
                    .load(photoUrl)
                    .resize(500,500)
                    .centerCrop()
                    .into(profileImageView);
        }

        TextView greetingNameTextView = (TextView) hView.findViewById(R.id.greetingNameTextView);
        if(userName != null){
            Log.d("personal", "got into changing userName in textView");
            greetingNameTextView.setText(userName);
        }

//        SwarmNotification swarmNotification = new SwarmNotification("New swarm", "New swarm", "A new swarm has been reported in your area", claimRecyclerView);

        auth = FirebaseAuth.getInstance();
        Log.d("personal", "is auth null? " + Boolean.toString(auth == null));

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = auth.getCurrentUser();
                for (UserInfo userInfo : user.getProviderData()) {
                    if (passedUserProfileURL == null && userInfo.getPhotoUrl() != null) {
                        passedUserProfileURL = userInfo.getPhotoUrl().toString();
                        ImageView profileImageView = (ImageView) hView.findViewById(R.id.profileImageView);
                        //TODO deal with either including user profile stuff or not
                        Picasso.with(hView.getContext())
                                .load(passedUserProfileURL)
                                .resize(SecretConstants.PIC_WIDTH, SecretConstants.PIC_HEIGHT)
                                .centerCrop()
                                .into(profileImageView);
                    }
                    if (userId == null && userInfo.getUid() != null) {
                        userId = userInfo.getUid();
                    }
                    if (userName == null && userInfo.getDisplayName() != null) {
                        userName = userInfo.getDisplayName();
                    }
                }
            }
        };

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1 * 1000);
    }

    public void clearCurrentUserNode(String userId){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(userId + "_current");
        ref.removeValue();
        Log.d("personal", "removed references in userName_current");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    private void logout() {
        FirebaseAuth.getInstance().signOut();
        addToSharedPreferences("userName", "");
        addToSharedPreferences("userId", "");
        addToSharedPreferences("photoUrl", "");
        Intent intent = new Intent(fisherdynamic.swarmreporter1.activities.MainActivity.this, LoginGateActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void addToSharedPreferences(String key, String passedUserName) {
        mEditor.putString(key, passedUserName).apply();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            logout();
            return true;
        }

        if (id == R.id.action_newReport) {
            Intent intent = new Intent(MainActivity.this, NewSwarmReportActivity.class);
            if (userName != null && userId != null) {
                intent.putExtra("userName", userName);
                intent.putExtra("userId", userId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Unable to retrieve username and id", Toast.LENGTH_SHORT).show();
            }

        }

        if (id == R.id.action_myClaims) {
            Intent intent = new Intent(MainActivity.this, MyClaimedSwarmsActivity.class);
            if (userName != null && userId != null) {
                intent.putExtra("userName", userName);
                intent.putExtra("userId", userId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Unable to retrieve username and id", Toast.LENGTH_SHORT).show();
            }
        }

        if (id == R.id.action_myReportedSwarms) {
            Intent intent = new Intent(MainActivity.this, MyReportedSwarmsActivity.class);
            if (userName != null && userId != null) {
                intent.putExtra("userName", userName);
                intent.putExtra("userId", userId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Unable to retrieve username and id", Toast.LENGTH_SHORT).show();
            }

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("personal", "got into onConnected");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            Log.d("personal", "location null");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Log.d("personal", "location not null");
            handleNewLocation(location);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d("personal", "got into permissionResults");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("personal", "permission was granted");
                    Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (location == null) {
                        Log.d("personal", "location is null inside permission");
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                    } else {
                        Log.d("personal", "about to call new location");
                        handleNewLocation(location);
                    }
                }
            }
        }
    }

    private void handleNewLocation(Location location) {
        currenLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        if (currenLatitude != null && currentLongitude != null) {
            setUpGeoFire();
        }
    }

    private void setUpGeoFire() {
        DatabaseReference geoFireRef = FirebaseDatabase.getInstance().getReference("geofire");
        geoFire = new GeoFire(geoFireRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(currenLatitude, currentLongitude), SecretConstants.QUERY_RADIUS);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.d("personal", "all of the onKeyEntered entered");
                Log.d("personal", String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
                claimCheckKey = key;
                swarmReportIds.add(claimCheckKey);
                clearCurrentUserNode(userId);
                Utilities.transferSwarmReportsFromAllToNewNode(userId + "_current", swarmReportIds);
                Log.d("personal", "all of the onKeyEntered stuff happened");
            }

            @Override
            public void onKeyExited(String key) {
                Log.d("personal", "onKeyExited entered");
                Log.d("personal", String.format("Key %s is no longer in the search area", key));
                Utilities.removeItemFromArrayList(key, swarmReportIds);
                clearCurrentUserNode(userId);
                Utilities.transferSwarmReportsFromAllToNewNode(userId + "_current", swarmReportIds);
                Log.d("personal", "onKeyExited stuff all happened");
            }


            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                clearCurrentUserNode(userId);
                swarmReportIds = Utilities.removeItemFromArrayList(key, swarmReportIds);
                Utilities.transferSwarmReportsFromAllToNewNode(userId + "_current", swarmReportIds);
            }

            @Override
            public void onGeoQueryReady() {
                Log.d("personal", "All initial data has been loaded and events have been fired!");
                Utilities.transferSwarmReportsFromAllToNewNode(userId + "_current", swarmReportIds);
                ArrayList<String> children = new ArrayList<>();
                children.add(userId + "_current");
                setUpFirebaseAdapter(children);
                //TODO check for asynchronicity issues when the swarm count is very high
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                System.err.println("There was an error with this query: " + error);
            }
        });
    }

    private void setUpFirebaseAdapter(final ArrayList<String> children) {
        Log.d("personal", "first child is " + children.get(0));

        DatabaseReference keyRef = FirebaseDatabase.getInstance().getReference(children.get(0)); //TODO edit here

        for (int i = 1; i < children.size(); i++) {
            keyRef = keyRef.getRef().child(children.get(i));
        }

        mFirebaseAdapter = new FirebaseRecyclerAdapter<SwarmReport, FirebaseClaimViewHolder>
                (SwarmReport.class, R.layout.claim_item, FirebaseClaimViewHolder.class,
                        keyRef) {

            @Override
            protected void populateViewHolder(final FirebaseClaimViewHolder viewHolder,
                                              SwarmReport model, int position) {
                viewHolder.bindClaimerLatLong(currenLatitude, currentLongitude);
                viewHolder.bindCurrentUserNameAndId(userName, userId);
                viewHolder.bindSwarmReport(model);
            }
        };
        setUpBlankAdapter(); //TODO check whether necessary
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(claimRecyclerView.getContext(),
                new LinearLayoutManager(MainActivity.this).getOrientation());
        dividerItemDecoration.setDrawable(getDrawable(R.drawable.recycler_view_divider));
        claimRecyclerView.addItemDecoration(dividerItemDecoration);
        progressBarForRecyclerView.setVisibility(View.GONE);
    }

    private void setUpBlankAdapter() {
        claimRecyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        claimRecyclerView.setLayoutManager(linearLayoutManager);
        claimRecyclerView.setAdapter(mFirebaseAdapter);
        claimRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.cleanup();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("personal", "Location services suspended. Please reconnect");
        Toast.makeText(MainActivity.this, "Location services suspended. Please reconnect", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "Location services failed with code " + connectionResult.getErrorCode(), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        this.geoQuery.removeAllListeners();
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }
}
