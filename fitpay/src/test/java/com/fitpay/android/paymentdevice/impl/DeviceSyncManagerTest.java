package com.fitpay.android.paymentdevice.impl;

import android.content.SharedPreferences;

import com.fitpay.android.TestActions;
import com.fitpay.android.TestConstants;
import com.fitpay.android.TestUtils;
import com.fitpay.android.api.models.apdu.ApduPackage;
import com.fitpay.android.api.models.card.CreditCard;
import com.fitpay.android.api.models.card.CreditCardInfo;
import com.fitpay.android.api.models.device.Device;
import com.fitpay.android.api.models.user.LoginIdentity;
import com.fitpay.android.api.models.user.UserCreateRequest;
import com.fitpay.android.paymentdevice.DeviceSyncManager;
import com.fitpay.android.paymentdevice.callbacks.DeviceSyncManagerCallback;
import com.fitpay.android.paymentdevice.constants.States;
import com.fitpay.android.paymentdevice.enums.Sync;
import com.fitpay.android.paymentdevice.events.CommitSuccess;
import com.fitpay.android.paymentdevice.impl.mock.MockPaymentDeviceConnector;
import com.fitpay.android.paymentdevice.models.SyncRequest;
import com.fitpay.android.utils.Listener;
import com.fitpay.android.utils.NamedResource;
import com.fitpay.android.utils.NotificationManager;
import com.fitpay.android.utils.TimestampUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import mockit.Mock;
import mockit.MockUp;
import mockit.internal.state.SavePoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by ssteveli on 7/6/17.
 */


public class DeviceSyncManagerTest extends TestActions {

    @ClassRule
    public static NamedResource rule = new NamedResource(DeviceSyncManagerTest.class);

    private DeviceSyncManager syncManager;
    protected MockPaymentDeviceConnector mockPaymentDevice;

    private Device device;

    private SyncCompleteListener listener;
    private CountDownLatch executionLatch;
    private DeviceSyncManagerCallback syncManagerCallback;

    private String lastCommitId = null;

    @Before
    @Override
    public void before() throws Exception {
        final SharedPreferences mockPrefs = Mockito.mock(SharedPreferences.class);
        final SharedPreferences.Editor mockEditor = Mockito.mock(SharedPreferences.Editor.class);

        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);

        when(mockPrefs.getAll()).thenReturn(Collections.emptyMap());
        when(mockPrefs.getString(ArgumentMatchers.eq("lastCommitId"), ArgumentMatchers.isNull())).then(invocation -> lastCommitId);
        when(mockEditor.putString(ArgumentMatchers.eq("lastCommitId"), ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            lastCommitId = (String) invocation.getArguments()[1];
            return mockEditor;
        });

        when(mockEditor.commit()).thenReturn(true);

        syncManager = DeviceSyncManager.getInstance();
        syncManager.subscribe();

        syncManagerCallback = new DeviceSyncManagerCallback() {
            @Override
            public void syncRequestAdded(SyncRequest request) {
            }

            @Override
            public void syncRequestFailed(SyncRequest request) {
                if (executionLatch != null) {
                    executionLatch.countDown();
                }
            }

            @Override
            public void syncTaskStarting(SyncRequest request) {
            }

            @Override
            public void syncTaskStarted(SyncRequest request) {
            }

            @Override
            public void syncTaskCompleted(SyncRequest request) {
                if (executionLatch != null) {
                    executionLatch.countDown();
                }
            }
        };
        syncManager.registerDeviceSyncManagerCallback(syncManagerCallback);

        mockPaymentDevice = new MockPaymentDeviceConnector(mContext);

        userName = TestUtils.getRandomLengthString(5, 10) + "@"
                + TestUtils.getRandomLengthString(5, 10) + "." + TestUtils.getRandomLengthString(4, 10);
        pin = TestUtils.getRandomLengthNumber(4, 4);

        UserCreateRequest userCreateRequest = getNewTestUser(userName, pin);
        createUser(userCreateRequest);

        assertTrue(doLogin(new LoginIdentity.Builder()
                .setPassword(pin)
                .setUsername(userName)
                .build()));
        this.user = getUser();

        this.device = createDevice(this.user, getTestDevice());
        assertNotNull(this.device);

        Properties props = new Properties();
        props.put(MockPaymentDeviceConnector.CONFIG_CONNECTED_RESPONSE_TIME, "0");
        mockPaymentDevice.init(props);

        assertEquals("payment service is not initialized", States.INITIALIZED, mockPaymentDevice.getState());

        mockPaymentDevice.connect();

        int count = 0;
        while (mockPaymentDevice.getState() != States.CONNECTED || ++count < 5) {
            TestConstants.waitForAction();
        }

        mockPaymentDevice.setUser(user);
        mockPaymentDevice.setDevice(device);

        assertEquals("payment service should be connected", States.CONNECTED, mockPaymentDevice.getState());

        String pan = "9999504454545450";
        CreditCardInfo creditCardInfo = getTestCreditCardInfo(pan);
        CreditCard createdCard = createCreditCard(user, creditCardInfo);
        assertNotNull("card not created", createdCard);

