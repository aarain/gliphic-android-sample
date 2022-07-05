/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import gliphic.android.R;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.libraries.LoadingDialog;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.display.libraries.TabLayoutMethods;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.misc.IntentHandler;
import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import com.google.android.material.tabs.TabLayout;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import libraries.GeneralUtils;
import libraries.Vars;
import pojo.account.GroupShare;
import pojo.group.GroupShareLoadabilityRequest;
import pojo.group.LoadableGroupSharesResponse;
import pojo.group.UnloadableGroupSharesResponse;
import pojo.misc.ContactAndGroupNumberPair;
import pojo.xmpp.XMPPMessageBody;

/**
 * The main activity which runs when the user starts the app.
 */
public class MainActivity extends BaseMainActivity {

    // (Additional) intent actions.
    private static final String UPDATE_ALERTS_TAB_ACTION = "UpdateAlertsTabAction";
    private static final String UPDATE_GROUPS_TAB_ACTION = "UpdateGroupsTabAction";

    // Handle tabs.
    private PagerAdapter mPagerAdapter;
    private TabLayout mTabLayout;

    // Hold references to all of the fragments created within this activity.
    private AlertsTab    alertsTab    = null;
    private WorkspaceTab workspaceTab = null;
    private GroupsTab    groupsTab    = null;
    private ContactsTab  contactsTab  = null;

