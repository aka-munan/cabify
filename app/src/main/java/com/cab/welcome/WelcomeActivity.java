package com.cab.welcome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.auth.LoginActivity;
import com.cab.app.MainActivity;
import com.cab.app.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class WelcomeActivity extends FragmentActivity {

    private static final int NUM_PAGES = 3;
    private ViewPager2 fragPager;
    private String[] anims, titles, descriptions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_main);

        initData();

        fragPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        Button iconButton = findViewById(R.id.iconButton);
        fragPager.setAdapter(createStateAdapter());
        new TabLayoutMediator(tabLayout, fragPager, (tab, position) -> {

        }).attach();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                iconButton.setText(position==2? "Continue":"");
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        iconButton.setOnClickListener(view ->{
            if (tabLayout.getSelectedTabPosition()==2){
                //go to login page
                Intent intent = new Intent();
                intent.setClass(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
                SharedPreferences preferences =  getApplication().getSharedPreferences("user",MODE_PRIVATE);
                preferences.edit().putBoolean("isNewUser",false).commit();
                finish();
            }
            fragPager.setCurrentItem(tabLayout.getSelectedTabPosition()+1);
        });
    }

    private void initData() {
        anims = new String[]{"https://lottie.host/a24176c9-0664-4c25-bdd8-0c7b5e1abfe9/WO4dJZJOqy.json",
                "https://lottie.host/a9d4d606-4838-415f-8dc8-d9a0355a23f9/YW78xl0dRa.json",
                "https://lottie.host/9d30be4c-fb7b-4cea-9a78-12976fdb14ba/LP7Go67ARn.json"};
        titles = new String[]{"Welcome to "+getString(R.string.app_name)+" \nYour Ride, Your Way!",
                "Track your Ride in real-time",
                "Ride with ease \nArrive in style!"};
        descriptions = new String[]{"Wherever you're headed, we’re here to take you there, faster and safer. Enjoy the convenience of booking a ride with just a few taps. ",
                "Our seamless booking process ensures that you’re always just a tap away from your next journey.\nReliable drivers and real-time tracking give you peace of mind.",
                "Press continue to book your first ride and experience the smoothest journey yet! Whether it’s a quick trip across town or a long-distance adventure, \nLet’s hit the road together!"};

    }

    @Override
    public void onBackPressed() {

        if (fragPager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            fragPager.setCurrentItem(fragPager.getCurrentItem() - 1);
        }
    }

    private FragmentStateAdapter createStateAdapter() {
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return NUM_PAGES;
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return new WelcomeFrag(anims[position], titles[0], descriptions[0]);
            }
        };
        return adapter;
    };


}

