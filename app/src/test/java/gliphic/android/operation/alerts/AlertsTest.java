package gliphic.android.operation.alerts;

import gliphic.android.TestUtils;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.Contact;
import gliphic.android.operation.ContactTest;
import gliphic.android.operation.Group;
import gliphic.android.operation.GroupTest;

import org.bouncycastle.util.encoders.Base64;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import libraries.Base256;
import libraries.Base256Exception;
import libraries.Vars;
import pojo.account.GroupShare;
import pojo.load.LoadContactObject;
import pojo.load.LoadGroupObject;
import pojo.misc.ContactAndGroupNumberPair;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AlertsTest {
    private static final long VALID_GROUP_NUMBER   = 255;
    private static final long VALID_CONTACT_NUMBER = 43;
    private static final String encryptedGroupKeyString = Base64.toBase64String(new byte[Vars.PUB_ENC_AES_KEY_LEN]);

    private static Group groupForPendingShares;
    private static Group groupForSuccessShares;
    private static Group groupForFailedShares;

    private void setMembers(LoadContactObject loadContactObject,
                            LoadGroupObject loadGroupObject,
                            Contact contact,
                            Group group) throws Base256Exception {

        loadContactObject.setNumber(contact.getNumber());
        loadContactObject.setId(contact.getId());
        loadContactObject.setName(contact.getName());
        loadContactObject.setType(contact.getType());
        loadGroupObject.setNumber(group.getNumber());
        loadGroupObject.setName(group.getName());
        loadGroupObject.setIdBase64(Base256.toBase64(group.getId()));
    }

    private GroupShare createPendingReceivedGroupShare() throws Exception {
        final LoadContactObject loadContactObject = new LoadContactObject();
        final LoadGroupObject   loadGroupObject   = new LoadGroupObject();

        setMembers(
                loadContactObject,
                loadGroupObject,
                ContactTest.createValidExtendedContact(),
                groupForPendingShares
        );

        return new GroupShare(
                Vars.GroupShareStatus.PENDING_RECEIVED,
                System.currentTimeMillis(),
                loadGroupObject,
                loadContactObject,
                encryptedGroupKeyString
        );
    }

    private GroupShare createPendingSentGroupShare() throws Exception {
        final LoadContactObject loadContactObject = new LoadContactObject();
        final LoadGroupObject   loadGroupObject   = new LoadGroupObject();

        setMembers(
                loadContactObject,
                loadGroupObject,
                ContactTest.createValidExtendedContact2(),
                groupForPendingShares
        );

        return new GroupShare(
                Vars.GroupShareStatus.PENDING_SENT,
                System.currentTimeMillis() - 1000,
                loadGroupObject,
                loadContactObject,
                encryptedGroupKeyString
        );
    }

    private GroupShare createSuccessReceivedGroupShare() throws Exception {
        final LoadContactObject loadContactObject = new LoadContactObject();
        final LoadGroupObject   loadGroupObject   = new LoadGroupObject();

        setMembers(
                loadContactObject,
                loadGroupObject,
                ContactTest.createValidKnownContact(),
                groupForSuccessShares
        );

        return new GroupShare(
                Vars.GroupShareStatus.SUCCESS_RECEIVED,
                System.currentTimeMillis() - 2000,
                loadGroupObject,
                loadContactObject,
                encryptedGroupKeyString
        );
    }

    private GroupShare createSuccessSentGroupShare() throws Exception {
        final LoadContactObject loadContactObject = new LoadContactObject();
        final LoadGroupObject   loadGroupObject   = new LoadGroupObject();

        setMembers(
                loadContactObject,
                loadGroupObject,
                ContactTest.createValidKnownContact2(),
                groupForSuccessShares
        );

        return new GroupShare(
                Vars.GroupShareStatus.SUCCESS_SENT,
                System.currentTimeMillis() - 3000,
                loadGroupObject,
                loadContactObject,
                encryptedGroupKeyString
        );
    }

    private GroupShare createFailedSentGroupShare() throws Exception {
        final LoadContactObject loadContactObject = new LoadContactObject();
        final LoadGroupObject   loadGroupObject   = new LoadGroupObject();

        setMembers(
                loadContactObject,
                loadGroupObject,
                ContactTest.createValidKnownContact(),
                groupForFailedShares
        );

        return new GroupShare(
                Vars.GroupShareStatus.FAILED_SENT,
                System.currentTimeMillis() - 4000,
                loadGroupObject,
                loadContactObject,
                encryptedGroupKeyString
        );
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @BeforeClass
    public static void setGroups() throws Exception {
        groupForPendingShares = GroupTest.createValidGroup();
        groupForSuccessShares = GroupTest.createValidGroup2();
        groupForFailedShares  = GroupTest.createValidGroup3();
    }

    @AfterClass
    public static void clearStaticListsAfterClass() {
        TestUtils.clearStaticLists();
    }

    @Before
    public void clearStaticListsBefore() {
        TestUtils.clearStaticLists();
    }

    /* Reverse sort */

    @Test
    public void validReverseSortByShareTime() throws Exception {
        GroupShare pendingReceived = createPendingReceivedGroupShare();
        GroupShare pendingSent     = createPendingSentGroupShare();
        GroupShare successReceived = createSuccessReceivedGroupShare();
        GroupShare successSent     = createSuccessSentGroupShare();
        GroupShare failedSent      = createFailedSentGroupShare();

        List<GroupShare> groupShareList = Arrays.asList(
                successSent,
                failedSent,
                pendingReceived,
                successReceived,
                pendingSent
        );

        Alerts.reverseSortByShareTime(groupShareList);

        assertThat(groupShareList,
                is(Arrays.asList(
                        pendingReceived,
                        pendingSent,
                        successReceived,
                        successSent,
                        failedSent
                )));
    }

    @Test
    public void invalidReverseSortByShareTimeNullArgument() {
        expectedEx.expect(NullPointerException.class);

        Alerts.reverseSortByShareTime(null);
    }

    /* Get group shares */

    @Test
    public void validGetGroupShares() throws Exception {
        GroupShare groupShare = createPendingReceivedGroupShare();

        Alerts.storeStatically(groupShare);

        List<GroupShare> retrievedGroupShares = Alerts.getGroupShares();
        assertThat(retrievedGroupShares, is(Collections.singletonList(groupShare)));
        assertThat(retrievedGroupShares.get(0).getEncryptedGroupKeyString(), is(nullValue()));
    }

    @Test
    public void nullGetKnownGroups() throws NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Group shares list is null.");

        Alerts.getGroupShares();
    }

    /* Set null group shares */

    @Test
    public void validSetNullGroupShares() throws Exception {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Group shares list is null.");

        Alerts.storeStatically(createPendingReceivedGroupShare());
        Alerts.setNullGroupShares();
        Alerts.getGroupShares();
    }

    /* Get group share */

    @Test
    public void getGroupShareWithNullStaticList() {
        assertThat(Alerts.getGroupShare(VALID_CONTACT_NUMBER, VALID_GROUP_NUMBER), is(nullValue()));
    }

    @Test
    public void getGroupShareNonExistent() throws Exception {
        Alerts.storeStatically(createPendingSentGroupShare());

        assertThat(Alerts.getGroupShare(VALID_CONTACT_NUMBER, VALID_GROUP_NUMBER), is(nullValue()));
    }

    @Test
    public void getGroupShareValidShare() throws Exception {
        final GroupShare storedShare = createPendingSentGroupShare();
        Alerts.storeStatically(storedShare);

        final GroupShare retrievedShare = Alerts.getGroupShare(
                storedShare.getContact().getNumber(),
                storedShare.getGroup().getNumber()
        );

        assertThat(retrievedShare, is(storedShare));
    }

    /* Remove group share */

    @Test
    public void positiveSafeRemoveGroupShare() throws Exception {
        final GroupShare groupShare = createPendingSentGroupShare();
        Alerts.storeStatically(groupShare);

        assertThat(Alerts.safeRemoveGroupShare(groupShare), is(true));
    }

    @Test
    public void negativeSafeRemoveGroupShareNullStaticList() throws Exception {
        assertThat(Alerts.safeRemoveGroupShare(createPendingSentGroupShare()), is(false));
    }

    @Test
    public void negativeSafeRemoveGroupShareNonExistent() throws Exception {
        final GroupShare groupShare1 = createPendingSentGroupShare();
        final GroupShare groupShare2 = createPendingReceivedGroupShare();
        Alerts.storeStatically(groupShare1);

        assertThat(Alerts.safeRemoveGroupShare(groupShare2), is(false));
    }

    /* Remove group shares for a given list of contact-group number pairs */

    @Test
    public void safeRemoveGroupSharesNullList() {
        assertThat(Alerts.safeRemoveGroupShares(null), is(Collections.emptyList()));
    }

    @Test
    public void safeRemoveGroupSharesNullValueInList() throws Exception {
        final GroupShare gs1 = createPendingSentGroupShare();
        final GroupShare gs2 = createPendingReceivedGroupShare();
        final List<ContactAndGroupNumberPair> contactAndGroupNumberPairs = Arrays.asList(
                null,
                new ContactAndGroupNumberPair(gs1.getContact().getNumber(), gs1.getGroup().getNumber()),
                new ContactAndGroupNumberPair(gs2.getContact().getNumber(), gs2.getGroup().getNumber())
        );
        final List<GroupShare> groupShares = Arrays.asList(gs1, gs2);
        Alerts.storeStatically(groupShares);

        assertThat(Alerts.safeRemoveGroupShares(contactAndGroupNumberPairs), is(groupShares));
    }

    @Test
    public void safeRemoveGroupSharesReturnListSmallerThanTotalGroupShares() throws Exception {
        final GroupShare gs1 = createPendingSentGroupShare();
        final GroupShare gs2 = createPendingReceivedGroupShare();
        final List<ContactAndGroupNumberPair> contactAndGroupNumberPairs = Collections.singletonList(
                new ContactAndGroupNumberPair(gs1.getContact().getNumber(), gs1.getGroup().getNumber())
        );
        Alerts.storeStatically(Arrays.asList(gs1, gs2));

        assertThat(Alerts.safeRemoveGroupShares(contactAndGroupNumberPairs), is(Collections.singletonList(gs1)));
    }

    /* Append new group shares */

    @Test
    public void appendNewGroupSharesNewUniqueShares() throws Exception {
        GroupShare gs1 = createPendingSentGroupShare();
        GroupShare gs2 = createSuccessSentGroupShare();
        GroupShare gs3 = createFailedSentGroupShare();

        List<GroupShare> baseList = new ArrayList<>();
        List<GroupShare> newList  = new ArrayList<>();

        baseList.add(gs1);
        newList.add(gs1);
        newList.add(gs3);   // Test that this group is sorted at the end.
        newList.add(gs2);

        Alerts.appendNewGroupShares(baseList, newList);

        assertThat(baseList, is(Arrays.asList(gs1, gs2, gs3)));
    }

    @Test
    public void appendNewGroupSharesNoNewUniqueShares() throws Exception {
        GroupShare gs1 = createPendingSentGroupShare();
        GroupShare gs2 = createSuccessSentGroupShare();

        List<GroupShare> baseList = new ArrayList<>();
        List<GroupShare> newList  = new ArrayList<>();

        baseList.add(gs1);
        baseList.add(gs2);
        newList.add(gs1);
        newList.add(gs2);

        Alerts.appendNewGroupShares(baseList, newList);

        assertThat(baseList, is(newList));
    }

    @Test
    public void appendNewGroupSharesBaseUniqueShares() throws Exception {
        GroupShare gs1 = createPendingSentGroupShare();
        GroupShare gs2 = createSuccessSentGroupShare();
        GroupShare gs3 = createFailedSentGroupShare();

        List<GroupShare> baseList = new ArrayList<>();
        List<GroupShare> newList  = new ArrayList<>();

        baseList.add(gs1);
        baseList.add(gs3);  // The order of items in the base list is preserved.
        baseList.add(gs2);
        newList.add(gs1);
        newList.add(gs2);

        Alerts.appendNewGroupShares(baseList, newList);

        assertThat(baseList, is(Arrays.asList(gs1, gs3, gs2)));
    }

    @Test
    public void appendNewGroupSharesEmptyBaseSharesList() throws Exception {
        GroupShare gs1 = createPendingSentGroupShare();
        GroupShare gs2 = createSuccessSentGroupShare();

        List<GroupShare> baseList = new ArrayList<>();
        List<GroupShare> newList  = new ArrayList<>();

        newList.add(gs1);
        newList.add(gs2);

        Alerts.appendNewGroupShares(baseList, newList);

        assertThat(baseList, is(newList));
    }

    @Test
    public void appendNewGroupSharesEmptyNewSharesList() throws Exception {
        GroupShare gs1 = createPendingSentGroupShare();
        GroupShare gs2 = createSuccessSentGroupShare();

        List<GroupShare> baseList = new ArrayList<>();
        List<GroupShare> newList  = new ArrayList<>();

        baseList.add(gs1);
        baseList.add(gs2);

        Alerts.appendNewGroupShares(baseList, newList);

        assertThat(baseList, is(Arrays.asList(gs1, gs2)));
    }

    /* Store group shares */

    @Test
    public void validStoreStatically() throws Exception {
        List<GroupShare> groupShareList1 = Arrays.asList(
                createPendingReceivedGroupShare(),
                createSuccessReceivedGroupShare(),
                createPendingSentGroupShare(),
                createSuccessSentGroupShare(),
                createFailedSentGroupShare()
        );

        List<GroupShare> returnedGroupShares = Alerts.storeStatically(groupShareList1);
        assertThat(returnedGroupShares, is(groupShareList1));

        // Assert that no duplicates are added.

        // All of these methods will have different share numbers from the equivalent calls above.
        List<GroupShare> groupShareList2 = Arrays.asList(
                createPendingReceivedGroupShare(),
                createSuccessReceivedGroupShare(),
                createPendingSentGroupShare(),
                createSuccessSentGroupShare(),
                createFailedSentGroupShare()
        );

        returnedGroupShares = Alerts.storeStatically(groupShareList2);

        assertThat(returnedGroupShares, is(groupShareList1));

        for (int i = 0; i < 5; i++) {
            GroupShare storedShare = returnedGroupShares.get(i);
            GroupShare suppliedShare = groupShareList2.get(i);

            assertThat(storedShare.getShareStatus(),                is(suppliedShare.getShareStatus()));
            assertThat(storedShare.getShareTime(),                  is(suppliedShare.getShareTime()));
            assertThat(storedShare.getEncryptedGroupKeyString(),    is(nullValue()));

            assertThat(storedShare.getGroup().getNumber(),          is(suppliedShare.getGroup().getNumber()));
            assertThat(storedShare.getGroup().getName(),            is(suppliedShare.getGroup().getName()));
            assertThat(storedShare.getGroup().getIdBase64(),        is(suppliedShare.getGroup().getIdBase64()));

            assertThat(storedShare.getContact().getNumber(),        is(suppliedShare.getContact().getNumber()));
            assertThat(storedShare.getContact().getId(),            is(suppliedShare.getContact().getId()));
            assertThat(storedShare.getContact().getName(),          is(suppliedShare.getContact().getName()));
            assertThat(storedShare.getContact().getType(),          is(suppliedShare.getContact().getType()));
        }
    }

    @Test
    public void validStoreStaticallySentNotOverwritesReceived() throws Exception {
        GroupShare pendingReceivedGroupShare = createPendingReceivedGroupShare();

        GroupShare returnedGroupShare = Alerts.storeStatically(pendingReceivedGroupShare);
        assertThat(returnedGroupShare, is(pendingReceivedGroupShare));

        // Assert that a sent group-share does not overwrite a received group-share.

        GroupShare pendingSentGroupShare = createPendingSentGroupShare();
        pendingSentGroupShare.getGroup().setNumber(pendingReceivedGroupShare.getGroup().getNumber());

        returnedGroupShare = Alerts.storeStatically(pendingSentGroupShare);
        assertThat(returnedGroupShare, is(pendingSentGroupShare));
        assertThat(Alerts.getGroupShares(), is(Arrays.asList(pendingReceivedGroupShare, pendingSentGroupShare)));
    }

    @Test
    public void validStoreStaticallyReceivedNotOverwritesSent() throws Exception {
        GroupShare failedSentGroupShare = createFailedSentGroupShare();

        GroupShare returnedGroupShare = Alerts.storeStatically(failedSentGroupShare);
        assertThat(returnedGroupShare, is(failedSentGroupShare));

        // Assert that a received group-share does not overwrite a sent group-share.

        GroupShare successReceivedGroupShare = createSuccessReceivedGroupShare();
        successReceivedGroupShare.getGroup().setNumber(failedSentGroupShare.getGroup().getNumber());
        successReceivedGroupShare.getContact().setNumber(failedSentGroupShare.getContact().getNumber());

        returnedGroupShare = Alerts.storeStatically(successReceivedGroupShare);
        assertThat(returnedGroupShare, is(successReceivedGroupShare));
        assertThat(Alerts.getGroupShares(), is(Arrays.asList(failedSentGroupShare, successReceivedGroupShare)));
    }

    @Test
    public void invalidStoreStaticallyNullArgument() {
        expectedEx.expect(NullPointerException.class);

        final GroupShare nullGroupShare = null;
        Alerts.storeStatically(nullGroupShare);
    }

    /* Get actionable alerts count */

    @Test
    public void validGetActionableAlertsCount() throws Exception {
        // Check that a null group-shares list returns a zero-count.
        try {
            Alerts.getGroupShares();
            fail();
        }
        catch (NullStaticVariableException e) {
            assertThat(Alerts.getActionableAlertsCount(), is(0));
        }

        // A group-share with a non-actionable status does not add to the count.
        Alerts.storeStatically(createPendingSentGroupShare());
        assertThat(Alerts.getActionableAlertsCount(), is(0));

        // A group-share with an actionable status adds 1 to the count.
        Alerts.storeStatically(createPendingReceivedGroupShare());
        assertThat(Alerts.getActionableAlertsCount(), is(1));

        // Multiple group-shares with non-actionable statuses do not add to the count.
        Alerts.storeStatically(Arrays.asList(createSuccessSentGroupShare(), createSuccessReceivedGroupShare()));
        assertThat(Alerts.getActionableAlertsCount(), is(1));

        // Adding the same actionable group-share does not add to the count.
        Alerts.storeStatically(createPendingReceivedGroupShare());
        assertThat(Alerts.getActionableAlertsCount(), is(1));

        // Multiple group-shares with actionable statuses adds more than 1 to the count.

        GroupShare gs1 = createPendingReceivedGroupShare();
        gs1.getGroup().setNumber(99951);

        GroupShare gs2 = createPendingReceivedGroupShare();
        gs2.getGroup().setNumber(2674251);

        Alerts.storeStatically(Arrays.asList(gs1, gs2));
        assertThat(Alerts.getActionableAlertsCount(), is(3));

        // Removing multiple actionable group-shares reduces more than 1 from the count.
        Alerts.safeRemoveGroupShares(Arrays.asList(
                new ContactAndGroupNumberPair(gs1.getContact().getNumber(), gs1.getGroup().getNumber()),
                new ContactAndGroupNumberPair(gs2.getContact().getNumber(), gs2.getGroup().getNumber())
        ));
        assertThat(Alerts.getActionableAlertsCount(), is(1));

        // Removing a group-share with a non-actionable status does not remove from the count.
        Alerts.storeStatically(createPendingSentGroupShare());
        assertThat(Alerts.getActionableAlertsCount(), is(1));
    }
}
