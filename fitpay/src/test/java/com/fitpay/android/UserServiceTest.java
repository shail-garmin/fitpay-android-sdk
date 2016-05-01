package com.fitpay.android;

import com.fitpay.android.api.models.user.User;
import com.fitpay.android.api.models.user.UserCreateRequest;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

/**
 * Created by tgs on 4/27/16.
 */
public class UserServiceTest extends TestActions {

    @Test
    public void canCreateUser() throws Exception {
        userName = TestUtils.getRandomLengthString(5, 10) + "@"
                + TestUtils.getRandomLengthString(5, 10) + "." + TestUtils.getRandomLengthString(4, 10);
        pin = TestUtils.getRandomLengthNumber(4, 4);

        UserCreateRequest user = getNewTestUser(userName, pin);
        User createdUser = createUser(user);
        assertNotNull("user should have been created", createdUser);

    }
}
