package ch.epfl.sweng.favors.database;

import android.databinding.ObservableList;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import ch.epfl.sweng.favors.utils.ExecutionMode;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;

public class RequestTest {
    class AnyRequest extends Request {}
    @Test
    public void RequestIsAbstract() {
        assertEquals(AnyRequest.db, Request.db);
    }

    @Before
    public void setup(){
        ExecutionMode.getInstance().setTest(true);
        FakeDatabase.getInstance().createBasicDatabase();
    }

    @Test
    public void userRequests(){
        // Try to get all men users
        Looper.prepare();
        ObservableList<User> someUsersList = UserRequest.getList(User.StringFields.sex, "M",  null, null);
        User user1 = UserRequest.getWithEmail("harvey.dent@gotham.com");
        User user2 = UserRequest.getWithId("U1");
        try {
            sleep(2000);
        } catch (InterruptedException e) {

        }
        assertEquals(user1.get(User.StringFields.firstName), "Harvey");
        assertEquals(user1.get(User.StringFields.lastName), "Dent");
        assertEquals(user1.get(User.StringFields.city), "Arkham Asylum");

        assertEquals(user2.get(User.StringFields.firstName), "Toto");
        assertEquals(user2.get(User.StringFields.lastName),"Lolo");
        assertEquals(user2.get(User.StringFields.city), "Tombouctou");

        assertEquals(someUsersList.size(), 3);
    }

    @Test
    public void favorsRequests(){

    }

    @Test
    public void interestsRequests(){

    }
}