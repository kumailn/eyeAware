package com.kumailn.yhack;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import mehdi.sakout.aboutpage.AboutPage;

public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setImage(R.drawable.about_icon_email)
                .addGroup("Connect with us")
                .addEmail("elmehdi.sakout@gmail.com")
                .addWebsite("http://medyo.github.io/")
                .addFacebook("the.medy")
                .addTwitter("medyo80")
                .addPlayStore("com.ideashower.readitlater.pro")
                .addGitHub("medyo")
                .setDescription("This is an app that is meant to help the visually impaired.")
                .create();
        super.onCreate(savedInstanceState);
        setContentView(aboutPage);
    }





}
