/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.adapters;

public class GroupShareItemCounts {
    private long pendingReceivedItemCount = 0;
    private long pendingSentItemCount = 0;
    private long successReceivedItemCount = 0;
    private long successAndFailedSentItemCount = 0;

    public GroupShareItemCounts() {}

    public long getPendingReceivedItemCount() {
        return pendingReceivedItemCount;
    }

    public long getPendingSentItemCount() {
        return pendingSentItemCount;
    }

    public long getSuccessReceivedItemCount() {
        return successReceivedItemCount;
    }

    public long getSuccessAndFailedSentItemCount() {
        return successAndFailedSentItemCount;
    }

    public void incrementPendingReceivedItemCount() {
        pendingReceivedItemCount++;
    }

    public void incrementPendingSentItemCount() {
        pendingSentItemCount++;
    }

    public void incrementSuccessReceivedItemCount() {
        successReceivedItemCount++;
    }

    public void incrementSuccessAndFailedSentItemCount() {
        successAndFailedSentItemCount++;
    }
}