    /**
     * Send an intent with an action to update the Alerts tab to an instance of this activity from a given calling
     * activity.
     *
     * This intent action can update any of the following:
     * * Alerts tab title depending on the number of actionable alerts in the AlertsAdapter.
     * * Show and hide views depending on the number of items in the AlertsAdapter.
     *
     * @param activity  The activity instance to send the local broadcast from.
     */
    public static void sendBroadcastToUpdateAlertsTab(@NonNull BaseMainActivity activity) {
        LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(UPDATE_ALERTS_TAB_ACTION));
    }

    /**
     * Send an intent with an action to update the Groups tab to an instance of this activity from a given calling
     * activity.
     *
     * This intent action can update any of the following:
     * * Show and hide views depending on the number of items in the GroupsAdapter.
     *
     * @param activity  The activity instance to send the local broadcast from.
     */
    public static void sendBroadcastToUpdateGroupsTab(@NonNull BaseMainActivity activity) {
        LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(UPDATE_GROUPS_TAB_ACTION));
    }

    private void logNullAccessToken() {
        Log.w(HttpOperations.GENERIC_LOG_TAG, "Unable to obtain an access token.");
    }

    private void logJsonSyntaxException(@NonNull Class clazz) {
        String s = "Failed to deserialize the JSON object response into an object of class %s.";
        Log.e(HttpOperations.GENERIC_LOG_TAG, String.format(s, clazz.getName()));
    }

    private void logReturnListEmpty(@NonNull Class clazz) {
        String s = "Response list in %s object is empty.";
        Log.w(HttpOperations.GENERIC_LOG_TAG, String.format(s, clazz.getName()));
    }

    // Receive local broadcasts for this activity and any fragments created within this activity.
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String intentAction = intent.getAction();
            if (intentAction == null) {
                return;
            }

            final Vars.GroupShareStatus statusToCheck;
            final XMPPMessageBody xmppMessageBody;

            if (intentAction.equals(UPDATE_ALERTS_TAB_ACTION)) {
                setAlertsTabTitle();
                alertsTab.showAndRemoveViews();
                return;
            }
            else if (intentAction.equals(UPDATE_GROUPS_TAB_ACTION)) {
                groupsTab.safeAdapterReset();
                return;
            }
            else if (intentAction.equals(XMPPMessageBody.Subject.GROUP_REQUEST_SUBMITTED.get())) {
                statusToCheck = Vars.GroupShareStatus.PENDING_RECEIVED;
            }
            else if (intentAction.equals(XMPPMessageBody.Subject.GROUP_REQUEST_ACCEPTED.get())) {
                statusToCheck = Vars.GroupShareStatus.SUCCESS_SENT;
            }
            else if (intentAction.equals(XMPPMessageBody.Subject.GROUP_REQUEST_FAILED.get())) {
                statusToCheck = Vars.GroupShareStatus.FAILED_SENT;
            }
            else if (intentAction.equals(XMPPMessageBody.Subject.GROUP_REQUEST_DECLINED.get())) {
                statusToCheck = Vars.GroupShareStatus.FAILED_SENT;  // There is no specific "SUCCESS_DENIED" status.

                xmppMessageBody = IntentHandler.getIntentExtra(intent);
                if (xmppMessageBody == null) {
                    return;
                }

                RequestGlobalStatic.requestAndSetAccessToken(
                        accessToken -> {
                            if (accessToken == null) {
                                logNullAccessToken();
                                return;
                            }

                            final GroupShareLoadabilityRequest gslr = new GroupShareLoadabilityRequest(
                                    accessToken,
                                    statusToCheck,  // The server only distinguishes between sent or received statuses
                                    // during a decline-check; the specific value is irrelevant.
                                    xmppMessageBody.getContactAndGroupNumberPairs()
                            );

                            HttpOperations.post(
                                    HttpOperations.URI_CHECK_SHARE_UNLOADABLE,
                                    gslr,
                                    MainActivity.this,
                                    response -> {
                                        List<ContactAndGroupNumberPair> returnedContactAndGroupNumberPairs =
                                                GeneralUtils.fromJson(
                                                        response,
                                                        UnloadableGroupSharesResponse.class
                                                ).getContactAndGroupNumberPairs();

                                        if (returnedContactAndGroupNumberPairs.isEmpty()) {
                                            logReturnListEmpty(UnloadableGroupSharesResponse.class);
                                            return;
                                        }

                                        // Remove any existing references to any matching object.

                                        final List<GroupShare> removedGroupShares = Alerts.safeRemoveGroupShares(
                                                returnedContactAndGroupNumberPairs
                                        );

                                        alertsTab.removeGroupShareAndUpdateDisplay(removedGroupShares);

                                        if (Alerts.isActionableGroupShare(statusToCheck)) {
                                            setAlertsTabTitle();
                                        }
                                    },
                                    error -> HttpOperations.handleOnErrorResponseAfterReceivedXmppMessage(
                                            error,
                                            MainActivity.this
                                    )
                            );
                        },
                        MainActivity.this,
                        null,
                        false
                );

                return;
            }
            else {
                return;
            }

            // If the if-else statement has not returned from this method then assume that the intent's action requires
            // a standard request to check the loadability of the group-share.

            xmppMessageBody = IntentHandler.getIntentExtra(intent);
            if (xmppMessageBody == null) {
                return;
            }

            RequestGlobalStatic.requestAndSetAccessToken(
                    accessToken -> {
                        if (accessToken == null) {
                            logNullAccessToken();
                            return;
                        }

                        final GroupShareLoadabilityRequest gslr = new GroupShareLoadabilityRequest(
                                accessToken,
                                statusToCheck,
                                xmppMessageBody.getContactAndGroupNumberPairs()
                        );

                        HttpOperations.post(
                                HttpOperations.URI_CHECK_SHARE_LOADABLE,
                                gslr,
                                MainActivity.this,
                                response -> {
                                    List<GroupShare> returnedGroupShares = GeneralUtils.fromJson(
                                            response,
                                            LoadableGroupSharesResponse.class
                                    ).getGroupShares();

                                    if (returnedGroupShares.isEmpty()) {
                                        logReturnListEmpty(LoadableGroupSharesResponse.class);
                                        return;
                                    }

                                    // Update any existing references to any matching object.
                                    List<GroupShare> storedGroupShares = Alerts.storeStatically(returnedGroupShares);

                                    // Remove and prepend new item(s) to the display.
                                    alertsTab.prependGroupShareAndUpdateDisplay(storedGroupShares);

                                    if (Alerts.isActionableGroupShare(statusToCheck)) {
                                        setAlertsTabTitle();
                                    }
                                },
                                error -> HttpOperations.handleOnErrorResponseAfterReceivedXmppMessage(
                                        error,
                                        MainActivity.this
                                )
                        );
                    },
                    MainActivity.this,
                    null,
                    false
            );
        }
    };

    private int getAlertsTabIndex() {
        return mPagerAdapter.tabTitles.indexOf(getString(R.string.main_alerts));
    }

    private int getWorkspaceTabIndex() {
        return mPagerAdapter.tabTitles.indexOf(getString(R.string.main_workspace));
    }

    private void setAlertsTabTitle() {
        final int largestDisplayedCount = 9;
        final int alertsCounter = Alerts.getActionableAlertsCount();

        final String alertsTabName;

        if (alertsCounter > largestDisplayedCount) {
            alertsTabName = String.format("%d+", largestDisplayedCount);
        }
        else {
            alertsTabName = Integer.toString(alertsCounter);
        }

        mTabLayout.getTabAt(getAlertsTabIndex()).setText(alertsTabName);

//        // Change the color of a single tab's text.
//        final int tabIndex = 0;
//        LinearLayout v = (LinearLayout) mTabLayout.getChildAt(0);
//        LinearLayout vTab = (LinearLayout) v.getChildAt(tabIndex);
//        TextView textView = (TextView) vTab.getChildAt(1);
//        textView.setTextColor(Color.RED);
//
//        // Ensure that the image displays at its original size.
//        View view1 = getLayoutInflater().inflate(R.layout.text_over_image, null);
//        view1.findViewById(R.id.image_background).setBackgroundResource(R.drawable.icon_octagon_outline_white);
//        mTabLayout.addTab(mTabLayout.newTab().setCustomView(view1));
    }

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

        // If the user has not signed in then force them to do so immediately.
        if (!SharedPreferencesHandler.isUserSignedIn(MainActivity.this)) {
            // Ensure that all contact data is removed.
            SharedPreferencesHandler.removeAllContactData(MainActivity.this);

            // Start the sign-in activity.
            Intent myIntent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(myIntent);
            // Kill this activity.
            finish();
            return;
        }

        /* Display setup */

        setContentView(R.layout.activity_base_layout);

        Toolbar mainToolbar = findViewById(R.id.toolbar_activity_base);
        setSupportActionBar(mainToolbar);

        // Set up ViewPager for content below tabs, and TabLayout for the actual tabs.
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager(), MainActivity.this);
        final ViewPager viewPager = findViewById(R.id.viewpager_activity_base);
        viewPager.setAdapter(mPagerAdapter);
        // This number of pages should never exist, so all pages are retained.
        viewPager.setOffscreenPageLimit(9);
        // Set the default tab which is selected on start-up.
        viewPager.setCurrentItem(getWorkspaceTabIndex());

        mTabLayout = findViewById(R.id.tablayout_activity_base);
        mTabLayout.setupWithViewPager(viewPager);

        // Reduce the weight of the Alerts tab manually.
        LinearLayout layout =
                ((LinearLayout) ((LinearLayout) mTabLayout.getChildAt(0)).getChildAt(getAlertsTabIndex()));
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) layout.getLayoutParams();
        layoutParams.weight = 0.5f;
        layout.setLayoutParams(layoutParams);

        setAlertsTabTitle();

        // A LoadingDialog and AlertDialog should be displayed when the MainActivity is initialized.
        // This removes the need for child tabs (fragments) to display an AlertDialog in their onCreateView methods
        // and prevents duplicate dialogs from being displayed when these onCreateViews are called (i.e. in both the
        // WorkspaceTab and GroupsTab).
        final LoadingDialog loadingDialog = new LoadingDialog(MainActivity.this);

        RequestGlobalStatic.requestAndSetAccessToken(
                accessToken -> {
                    if (accessToken != null) {
                        loadingDialog.dismissDialog();
                    }
                },
                MainActivity.this,
                loadingDialog,
                false
        );
    }

    @Override
    public void onStart() {
        super.onStart();

        if (localBroadcastManager == null) {
            localBroadcastManager = LocalBroadcastManager.getInstance(this);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UPDATE_ALERTS_TAB_ACTION);
        intentFilter.addAction(UPDATE_GROUPS_TAB_ACTION);
        intentFilter.addAction(XMPPMessageBody.Subject.GROUP_REQUEST_SUBMITTED.get());
        intentFilter.addAction(XMPPMessageBody.Subject.GROUP_REQUEST_ACCEPTED.get());
        intentFilter.addAction(XMPPMessageBody.Subject.GROUP_REQUEST_FAILED.get());
        intentFilter.addAction(XMPPMessageBody.Subject.GROUP_REQUEST_DECLINED.get());

        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (localBroadcastManager != null && broadcastReceiver != null) {
            localBroadcastManager.unregisterReceiver(broadcastReceiver);
        }
    }

    /**
     * Disable the home button in the main activity toolbar.
     *
     * If the home button was not disabled in this activity then if it was clicked the call to
     * NavUtils.navigateUpFromSameTask(activity) from the onOptionsItemSelected method would throw
     * an IllegalArgumentException because there is no parent activity for MainActivity.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_bar_home).setVisible(false);
        super.onPrepareOptionsMenu(menu);
        return true;
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

        List<String> tabTitles = Arrays.asList(
                getString(R.string.main_alerts),
                getString(R.string.main_workspace),
                getString(R.string.main_groups),
                getString(R.string.main_contacts)
        );

        private PagerAdapter(FragmentManager fm, Context context) {
            super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.context = context;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new AlertsTab();
                case 1:
                default:
                    return new WorkspaceTab();
                case 2:
                    return new GroupsTab();
                case 3:
                    return new ContactsTab();
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);

            switch (position) {
                case 0:
                    alertsTab = (AlertsTab) createdFragment;
                    break;
                case 1:
                    workspaceTab = (WorkspaceTab) createdFragment;
                    break;
                case 2:
                    groupsTab = (GroupsTab) createdFragment;
                    break;
                case 3:
                    contactsTab = (ContactsTab) createdFragment;
                    break;
            }

            return createdFragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // Generate the title based on its array position.
            return tabTitles.get(position);
        }

        @Override
        public int getCount() {
            return tabTitles.size();
        }
    }

    /**
     * This fires when an activity launched using startActivityForResult() exits and focus returns to this activity.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (alertsTab != null) {
            alertsTab.onActivityResult(requestCode, resultCode, data);
        }

        if (workspaceTab != null) {
            workspaceTab.onActivityResult(requestCode, resultCode, data);
        }

        if (groupsTab != null) {
            groupsTab.onActivityResult(requestCode, resultCode, data);
        }

        if (contactsTab != null) {
            contactsTab.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * This fires when an app has shared data with this app, but an instance of this activity is already available.
     *
     * The required EditText view has already been created so data can be put into it without requiring the Workspace
     * tab to create the EditText view first.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleSharedText(intent, findViewById(R.id.edittext_encrypt), findViewById(R.id.edittext_decrypt));
    }

    /**
     * The types of text which can be shared with this application.
     */
    private enum TEXT_TYPE {
        PLAIN_TEXT  ("Plain"),
        CIPHER_TEXT ("Cipher");

        private final String name;

        TEXT_TYPE(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Share data from another app to this app by allowing the user to choose where in the Workspace tab it gets
     * displayed.
     *
     * @param intent            The received intent from the other app.
     * @param editTextEncrypt   The EditText to paste plain text in to.
     * @param editTextDecrypt   The EditText to paste cipher (published) text in to.
     */
    public void handleSharedText(@NonNull Intent intent,
                                 @NonNull final EditText editTextEncrypt,
                                 @NonNull final EditText editTextDecrypt) {

        String intentAction = intent.getAction();
        String intentType = intent.getType();
        if (Intent.ACTION_SEND.equals(intentAction) && intentType != null) {
            if ("text/plain".equals(intentType)) {
                final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    this.setMAlertDialog(alertDialog);
                    alertDialog.setTitle("Insert text as...");
                    final String msg = "Choose to insert shared text as either plain text or cipher text.";
                    alertDialog.setMessage(msg);
                    alertDialog.setButton(
                            AlertDialog.BUTTON_NEUTRAL,
                            TEXT_TYPE.PLAIN_TEXT.getName(),
                            (dialog, which) -> handleShareToEditText(
                                    TEXT_TYPE.PLAIN_TEXT,
                                    dialog,
                                    alertDialog,
                                    editTextEncrypt,
                                    editTextDecrypt,
                                    sharedText
                            ));
                    alertDialog.setButton(
                            AlertDialog.BUTTON_POSITIVE,
                            TEXT_TYPE.CIPHER_TEXT.getName(),
                            (dialog, which) -> handleShareToEditText(
                                    TEXT_TYPE.CIPHER_TEXT,
                                    dialog,
                                    alertDialog,
                                    editTextEncrypt,
                                    editTextDecrypt,
                                    sharedText
                            ));
                    alertDialog.show();

                    // Force this activity to switch to the Workspace tab.
                    mTabLayout.getTabAt(getWorkspaceTabIndex()).select();
                }
            }
        }
    }

    private void handleShareToEditText(@NonNull final TEXT_TYPE textType,
                                       @NonNull final DialogInterface dialogInterface,
                                       @NonNull final AlertDialog alertDialog,
                                       @NonNull final EditText editTextEncrypt,
                                       @NonNull final EditText editTextDecrypt,
                                       @NonNull final String sharedText) {

        // Dismiss the share prompt.

        dialogInterface.dismiss();

        // Modify EditText views.

        final EditText insertedEditText;
        final EditText clearedEditText;

        switch (textType) {
            case PLAIN_TEXT:
                insertedEditText = editTextEncrypt;
                clearedEditText  = editTextDecrypt;
                break;
            case CIPHER_TEXT:
                insertedEditText = editTextDecrypt;
                clearedEditText  = editTextEncrypt;
                break;
            default:
                String s = "Invalid text type '%s'";
                throw new Error(new IllegalArgumentException(String.format(s, textType.getName())));
        }

        clearedEditText.setText("");
        insertedEditText.setText(sharedText);

        // Remove the shown AlertDialog which is stored.

        if (alertDialog.equals(MainActivity.this.getMAlertDialog())) {
            MainActivity.this.setMAlertDialog(null);
        }
    }
}
