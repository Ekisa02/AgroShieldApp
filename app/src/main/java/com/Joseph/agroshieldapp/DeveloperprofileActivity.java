package com.Joseph.agroshieldapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DeveloperprofileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developerprofile);

        // Set up toolbar
        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
       // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setTitle("Developer Profile");

        // Set all text content programmatically
        TextView tvDeveloperName = findViewById(R.id.tv_developer_name);
        TextView tvInstitution = findViewById(R.id.tv_institution);
        TextView tvProgram = findViewById(R.id.tv_program);
        TextView tvEducation = findViewById(R.id.tv_education);
        TextView tvExperience = findViewById(R.id.tv_experience);
        TextView tvAchievements = findViewById(R.id.tv_achievements);

        // Set text content
        tvDeveloperName.setText("Ekisa Joseph Opurong'o");
        tvInstitution.setText("University of Eldoret, Kenya");
        tvProgram.setText("Bachelor of Science in Information and Communication Technology (ICT)");

        // Education section
        String educationText = "Pursuing a Bachelor's degree in ICT at the University of Eldoret.\n\n" +
                "• Specialized in software development, mobile applications, and AI integration\n" +
                "• Strong foundation in computer programming (Java, JavaScript, Python, React Native, and Android development)";
        tvEducation.setText(educationText);

        // Experience section
        String experienceText = "• Mobile App Development: Experienced in designing and building scalable mobile applications with a focus on agriculture, environment, and social impact.\n\n" +
                "• AI & Machine Learning: Worked on computer vision models for plant disease detection and prediction systems.\n\n" +
                "• Firebase & Cloud Services: Skilled in implementing authentication, storage, and real-time databases for mobile applications.\n\n" +
                "• Collaborative Projects: Co-founded Innovatech, a student-led tech company focused on building digital solutions for African challenges.";
        tvExperience.setText(experienceText);

        // Achievements section
        String achievementsText = "• Prototype Developer of AgroShield, a mobile app for smart farming that integrates AI, weather intelligence, and a digital marketplace.\n\n" +
                "• National WorldSkills Competition Participant (2024) representing in software solutions for business.\n\n" +
                "• Successfully developed applications addressing environmental justice, tree carbon sequestration, and student skill exchange (SkillSwap).\n\n" +
                "• Recognized for innovation in climate-tech and AgriTech solutions targeting smallholder farmers.\n\n" +
                "• Active contributor in university tech communities and hackathons.";
        tvAchievements.setText(achievementsText);

        // Set up social media buttons
        ImageView btnLinkedIn = findViewById(R.id.btn_linkedin);
        ImageView btnGithub = findViewById(R.id.btn_github);
        ImageView btnEmail = findViewById(R.id.btn_email);

        btnLinkedIn.setOnClickListener(v -> {
            // Open LinkedIn profile
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/ekisa-joseph-opurongo"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "LinkedIn app not installed", Toast.LENGTH_SHORT).show();
            }
        });

        btnGithub.setOnClickListener(v -> {
            // Open GitHub profile
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ekisajoseph"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Browser not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnEmail.setOnClickListener(v -> {
            // Send email
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:ekisa.joseph@example.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about AgroShield App");
            try {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } catch (Exception e) {
                Toast.makeText(this, "Email app not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}