        this.executionLatch = new CountDownLatch(1);
        this.listener = new SyncCompleteListener();
        NotificationManager.getInstance().addListener(this.listener);
    }

    @Override
    @After
    public void after() {
        if (syncManager != null) {
            syncManager.unsubscribe();
            syncManager.removeDeviceSyncManagerCallback(syncManagerCallback);
        }

        NotificationManager.getInstance().removeListener(this.listener);
        super.after();
    }

    @Test
    public void missingUserSyncRequestIsSkipped() throws Exception {
        syncManager.add(SyncRequest.builder()
                .setConnector(mockPaymentDevice)
                .setDevice(device)
                .build());

        executionLatch.await();

        //wait for new event
        TestConstants.waitForAction();

        assertEquals(1, listener.getSyncEvents().stream()
                .filter(syncEvent -> syncEvent.getState() == States.SKIPPED)
                .count());
        assertEquals(0, listener.getCommits().size());
    }

    @Test
    public void missingDeviceSyncRequestIsSkipped() throws Exception {
        syncManager.add(SyncRequest.builder()
                .setConnector(mockPaymentDevice)
                .setUser(user)
                .build());

        executionLatch.await();

        //wait for new event
        TestConstants.waitForAction();

        assertEquals(1, listener.getSyncEvents().stream()
                .filter(syncEvent -> syncEvent.getState() == States.SKIPPED)
                .count());
        assertEquals(0, listener.getCommits().size());
    }

    @Test
    public void missingConnectorSyncRequestIsSkipped() throws Exception {
        syncManager.add(SyncRequest.builder()
                .setUser(user)
                .setDevice(device)
                .build());

        executionLatch.await();

        //wait for new event
        TestConstants.waitForAction();

        assertEquals(1, listener.getSyncEvents().stream()
                .filter(syncEvent -> syncEvent.getState() == States.SKIPPED)
                .count());
        assertEquals(0, listener.getCommits().size());
    }

    @Test
    public void notConnectedDeviceSyncRequestIsSkipped() throws Exception {
        mockPaymentDevice.disconnect();

        while (mockPaymentDevice.getState() != States.DISCONNECTED) {
            TestConstants.waitForAction();
        }

        syncManager.add(SyncRequest.builder()
                .setConnector(mockPaymentDevice)
                .setUser(user)
                .setDevice(device)
                .build());

        executionLatch.await();

        assertEquals(1, listener.getSyncEvents().stream()
                .filter(syncEvent -> syncEvent.getState() == States.SKIPPED)
                .count());
        assertEquals(0, listener.getCommits().size());
    }

    @Test
    public void happyPathSyncTest() throws Exception {
        SavePoint sp = new SavePoint();

        if(!TestConstants.testConfig.useRealTests()){
            mockAPDUValidation();
        }

        int syncCount = 10;

        for (int i = 0; i < syncCount; i++) {
            System.out.println("");
            System.out.println("###############################################################################################################");
            System.out.println("################ sync #" + (i + 1) + " of " + syncCount + " started");
            System.out.println("###############################################################################################################");
            System.out.println("");

            syncManager.add(SyncRequest.builder()
                    .setConnector(mockPaymentDevice)
                    .setUser(user)
                    .setDevice(device)
                    .build());

            executionLatch.await();
            executionLatch = new CountDownLatch(1);

            System.out.println("");
            System.out.println("###############################################################################################################");
            System.out.println("################ sync #" + (i + 1) + " of " + syncCount + " completed");
            System.out.println("###############################################################################################################");
            System.out.println("");

        }

        mockPaymentDevice.disconnect();

        sp.rollback();

        assertEquals(syncCount,
                listener.getSyncEvents().stream()
                        .filter(syncEvent -> syncEvent.getState() == States.COMPLETED_NO_UPDATES || syncEvent.getState() == States.COMPLETED)
                        .count());

        /*
        This test will emit three APDU packages for the newly boarded SE, therefore there should be 3 commits that show up...
        */
        assertEquals(3,
                listener.getCommits().stream()
                        .filter(commit -> commit.getCommitType().equals("APDU_PACKAGE"))
                        .count());
    }

    private void mockAPDUValidation() {
        new MockUp<ApduPackage>() {
            @Mock
            public String getValidUntil() {
                return TimestampUtils.getISO8601StringForTime(System.currentTimeMillis() + 1000 * 60 * 10);
            }
        };
    }

    private class SyncCompleteListener extends Listener {
        private final List<Sync> syncEvents = new ArrayList<>();
        private final List<CommitSuccess> commits = new ArrayList<>();

        private SyncCompleteListener() {
            mCommands.put(Sync.class, data -> onSyncStateChanged((Sync) data));
            mCommands.put(CommitSuccess.class, data -> onCommitSuccess((CommitSuccess) data));
        }

        public void onSyncStateChanged(Sync syncEvent) {
            syncEvents.add(syncEvent);
        }

        public void onCommitSuccess(CommitSuccess commit) {
            commits.add(commit);
        }

        public List<Sync> getSyncEvents() {
            return syncEvents;
        }

        public List<CommitSuccess> getCommits() {
            return commits;
        }
    }
}
