/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import libraries.Base256;
import libraries.Base256Exception;

public class UnknownGroupIdException extends Exception {
    private final String groupId;
    private final List<String> groupIds;

    public UnknownGroupIdException(String message, @NonNull String groupId) {
        super(message);

        this.groupId = groupId;
        this.groupIds = null;
    }

    public UnknownGroupIdException(String message, @NonNull List<String> groupIds) {
        super(message);

        this.groupId = null;
        this.groupIds = groupIds;
    }

    @NonNull
    public String getGroupIdBase64() throws Base256Exception {
        if (groupId == null) {
            throw new Base256Exception("Null group ID.");
        }

        return Base256.toBase64(groupId);
    }

    @NonNull
    public List<String> getGroupIdsBase64() throws Base256Exception {
        if (groupIds == null) {
            return Collections.singletonList(getGroupIdBase64());
        }

        List<String> groupIdsBase64 = new ArrayList<>();

        for (String id : groupIds) {
            groupIdsBase64.add(Base256.toBase64(id));
        }

        return groupIdsBase64;
    }
}
