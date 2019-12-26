package ca.acsea.funstop;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class Login extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    public static final String NODE_USERS = "users";
    private static String TAG = "LogIn";
    private static final int RC_SIGN_IN = 9001;
    private EditText emailInput;
    private EditText passwordInput;
    private Button submitBtn;
    private SignInButton googleSignInBtn;
    private GoogleApiClient googleApiClient;
    private GoogleSignInClient mGoogleSignInClient;
    private String email;
    private String password;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    Semaphore semaphore = new Semaphore(0);

    private User mUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //set up google sign in options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        mAuth = FirebaseAuth.getInstance();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mDatabase = FirebaseDatabase.getInstance();
        mDatabase.getReference().child("users-testing").setValue("Loggin ");

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        submitBtn = findViewById(R.id.submit);

        googleSignInBtn = findViewById(R.id.google_sign_in_button);
        googleSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });


        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = emailInput.getText().toString();
                password = passwordInput.getText().toString();

                if(TextUtils.isEmpty(email)) {
                    emailInput.setError("Please enter your email");
                    return;
                } else if(TextUtils.isEmpty(password)){
                    passwordInput.setError("Please enter your password");
                    return;
                }
                createUser(email,password);
            }
        });

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void googleSignIn(){
        //Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);

        System.out.println("\n googleLogin \n");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        System.out.println("\n OnActivityResult\n");

        if (requestCode == RC_SIGN_IN){
            System.out.println("\n requestCode arrived\n");
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            System.out.println("\n 구글 사인인 결과: \n" + result + "/n");
            System.out.println("\n 구글 사인인 결과(boolben): \n" + result.isSuccess() + "/n");
            System.out.println(result.getStatus());

            if(result.isSuccess()){
                System.out.println("\n result succeeds\n");
                GoogleSignInAccount account = result.getSignInAccount();
                resultLogin(account);
            }
        }
    }

    private void resultLogin(final GoogleSignInAccount account) {
        System.out.println("\n resultLogin \n");
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    System.out.println("\n resultLogin : success\n");
                    Toast.makeText(Login.this, "google login succeeds", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(getApplicationContext(), Location.class);
                    intent.putExtra("user", mUser);
                    startActivity(intent);
                } else {
                    Toast.makeText(Login.this, "google login fails", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    public void createUser(final String email, final String password){
        // create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                System.out.println("createUser method starts");
                if(task.isSuccessful()){
                    final DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                    final FirebaseUser fUser = mAuth.getCurrentUser();

                    System.out.println("create user / task is successful");
                    new AlertDialog.Builder(Login.this).setTitle("Create New Account")

                            .setMessage("There is no such account. Do you want to create new account with the input id and password?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // OK
                                    mUser = new User(email, fUser.getUid());
                                    ref.child("user-test").child(fUser.getUid()).setValue(mUser);

                                    Toast.makeText(Login.this, "New account is created.", Toast.LENGTH_SHORT).show();
                                    //InitValues(); // init values
                                    signIn(email, password);
                                    //finish();

                                }})
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Cancel
                                    signIn(email,password);
                                    Toast.makeText(Login.this, "It is canceled.", Toast.LENGTH_SHORT).show();
                                }})
                            .show();

                }else{
                    mUser = new User(email, null);
                    System.out.println("create user / task is failed");
                    signIn(email, password);
                    //Toast.makeText(Login.this, "CreateUser method is failed", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public interface MyCallback {
        void onCallback(User value);
    }

    public void signIn(String email, String password){
        //sign in
        System.out.println("\n start sign in \n");
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    mUser.setUid(mAuth.getCurrentUser().getUid());
                    //TODO: May need to make it synchronous


                    // Retrieves the data from DB and moves to the next activity
                    getDataFromFirebase(new MyCallback() {
                        @Override
                        public void onCallback(User value) {
                            Log.d(TAG, "Value is: " + value);
                        }
                    });


                }else{
                    Log.println(Log.VERBOSE,"Login Error", task.getException().getMessage());
                    Toast.makeText(Login.this, task.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }



    private void getDataFromFirebase(final MyCallback callback){
        // Read from the database
        mDatabase.getReference().child("user-test").child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                mUser = dataSnapshot.getValue(User.class);

                Intent submit_intent = new Intent(Login.this, Location.class);
                submit_intent.putExtra("user", mUser);
                startActivity(submit_intent);

                callback.onCallback(mUser);
                Log.d(TAG, "Value is: " + mUser);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void InitValues() {
        System.out.println("initing values");
        DatabaseReference dbUsers = FirebaseDatabase.getInstance().getReference(NODE_USERS);
        String currentUser = mAuth.getCurrentUser().getUid();
        dbUsers.child(currentUser).child("email").setValue(mAuth.getCurrentUser().getEmail());
        dbUsers.child(currentUser).child("point").setValue(0); //initialize point
        dbUsers.child(currentUser).child("quiz").child("cutoff").setValue(0);
        dbUsers.child(currentUser).child("QR").child("korean").setValue(false);
        dbUsers.child(currentUser).child("QR").child("chinese").setValue(false);
        dbUsers.child(currentUser).child("QR").child("ladyHao").setValue(false);
        dbUsers.child(currentUser).child("QR").child("loneWolf1").setValue(false);
        dbUsers.child(currentUser).child("QR").child("loneWolf2").setValue(false);
        dbUsers.child(currentUser).child("QR").child("protector1").setValue(false);
        dbUsers.child(currentUser).child("QR").child("protector2").setValue(false);
        dbUsers.child(currentUser).child("QR").child("redFawn1").setValue(false);
        dbUsers.child(currentUser).child("QR").child("redFawn2").setValue(false);
        dbUsers.child(currentUser).child("QR").child("salishSea1").setValue(false);
        dbUsers.child(currentUser).child("QR").child("salishSea2").setValue(false);
        dbUsers.child(currentUser).child("QR").child("taiwanese").setValue(false);
        dbUsers.child(currentUser).child("QR").child("vietnamese").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station1").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station2").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station3").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station4").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station5").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station6").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station7").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station8").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station9").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station10").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station11").setValue(false);
        dbUsers.child(currentUser).child("QR").child("station12").setValue(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        mUser = new User(currentUser.getEmail(), currentUser.getUid());

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if(account != null || currentUser!= null){
           // Intent intent = new Intent(Login.this, MainActivity.class);
           // startActivity(intent);

            getData(new MyCallback() {
                @Override
                public void onCallback(User value) {
                    Log.d(TAG, "Value is: " + value);
                }
            });


        }else{
            Toast.makeText(Login.this, "Please Sign In", Toast.LENGTH_LONG).show();
        }
    }

    private void getData(final MyCallback callback){
        // Read from the database
        mDatabase.getReference().child("user-test").child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                mUser = dataSnapshot.getValue(User.class);

                // move to next activity
                Intent i = new Intent(Login.this, Location.class);
                i.putExtra("user", mUser);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);

                callback.onCallback(mUser);
                Log.d(TAG, "Value is: " + mUser);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

}