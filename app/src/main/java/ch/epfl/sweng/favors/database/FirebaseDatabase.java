package ch.epfl.sweng.favors.database;

import android.databinding.Observable;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableField;
import android.databinding.ObservableList;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import ch.epfl.sweng.favors.database.fields.DatabaseField;
import ch.epfl.sweng.favors.database.fields.DatabaseStringField;
import ch.epfl.sweng.favors.main.FavorsMain;

import static ch.epfl.sweng.favors.main.FavorsMain.TAG;

public class FirebaseDatabase extends Database{

    private static FirebaseDatabase db = null;
    private static FirebaseFirestore dbFireStore = null;

    private FirebaseDatabase(){
        dbFireStore = FirebaseFirestore.getInstance();
    }


    /**
     * @return The current FirebaseDatabase or a new one if not yet instantiated
     */
    public static FirebaseDatabase getInstance(){
        if(db == null){
            db = new FirebaseDatabase();
        }

        return db;
    }

    public static void setFirebaseTest(FirebaseFirestore newFireStore) {
        dbFireStore = newFireStore;
    }

    @Override
    public void updateOnDb(DatabaseEntity databaseEntity){
        if(databaseEntity.documentID == null){
            // Do the same here if other types of datas

            dbFireStore.collection(databaseEntity.collection).add(databaseEntity.getEncapsulatedObjectOfMaps())
                    .addOnSuccessListener(docRef -> {
                        databaseEntity.documentID = docRef.getId();
                        updateFromDb(databaseEntity);
                    }).addOnFailureListener(e -> {
                Log.d(TAG,"failure to push favor to database");
                /* Feedback of an error here - Impossible to update user informations*/
            });
        }else {
            dbFireStore.collection(databaseEntity.collection).document(databaseEntity.documentID).set(databaseEntity.getEncapsulatedObjectOfMaps())
                    .addOnSuccessListener(aVoid -> updateFromDb(databaseEntity));
        }
        /* Feedback of an error here - Impossible to update user informations */
    }

    @Override
    public Task updateFromDb(DatabaseEntity databaseEntity){
        if(databaseEntity.documentID == null){return Tasks.forCanceled();}
        return dbFireStore.collection(databaseEntity.collection).document(databaseEntity.documentID)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        databaseEntity.updateLocalData(document.getData());
                    } else {
                        Toast.makeText(FavorsMain.getContext(), "An error occured while requesting " +
                                "data from database",Toast.LENGTH_LONG);
                    }
                });

    }

    class ListRequestFb<T extends DatabaseEntity> implements OnCompleteListener{

        ObservableList<T> list;
        T firstElement;
        Class<T> clazz;

        public  ListRequestFb(ObservableList<T> list, Class<T> clazz){
            super();
            this.list = list;
            this.clazz = clazz;
        }
        public  ListRequestFb(T extratctedFirstElement, Class<T> clazz){
            super();
            this.firstElement = extratctedFirstElement;
            this.clazz = clazz;
        }

        @Override
        public void onComplete(@NonNull Task task) {
            if (task.isSuccessful()) {
                Log.d(TAG, "Request success", task.getException());
                ArrayList<T> tempList = new ArrayList<>();
                if(task.getResult() instanceof DocumentSnapshot){
                    DocumentSnapshot document = (DocumentSnapshot) task.getResult();
                    if(firstElement != null ){
                        firstElement.set(document.getId(), document.getData());
                    }
                }
                else if(task.getResult() instanceof QuerySnapshot){
                    for (QueryDocumentSnapshot document : (QuerySnapshot) task.getResult()) {
                        try {
                            if (firstElement != null) {
                                firstElement.set(document.getId(), document.getData());
                            }
                            if (list != null) {
                                T documentObject = clazz.newInstance();
                                documentObject.set(document.getId(), document.getData());
                                tempList.add(documentObject);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Illegal access exception");
                        }
                    }
                    if (list != null) {
                        list.addAll(tempList);
                    }
                }
            } else {
                Log.d(TAG, "get failed with ", task.getException());
            }
        }
    }

    void addParametersToQuery(Query query, Integer limit, DatabaseStringField orderBy){
        if(orderBy != null){
            query = query.orderBy(orderBy.toString());
        }
        if(limit != null){
            query = query.limit(limit);
        }
    }

    @Override
    protected <T extends DatabaseEntity> ObservableArrayList<T> getAll(Class<T> clazz,
                                                                       String collection,
                                                                       Integer limit,
                                                                       DatabaseStringField orderBy){
        ObservableArrayList<T> list = new ObservableArrayList<>();
        Query query = dbFireStore.collection(collection);
        addParametersToQuery(query, limit, orderBy);
        query.get().addOnCompleteListener(new ListRequestFb<T>(list, clazz));
        return list;
    }


    @Override
    protected  <T extends DatabaseEntity> void updateList(ObservableArrayList<T> list, Class<T> clazz,
                                                                         String collection,
                                                                         DatabaseField element,
                                                                         String value,
                                                                         Integer limit,
                                                                         DatabaseStringField orderBy){
        if(element == null || value == null){return;}
        Query query = dbFireStore.collection(collection).whereEqualTo(element.toString(), value);
        addParametersToQuery(query, limit, orderBy);
        query.get().addOnCompleteListener(new ListRequestFb<T>(list, clazz));
    }


    @Override
    protected  <T extends DatabaseEntity> void updateElement(T toUpdate, Class<T> clazz, String collection,
                                                             String value){
        if(value == null || toUpdate == null){return;}
        DocumentReference query = dbFireStore.collection(collection).document(value);
        query.get().addOnCompleteListener(new ListRequestFb<T>(toUpdate, clazz));

    }


    @Override
    protected  <T extends DatabaseEntity> void updateElement(T toUpdate, Class<T> clazz, String collection,
                                                             DatabaseField element, String value){
        if(value == null || toUpdate == null){return;}
        Query query = dbFireStore.collection(collection).whereEqualTo(element.toString(), value);
        query.get().addOnCompleteListener(new ListRequestFb<T>(toUpdate, clazz));

    }


    public void cleanUp(){
        db = null;
        dbFireStore = null;
    }
}
