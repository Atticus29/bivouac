package fisherdynamic.swarmreporter1.activities;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import fisherdynamic.swarmreporter1.R;
import fisherdynamic.swarmreporter1.models.SwarmReport;
import fisherdynamic.swarmreporter1.utilityClasses.Utilities;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;

public class NewSwarmReportActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private String TAG = NewSwarmReportActivity.class.getSimpleName();
    private FirebaseDatabase database;
    private DatabaseReference ref;
    private String size;
    private String accessibility;
    private String description;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authListener;
    private GoogleApiClient mGoogleApiClient;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationRequest mLocationRequest;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private String userName;
    private String userId;
    private Double currenLatitude;
    private Double currentLongitude;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private SwarmReport newSwarmReport = new SwarmReport();
    private DatabaseReference pushRef;
    private SharedPreferences mSharedPreferences;

    @Bind(R.id.reportSwarmButton) Button reportSwarmButton;
    @Bind(R.id.baseball) RadioButton baseball;
    @Bind(R.id.football) RadioButton football;
    @Bind(R.id.basketball) RadioButton basketball;
    @Bind(R.id.beachball) RadioButton beachball;
    @Bind(R.id.tallLadder) RadioButton tallLadder;
    @Bind(R.id.ladder) RadioButton ladder;
    @Bind(R.id.reach) RadioButton reach;
    @Bind(R.id.hasLadder) RadioButton hasLadder;
    @Bind(R.id.addImageButton) Button addImageButton;
    @Bind(R.id.progressBar) ProgressBar progressBar;
    @Bind(R.id.descriptionTextView) EditText descriptionTextView;
    @Bind(R.id.sizeLabel) TextView sizeLabel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_swarm_report);

        ButterKnife.bind(this);

        reportSwarmButton.setOnClickListener(this);
        addImageButton.setOnClickListener(this);

        sizeLabel.requestFocus();

        auth = FirebaseAuth.getInstance();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        userName = mSharedPreferences.getString("userName", null);
        userId = mSharedPreferences.getString("userId", null);
        Log.d("personal", "newSwarm userName is " + userName);
        Log.d("personal", "newSwarm userId is " + userId);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(30 * 1000)
                .setFastestInterval(1 * 1000);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            handleNewLocation(location);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (location == null) {
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                    } else {
                        handleNewLocation(location);
                    }
                }
            }
        }
    }

    private void handleNewLocation(Location location) {
        currenLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        Log.d("personal", "newSwarm lat is " + currenLatitude);
        Log.d("personal", "newSwarm lon is " + currentLongitude);
        if(userId != null && userName != null && currenLatitude != 0.0 && currentLongitude != 0.0) {
            progressBar.setVisibility(View.GONE);
            reportSwarmButton.setVisibility(View.VISIBLE);
            addImageButton.setVisibility(View.VISIBLE);
        } else {
            Log.d("newSwarm", "either location or user info is null!");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("newSwarm", "Location services suspended. Please reconnect");

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
            Log.d("newSwarm", "Location services failed with code " + connectionResult.getErrorCode());
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
    }

    @Override
    public void onClick(View v) {
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
        java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(now.getTime());
        String timeString = new SimpleDateFormat("MM/dd/yyyy h:mm a").format(currentTimestamp);
        newSwarmReport.setLatitude(currenLatitude);
        newSwarmReport.setLongitude(currentLongitude);
        newSwarmReport.setReporterName(userName);
        newSwarmReport.setReporterId(userId);
        newSwarmReport.setClaimed(false);
        newSwarmReport.setReportTimestamp(timeString);
        if (newSwarmReport.getImageString() == null) {
            newSwarmReport.setImageString("https://coxshoney.com/wp-content/uploads/bee_swarm_man.jpg");
        }

        size = getSize();
        if(size != null){
            newSwarmReport.setSize(size);
            accessibility = getAccessibility();
            if(accessibility != null){
                newSwarmReport.setAccessibility(accessibility);
                try{
                    description = getDescription();
                    newSwarmReport.setDescription(description);
                } catch(Exception e){
                    descriptionTextView.setError("Please add a detailed description");
//                    Toast.makeText(NewSwarmReportActivity.this, "Please add a detailed description about the location", Toast.LENGTH_SHORT).show();
                    Log.d("personal", "description is null");
                }
            } else{
                Toast.makeText(NewSwarmReportActivity.this, "Please select accessibility", Toast.LENGTH_SHORT).show();
            }
        } else{
            Toast.makeText(NewSwarmReportActivity.this, "Please select size", Toast.LENGTH_SHORT).show();
        }




        if (size != null && accessibility != null && description != null) {
            database = FirebaseDatabase.getInstance();
            ref = database.getReference("all_unclaimed");
            pushRef = ref.push();
            String pushId = pushRef.getKey();
            newSwarmReport.setReportId(pushId);
        }
        if (v == addImageButton) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(v.getContext().getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        } else if (v == reportSwarmButton) {
            if (newSwarmReport.getSize() != null && newSwarmReport.getAccessibility() != null && newSwarmReport.getDescription() != null && !newSwarmReport.getDescription().equals("")) {
                if (newSwarmReport.getReportId() != null) {
                    pushRef.setValue(newSwarmReport);
                    Utilities.establishSwarmReportInGeoFire(newSwarmReport);
                    Utilities.updateSwarmReportWithItsGeoFireCode(newSwarmReport, userId);
                }
                Intent intent = new Intent(NewSwarmReportActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == this.RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            encodeBitmapAndSaveToFirebase(imageBitmap);
        }
    }

    public void encodeBitmapAndSaveToFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        newSwarmReport.setImageString(imageEncoded);
    }

    public String getSize() {
        String size = null;
        if (baseball.isChecked()) {
            size = "baseball";
        } else if (football.isChecked()) {
            size = "football";
        } else if (basketball.isChecked()) {
            size = "basketball";
        } else if (beachball.isChecked()) {
            size = "beachball";
        }
        return size;
    }

    public String getDescription() throws Exception{
        String returnVal = null;
        returnVal = descriptionTextView.getText().toString().trim();
        if(returnVal == null || returnVal.equals("")){
            throw new Exception("Description is null");
        }
        return returnVal;
    }

    public String getAccessibility() {
        String accessibility = null;
        if (reach.isChecked()) {
            accessibility = "within reach";
        } else if (ladder.isChecked()) {
            accessibility = "requires ladder";

        } else if (hasLadder.isChecked()) {
            accessibility = "reporter has ladder";
        } else if (tallLadder.isChecked()) {
            accessibility = "requires tall ladder";
        }
        return accessibility;
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }
}
