/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.main.group_details;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import gliphic.android.R;
import gliphic.android.operation.TempGlobalStatics;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.libraries.TabLayoutMethods;
import gliphic.android.operation.Group;
import gliphic.android.exceptions.NullStaticVariableException;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

/**
 * The group details activity for a single group.
 *
 * This activity contains child-tabs.
 */
public class GroupDetailsActivity extends BaseMainActivity {
    // Hold references to all of the fragments created within this activity.
    private GroupSettingsTab groupSettingsTab = null;
    private GroupContactsTab groupContactsTab = null;

    // The group whose details are to be shown in this activity.
    public Group viewedGroup;

    @Override
    public void onNetworkAvailable(boolean isFirstOnNetworkAvailable) {
        // Do nothing since this is handled for each fragment.
    }

    @Override
    public void noNetworkOnStart() {
        // Do nothing since this is handled for each fragment.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the group the user clicked on.
        try {
            this.viewedGroup = TempGlobalStatics.getGroupClicked();
        }
        catch (NullStaticVariableException e) {
            AlertDialogs.internalErrorDialog(
                    GroupDetailsActivity.this,
                    true,
                    "Unable to load group details at this time."
            );
            return;
        }

        setContentView(R.layout.activity_base_layout);

        // Toolbar configuration.
        setSupportActionBar(findViewById(R.id.toolbar_activity_base));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set up ViewPager for content below tabs, and TabLayout for the actual tabs.
        final ViewPager viewPager = findViewById(R.id.viewpager_activity_base);
        PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), GroupDetailsActivity.this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(9);     // This number of pages should never exist so all pages are retained.

        final TabLayout tabLayout = findViewById(R.id.tablayout_activity_base);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overflow_menu, menu);

        TabLayoutMethods.onCreateOptionsMenuContent(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final boolean isItemSelected = TabLayoutMethods.onOptionsItemSelectedContent(item, this);

        return isItemSelected || super.onOptionsItemSelected(item);
    }

    private class PagerAdapter extends FragmentPagerAdapter {
        Context context;
        String[] tabTitles = new String[] {
                getString(R.string.group_settings),
                getString(R.string.group_contacts)
        };

        private PagerAdapter(FragmentManager fm, Context context) {
            super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.context = context;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                default:
                    return new GroupSettingsTab();
                case 1:
                    return new GroupContactsTab();
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);

            switch (position) {
                case 0:
                    groupSettingsTab = (GroupSettingsTab) createdFragment;
                    break;
                case 1:
                    groupContactsTab = (GroupContactsTab) createdFragment;
                    break;
            }

            return createdFragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // Generate the title based on its array position.
            return tabTitles[position];
        }

        @Override
        public int getCount() {
            return tabTitles.length;
        }
    }

    /**
     * This fires when an activity launched using startActivityForResult() exits and focus returns
     * to this activity. See fragment implementation of onActivityResult() for more details.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (groupSettingsTab != null) {
            groupSettingsTab.onActivityResult(requestCode, resultCode, data);
        }

        if (groupContactsTab != null) {
            groupContactsTab.onActivityResult(requestCode, resultCode, data);
        }
    }

//    /**
//     * Get the most recent AlertDialog this activity has displayed. Note that this includes any AlertDialog which is
//     * currently showing.
//     *
//     * This method allows other classes (e.g. classes extending Fragment) which have access to an instance of this
//     * class to access this method.
//     *
//     * @return  The AlertDialog object variable accessed from the parent class.
//     */
//    public AlertDialog getMAlertDialog() {
//        return mAlertDialog;
//    }
}
