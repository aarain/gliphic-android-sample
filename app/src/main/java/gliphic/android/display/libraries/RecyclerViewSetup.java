/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.libraries;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.SoundEffectConstants;

import gliphic.android.adapters.AlertsAdapter;
import gliphic.android.adapters.ContactsAdapter;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.GroupSelectionActivity;
import gliphic.android.adapters.GroupsAdapter;
import gliphic.android.listeners.RecyclerViewItemTouchListener;
import gliphic.android.operation.TempGlobalStatics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Methods for setting up a RecyclerView using a ContactsAdapter or GroupsAdapter.
 */
public class RecyclerViewSetup {
    /**
     * Whenever an activity requires a RecyclerView to be updated as a result of an action from another activity,
     * this request code should be used when starting the new activity via the startActivityForResult() method
     */
    public static final int UPDATE_RECYCLER_VIEWS = 1;

    /**
     * Factor out the common setup methods for all RecyclerViews.
     *
     * @param recyclerView      The RecyclerView instance to operate on.
     * @param callingContext    The activity or fragment context containing the RecyclerView.
     */
    private static void setupBasicRecyclerView(@NonNull final RecyclerView recyclerView,
                                               @NonNull final Context callingContext) {

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(callingContext);
        recyclerView.setLayoutManager(linearLayoutManager);

        // Allow smooth, continuous, decelerating scrolling after the user flings the screen.
        ViewCompat.setNestedScrollingEnabled(recyclerView, false);

        // Display item dividers between RecyclerView items.
        try {
            recyclerView.addItemDecoration(new DividerItemDecoration(callingContext, LinearLayoutManager.VERTICAL));
        }
        catch (NullPointerException e) {
            // TODO: Rework this crude fix (which catches a NullPointerException and does nothing) which occurs when
            //       running multiple instrumentation test classes without the server(s).
            //
            //       Unexpected behaviour which is shown as part of the test error is that the
            //       RequestGlobalStatic.requestAndSetTargetContacts method is called even when the failed test
            //       only deals with all contacts (not just exclusively group contacts).
            //
            //       Note that, as of writing this comment, a NullPointerException has only ever been observed here
            //       when running instrumentation tests for multiple classes without the server(s).
        }
    }

    /**
     * @see #handleSetupStandardRecyclerView(RecyclerView, Context, RecyclerView.Adapter).
     */
    public static RecyclerView setupStandardRecyclerView(@NonNull final RecyclerView recyclerView,
                                                         @NonNull final Context callingFragmentContext,
                                                         @NonNull final AlertsAdapter alertsAdapter) {

        // The item-touch listeners for the buttons in each RecyclerView item are in the corresponding adapter.

        return handleSetupStandardRecyclerView(recyclerView, callingFragmentContext, alertsAdapter);
    }

    /**
     * @see #handleSetupStandardRecyclerView(RecyclerView, Context, RecyclerView.Adapter).
     */
    public static RecyclerView setupStandardRecyclerView(@NonNull final RecyclerView recyclerView,
                                                         @NonNull final BaseMainActivity callingActivity,
                                                         @NonNull final ContactsAdapter contactsAdapter) {

        return handleSetupStandardRecyclerView(recyclerView, callingActivity, contactsAdapter);
    }

    /**
     * @see #handleSetupStandardRecyclerView(RecyclerView, Context, RecyclerView.Adapter).
     */
    public static RecyclerView setupStandardRecyclerView(@NonNull final RecyclerView recyclerView,
                                                         @NonNull final BaseMainActivity callingActivity,
                                                         @NonNull final GroupsAdapter groupsAdapter) {

        return handleSetupStandardRecyclerView(recyclerView, callingActivity, groupsAdapter);
    }

    /**
     * Initialize the given RecyclerView with an instance of the given adapter.
     *
     * Note that no RecyclerViewItemTouchListener is added to the RecyclerView so this has to be handled by the calling
     * activity if required.
     *
     * @param recyclerView  The RecyclerView instance identified by its ID.
     * @param context       The activity which called this method.
     * @param adapter       The adapter to set for the RecyclerView.
     * @return              The RecyclerView object instantiated with the given adapter.
     */
    private static RecyclerView handleSetupStandardRecyclerView(@NonNull final RecyclerView recyclerView,
                                                                @NonNull final Context context,
                                                                @NonNull final RecyclerView.Adapter adapter) {

        setupBasicRecyclerView(recyclerView, context);

        recyclerView.setAdapter(adapter);

        return recyclerView;
    }

