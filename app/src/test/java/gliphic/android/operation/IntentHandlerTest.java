package gliphic.android.operation;

import android.content.Intent;

import gliphic.android.operation.misc.IntentHandler;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;

import pojo.misc.ContactAndGroupNumberPair;
import pojo.xmpp.XMPPMessageBody;
import pojo.xmpp.XMPPMessageBody.Subject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IntentHandlerTest {
    private static final Subject SUBJECT        = Subject.GROUP_REQUEST_DECLINED;
    private static final long    CONTACT_NUMBER = 43;
    private static final long    GROUP_NUMBER   = 255;

    private XMPPMessageBody generateXMPPMessageBody() {
        return new XMPPMessageBody(
                SUBJECT,
                Collections.singletonList(new ContactAndGroupNumberPair(CONTACT_NUMBER, GROUP_NUMBER))
        );
    }

    private Intent generateIntent() {
        return new Intent(Subject.GROUP_REQUEST_DECLINED.get());
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Ignore("This test fails with the message: " +
            "java.lang.RuntimeException: Method putExtra in android.content.Intent not mocked.")
    @Test
    public void putAndGetIntentExtraTest() {
        final XMPPMessageBody xmppMessageBody = generateXMPPMessageBody();
        final Intent intent = generateIntent();

        IntentHandler.putAsIntentExtra(intent, xmppMessageBody);
        final XMPPMessageBody getIntent = IntentHandler.getIntentExtra(intent);

        assertThat(getIntent.getSubject(),       is(xmppMessageBody.getSubject()));

        assertThat(getIntent.getContactAndGroupNumberPairs().size(), is(1));

        final ContactAndGroupNumberPair originalPair = xmppMessageBody.getContactAndGroupNumberPairs().get(0);
        final ContactAndGroupNumberPair returnedPair = getIntent.getContactAndGroupNumberPairs().get(0);

        assertThat(returnedPair.getContactNumber(), is(originalPair.getContactNumber()));
        assertThat(returnedPair.getGroupNumber(),   is(originalPair.getGroupNumber()));
    }
}
