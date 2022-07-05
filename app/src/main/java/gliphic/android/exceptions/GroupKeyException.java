/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.exceptions;

public class GroupKeyException extends GroupException {
    public GroupKeyException(String message) {
        super(message);
    }

    public GroupKeyException(Throwable throwable) {
        super(throwable);
    }

    public GroupKeyException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
