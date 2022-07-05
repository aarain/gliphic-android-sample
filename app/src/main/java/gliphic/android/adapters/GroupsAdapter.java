/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import gliphic.android.R;
import gliphic.android.operation.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The adapter for a list of Contacts.
 *
 * Facilitates creating a RecyclerView to list a user's Contacts.
 */
public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.GroupItemViewHolder> {
    public static final String GENERIC_LOAD_GROUPS_FAILED_MSG = "Unable to load more groups at this time.";

    public static final int MAX_NUM_OF_ITEMS_APPENDED = 50;

    private int maxNumOfItemsDisplayed = MAX_NUM_OF_ITEMS_APPENDED;

    // All groups available to this adapter before any filters are applied.
    private List<Group> completeGroupList = new ArrayList<>();
    // The list of groups available after all filters are applied.
    private List<Group> filteredGroupList = new ArrayList<>();
    // The list of groups to display after all filters are applied and the item display limit is applied.
    private List<Group> displayedGroupList = new ArrayList<>();

    // Filter(s) for this adapter.
    private String searchString = "";

    static private class NullListOrContainsNulls extends NullPointerException {
        NullListOrContainsNulls(String message) {
            super(message);
        }
    }

    private boolean isNullOrContainsNulls(List<Group> groupList) {
        if (groupList == null) {
            return true;
        }

        for (Group group : groupList) {
            if (group == null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Constructor for initialising the adapter with groups this contact knows.
     *
     * @param groupList     The complete list of known groups for this contact. If this is null it is assumed that the
     *                      app could not load any groups from either the local cache or from the server.
     */
    public GroupsAdapter(@Nullable List<Group> groupList) {
        // Assume that the calling method handles the else-case.
        if (!isNullOrContainsNulls(groupList)) {
            completeGroupList.addAll(groupList);
            filteredGroupList.addAll(filter(searchString));
            displayedGroupList.addAll(
                    filteredGroupList.subList(
                            0,
                            Math.min(filteredGroupList.size(), maxNumOfItemsDisplayed)
                    )
            );
        }
    }

    /**
     * Return the search string associated with the given adapter.
     *
     * @param groupsAdapter     An instance of this class (or null).
     * @return                  Null if the given adapter is null and a non-null string otherwise.
     */
    public static String getSearchString(@Nullable GroupsAdapter groupsAdapter) {
        if (groupsAdapter == null) {
            return null;
        }
        else {
            return groupsAdapter.searchString;
        }
    }

    static class GroupItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView groupImage;
        private TextView  groupName;
        private TextView  groupDescription;

        private GroupItemViewHolder(View view) {
            super(view);

            this.groupImage       = view.findViewById(R.id.group_item_img);
            this.groupName        = view.findViewById(R.id.group_item_name);
            this.groupDescription = view.findViewById(R.id.group_item_description);
        }
    }

    @Override
    @NonNull
    public GroupItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupItemViewHolder groupItemViewHolder, int position) {
        Group groupItem = getItemGroup(position);

        groupItem.getImage().setImageView(groupItemViewHolder.groupImage);
        groupItemViewHolder.groupName.setText(groupItem.getName());
        groupItemViewHolder.groupDescription.setText(groupItem.getDescription());
    }

    @Override
    public int getItemCount() {
        return (displayedGroupList != null ? displayedGroupList.size() : 0);
    }

    /**
     * Get the total filtered number of groups stored in this adapter.
     *
     * @return  Return the size of the filtered groups list, if the list is null then return 0.
     */
    public int getFilteredItemCount() {
        return (filteredGroupList != null ? filteredGroupList.size() : 0);
    }

    /**
     * Get the total unfiltered number of groups stored in this adapter.
     *
     * @return  Return the size of the complete groups list, if the list is null then return 0.
     */
    public int getTotalItemCount() {
        return (completeGroupList != null ? completeGroupList.size() : 0);
    }

    /**
     * Return the number of filtered items associated with the given adapter.
     *
     * @param groupsAdapter     An instance of this class (or null).
     * @return                  Zero if the given adapter is null and the return value of
     *                          {@link #getFilteredItemCount()} otherwise.
     */
    public static int getFilteredItemCount(@Nullable GroupsAdapter groupsAdapter) {
        if (groupsAdapter == null) {
            return 0;
        }
        else {
            return groupsAdapter.getFilteredItemCount();
        }
    }

    /**
     * Return the group at the given position in the displayed list of groups.
     *
     * @param position  The zero-indexed position of the group to return.
     * @return          The group stored at the given position in the displayed groups list.
     */
    public Group getItemGroup(int position) {
        return displayedGroupList.get(position);
    }

    /**
     * Filter the list of groups shown based on a given CharSequence provided by the user.
     *
     * It is assumed that the user input refers to the group name which they are searching for.
     *
     * @param charSequence  The user input to the EditText.
     * @return              The new list of filtered groups.
     */
    private List<Group> filter(@NonNull CharSequence charSequence) {
        searchString = charSequence.toString();
        List<Group> filteredGroupList = new ArrayList<>();

        // Set anchor points at the beginning of each space-separated string in the name, and ignore case.
        Pattern searchFilter = Pattern.compile("\\b" + Pattern.quote(searchString), Pattern.CASE_INSENSITIVE);

        for (Group group : completeGroupList) {
            if (searchFilter.matcher(group.getName()).find()) {
                filteredGroupList.add(group);
            }
        }

        return filteredGroupList;
    }

    /**
     * Reset the maximum number of items on display.
     */
    private void resetMaxDisplaySize() {
        maxNumOfItemsDisplayed = MAX_NUM_OF_ITEMS_APPENDED;
    }

    /**
     * Clear the adapter by clearing all lists and removing all items bound to the view holder.
     */
    public void clear() {
        resetMaxDisplaySize();

        searchString = "";

        completeGroupList.clear();
        filteredGroupList.clear();
        displayedGroupList.clear();

        notifyDataSetChanged();
    }

    /**
     * Update a RecyclerView to show only groups matching the given search-term.
     *
     * The stored complete list of groups is used to display groups and the old filtered and displayed groups are
     * discarded.
     *
     * @param charSequence  The (new) CharSequence to filter groups with.
     * @return              Return true if a server request is necessary after this method terminates, false otherwise.
     */
    public boolean update(@NonNull CharSequence charSequence) {
        // The completeGroupList should never be null or contain null groups so don't catch a NullListOrContainsNulls.
        return handleUpdate(true, true, completeGroupList, charSequence.toString());
    }

    /**
     * Update a RecyclerView to show a (new) list of groups, discarding the old list.
     *
     * The stored filterSearch string is applied to this new list.
     *
     * @param resetAdapterLists         Set to true to reset the entire data set or false to add groups to the
     *                                  existing complete list.
     * @param resetDisplayedItemCount   True to reset the items displayed or false to append to the display.
     * @param newCompleteGroupList      The list of groups to display in the RecyclerView.
     */
    public void update(boolean resetAdapterLists,
                       boolean resetDisplayedItemCount,
                       @Nullable List<Group> newCompleteGroupList) {
        try {
            handleUpdate(resetAdapterLists, resetDisplayedItemCount, newCompleteGroupList, searchString);
        }
        catch (NullListOrContainsNulls e) {
            // Assume that the calling method handles this case.
        }
    }

    private boolean handleUpdate(boolean resetAdapterLists,
                                 boolean resetDisplayedItemCount,
                                 @Nullable List<Group> newCompleteGroupList,
                                 @NonNull String searchString) throws NullListOrContainsNulls {

        if (isNullOrContainsNulls(newCompleteGroupList)) {
            throw new NullListOrContainsNulls("Either the given list of groups is null or it contains a null group.");
        }

        if (newCompleteGroupList != completeGroupList) {
            // Either reset the entire data set or append to the existing display.
            if (resetAdapterLists) {
                completeGroupList.clear();
                completeGroupList.addAll(newCompleteGroupList);
            }
            else {
                // Ensure that groups are sorted with the already displayed groups at the beginning of the list and the
                // new groups to display at the end of the list. This is required for the notifyItemRangeInserted
                // method called in the append method.
                Group.appendNewGroups(completeGroupList, newCompleteGroupList);
            }
        }

        // Apply filters to the complete list of groups.
        filteredGroupList = filter(searchString);

        // Either reset the displayed groups or append to the existing display.
        if (resetDisplayedItemCount) {
            resetMaxDisplaySize();

            displayedGroupList.clear();
            displayedGroupList.addAll(
                    filteredGroupList.subList(
                            0,
                            Math.min(filteredGroupList.size(), maxNumOfItemsDisplayed)
                    )
            );

            notifyDataSetChanged();

            return getItemCount() < maxNumOfItemsDisplayed;
        }
        else {
            return append(0);
        }
    }

    /**
     * Append the maximum number of items possible to the RecyclerView display. If fewer than this maximum are
     * available to append then no items are appended and a server request should be made to acquire more.
     *
     * This method is typically called within a call to a RecyclerView's addOnScrollListener() method when a user has
     * scrolled near/to the bottom of the RecyclerView.
     *
     * @return  Returns true if a new server request should be made to get more groups and false otherwise.
     */
    public boolean append() {
        // If MAX_NUM_OF_ITEMS_APPENDED (or more) items are available to append then they should be appended without
        // requiring a server request.
        return append(MAX_NUM_OF_ITEMS_APPENDED - 1);
    }

    /**
     * Append items to the RecyclerView display based on the given threshold.
     *
     * @param appendedItemCountThreshold    The number of items available to append must be larger than this number.
     *                                      If this is not positive, items will always be appended if they are loaded.
     * @return                              Returns true if a new server request should be made to get more alerts and
     *                                      false otherwise.
     *                                      If a server request has already been made, this return value can be safely
     *                                      ignored.
     */
    private boolean append(int appendedItemCountThreshold) {
        final int oldListSize = getItemCount();
        final int newListSize = Math.min(filteredGroupList.size(), oldListSize + MAX_NUM_OF_ITEMS_APPENDED);
        final int appendedItemCount = newListSize - oldListSize;

        if (appendedItemCount > Math.max(0, appendedItemCountThreshold)) {
            maxNumOfItemsDisplayed += MAX_NUM_OF_ITEMS_APPENDED;

            displayedGroupList.clear();
            displayedGroupList.addAll(filteredGroupList.subList(0, newListSize));

            // This method is more efficient than notifyDataSetChanged() so use it when possible.
            notifyItemRangeInserted(oldListSize, appendedItemCount);

            return false;
        }
        else {
            // If the number of items available to append is not larger than the threshold, request more from the
            // server before updating the displayed list.
            return true;
        }
    }
}