    /**
     * Initialise the given RecyclerView with a ContactsAdapter instance, and create a RecyclerViewItemTouchListener
     * which will start a new activity when an item in the RecyclerView is selected.
     *
     * @param recyclerView          The RecyclerView instance identified by its ID.
     * @param callingFragment       The fragment which called this method.
     * @param destinationActivity   The class to launch an instance of from the onItemClick listener.
     * @param contactsAdapter       The ContactsAdapter to set for the RecyclerView.
     * @return                      The RecyclerView object instantiated with the given adapter.
     */
    @Nullable
    public static RecyclerView setupContactsRecyclerView(RecyclerView recyclerView,
                                                         final Fragment callingFragment,
                                                         final Class<?> destinationActivity,
                                                         final ContactsAdapter contactsAdapter) {

        final Context context = callingFragment.getContext();
        if (context == null) {
            return null;
        }

        setupBasicRecyclerView(recyclerView, context);

        recyclerView.setAdapter(contactsAdapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerViewItemTouchListener(
                        context,
                        (view, position) -> {
                            final Activity activity = callingFragment.getActivity();
                            if (activity == null) {
                                return;
                            }

                            // The default 'tick' sound does not play automatically in a RecyclerView.
                            view.playSoundEffect(SoundEffectConstants.CLICK);

                            TempGlobalStatics.setContactClicked(contactsAdapter.getItemContact(position));

                            Intent intent = new Intent(activity, destinationActivity);
                            callingFragment.startActivityForResult(intent, UPDATE_RECYCLER_VIEWS);
                        }
                )
        );

        return recyclerView;
    }

    /**
     * Initialise the given RecyclerView with a ContactsAdapter instance, and create a RecyclerViewItemTouchListener
     * which will start a new activity when an item in the RecyclerView is selected.
     *
     * @param recyclerView          The RecyclerView instance identified by its ID.
     * @param callingActivity       The activity which called this method.
     * @param destinationActivity   The class to launch an instance of from the onItemClick listener.
     * @param contactsAdapter       The ContactsAdapter to set for the RecyclerView.
     * @return                      The RecyclerView object instantiated with the given adapter.
     */
    public static RecyclerView setupContactsRecyclerView(RecyclerView recyclerView,
                                                         final Activity callingActivity,
                                                         final Class<?> destinationActivity,
                                                         final ContactsAdapter contactsAdapter) {

        setupBasicRecyclerView(recyclerView, callingActivity);

        recyclerView.setAdapter(contactsAdapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerViewItemTouchListener(
                        callingActivity,
                        (view, position) -> {
                            // The default 'tick' sound does not play automatically in a RecyclerView.
                            view.playSoundEffect(SoundEffectConstants.CLICK);

                            TempGlobalStatics.setContactClicked(contactsAdapter.getItemContact(position));

                            Intent intent = new Intent(callingActivity, destinationActivity);
                            callingActivity.startActivityForResult(intent, UPDATE_RECYCLER_VIEWS);
                        }
                )
        );

        return recyclerView;
    }

    /**
     * Initialise the given RecyclerView with a GroupsAdapter instance, and create a RecyclerViewItemTouchListener
     * which will start a new activity when an item in the RecyclerView is selected.
     *
     * @param recyclerView          The RecyclerView instance identified by its ID.
     * @param callingFragment       The fragment which called this method.
     * @param destinationActivity   The class to launch an instance of from the onItemClick listener.
     * @param groupsAdapter         The GroupsAdapter to set for the RecyclerView.
     * @return                      The RecyclerView object instantiated with the given adapter.
     */
    @Nullable
    public static RecyclerView setupGroupsRecyclerView(RecyclerView recyclerView,
                                                       final Fragment callingFragment,
                                                       final Class<?> destinationActivity,
                                                       final GroupsAdapter groupsAdapter) {

        final Context context = callingFragment.getContext();
        if (context == null) {
            return null;
        }

        setupBasicRecyclerView(recyclerView, context);

        recyclerView.setAdapter(groupsAdapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerViewItemTouchListener(
                        callingFragment.getContext(),
                        (view, position) -> {
                            final Activity activity = callingFragment.getActivity();
                            if (activity == null) {
                                return;
                            }

                            // The default 'tick' sound does not play automatically in a RecyclerView.
                            view.playSoundEffect(SoundEffectConstants.CLICK);

                            TempGlobalStatics.setGroupClicked(groupsAdapter.getItemGroup(position));

                            Intent intent = new Intent(activity, destinationActivity);
                            callingFragment.startActivityForResult(intent, UPDATE_RECYCLER_VIEWS);
                        }
                )
        );

        return recyclerView;
    }

    /**
     * Initialise the given RecyclerView with a GroupsAdapter instance, and create a RecyclerViewItemTouchListener
     * which will change the currently selected group and alert the user of this change.
     *
     * @param viewId            The resource ID in the XML file for the RecyclerView.
     * @param callingActivity   The activity which called this method.
     * @param groupsAdapter     The GroupsAdapter to set for the RecyclerView.
     * @return                  The RecyclerView object instantiated with the given adapter.
     */
    public static RecyclerView setupGroupSelectionRecyclerView(int viewId,
                                                               final BaseMainActivity callingActivity,
                                                               final GroupsAdapter groupsAdapter) {

        RecyclerView recyclerView = callingActivity.findViewById(viewId);

        setupBasicRecyclerView(recyclerView, callingActivity);

        recyclerView.setAdapter(groupsAdapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerViewItemTouchListener(
                        callingActivity,
                        (view, position) -> {
                            // The default 'tick' sound does not play automatically in a RecyclerView.
                            view.playSoundEffect(SoundEffectConstants.CLICK);

                            GroupSelectionActivity.selectGroup(
                                    callingActivity,
                                    groupsAdapter.getItemGroup(position)
                            );
                        }
                )
        );

        return recyclerView;
    }
}
