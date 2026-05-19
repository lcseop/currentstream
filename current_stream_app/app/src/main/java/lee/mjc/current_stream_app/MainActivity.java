package lee.mjc.current_stream_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    Button logoutBtn, noTeamLogoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoutBtn = findViewById(R.id.main_team_logout);
        noTeamLogoutBtn = findViewById(R.id.main_no_team_logout);

        logoutBtn.setOnClickListener((v) -> logoutBtnClick());
        noTeamLogoutBtn.setOnClickListener((v) -> logoutBtnClick());

    }

    private void logoutBtnClick() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}