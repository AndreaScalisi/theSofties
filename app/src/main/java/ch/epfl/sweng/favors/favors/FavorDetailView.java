package ch.epfl.sweng.favors.favors;

import android.arch.lifecycle.ViewModelProviders;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ch.epfl.sweng.favors.R;
import ch.epfl.sweng.favors.authentication.Authentication;
import ch.epfl.sweng.favors.chat.ChatsList;
import ch.epfl.sweng.favors.database.ChatInformations;
import ch.epfl.sweng.favors.database.ChatRequest;
import ch.epfl.sweng.favors.database.Database;
import ch.epfl.sweng.favors.database.DatabaseEntity;
import ch.epfl.sweng.favors.database.Favor;
import ch.epfl.sweng.favors.database.NotificationEntity;
import ch.epfl.sweng.favors.database.ObservableArrayList;
import ch.epfl.sweng.favors.database.User;
import ch.epfl.sweng.favors.database.UserRequest;
import ch.epfl.sweng.favors.database.storage.FirebaseStorageDispatcher;
import ch.epfl.sweng.favors.database.storage.StorageCategories;
import ch.epfl.sweng.favors.databinding.FragmentFavorDetailViewBinding;
import ch.epfl.sweng.favors.location.LocationHandler;
import ch.epfl.sweng.favors.notifications.Notification;
import ch.epfl.sweng.favors.notifications.NotificationType;
import ch.epfl.sweng.favors.utils.email.Email;
import ch.epfl.sweng.favors.utils.email.EmailUtils;

import static ch.epfl.sweng.favors.utils.Utils.getIconPathFromCategory;


/**
 * FavorDetailView
 * when you click on a Favor in the ListAdapter
 * fragment_favor_detail_view.xml
 */
@SuppressWarnings("unchecked")
public class FavorDetailView extends android.support.v4.app.Fragment {
    private static final String TAG = "FAVOR_DETAIL_FRAGMENT";

    public ObservableField<String> title;
    public ObservableField<String> description;
    public ObservableField<String> location;
    public ObservableField<String> category;
    public ObservableField<Object> geo;
    public ObservableField<String> distance = new ObservableField<>();
    public ObservableField<String> ownerEmail;
    public ObservableField<String> posterName;
    public ObservableField<String> user;

    public ObservableField<Long> tokenPerPers;
    public ObservableField<Long> nbPers;

    public ObservableBoolean isItsOwn = new ObservableBoolean(false);
    public ObservableBoolean buttonsEnabled = new ObservableBoolean(true);
    public ObservableBoolean isInterested = new ObservableBoolean(false);
    public ObservableField<String> pictureRef;

    private Favor localFavor;

    private ArrayList<String> interestedPeople = new ArrayList<>();
    private Map<String, User> userNames;

    FragmentFavorDetailViewBinding binding;

    public static final String FAVOR_ID = "favor_id";
    public static final String ENABLE_BUTTONS = "enable_buttons";
    private ArrayList<String> selectedUsers = new ArrayList<>();
    private Uri imageToDisplay = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userNames = new HashMap<>();

        Bundle arguments = getArguments();
        SharedViewFavor model = ViewModelProviders.of(getActivity()).get(SharedViewFavor.class);
        if (arguments != null && getArguments().containsKey(ENABLE_BUTTONS)) {
            buttonsEnabled.set(arguments.getBoolean(ENABLE_BUTTONS));
        }
        if (arguments != null && getArguments().containsKey(FAVOR_ID)) {
            setFields(new Favor(arguments.getString(FAVOR_ID)));
        } else {
            model.getFavor().observe(this, newFavor -> {
                setFields(newFavor);
                //TODO add token cost binding with new database implementation
            });
        }
      
        if(arguments != null && getArguments().containsKey("uri")){
            imageToDisplay = Uri.parse(arguments.getCharSequence("uri").toString());
        }
    }

    public void setFields(Favor favor) {
        localFavor = favor;
        title = favor.getObservableObject(Favor.StringFields.title);
        description = favor.getObservableObject(Favor.StringFields.description);
        category = favor.getObservableObject(Favor.StringFields.category);
        location = favor.getObservableObject(Favor.StringFields.locationCity);
        geo = favor.getObservableObject(Favor.ObjectFields.location);
        ownerEmail = favor.getObservableObject(Favor.StringFields.ownerEmail);
        distance.set(LocationHandler.distanceBetween((GeoPoint) geo.get()));
        tokenPerPers = favor.getObservableObject(Favor.LongFields.tokenPerPerson);
        nbPers = favor.getObservableObject(Favor.LongFields.nbPerson);
        isItsOwn.set(favor.get(Favor.StringFields.ownerID).equals(User.getMain().getId()));
        pictureRef = favor.getObservableObject(Favor.StringFields.pictureReference);

        if(imageToDisplay==null){
            FirebaseStorageDispatcher.getInstance().displayImage(pictureRef, binding.imageView, StorageCategories.FAVOR);
        }

        updateInterestedUsersNames(favor);

        User favorCreationUser = new User();
        UserRequest.getWithId(favorCreationUser, favor.get(Favor.StringFields.ownerID));
        posterName = favorCreationUser.getObservableObject(User.StringFields.firstName);
    }

    Boolean buttonEnabled = true;

    Observable.OnPropertyChangedCallback userInfosCb = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            if(propertyId == DatabaseEntity.UpdateType.FROM_REQUEST.ordinal()) {
                userNames.put(((User)sender).getId(),((User)sender));
                sender.removeOnPropertyChangedCallback(this);
            }
        }
    };

    private void updateInterestedUsersNames(Favor favor){
        if (favor.get(Favor.ObjectFields.interested) != null && favor.get(Favor.ObjectFields.interested) instanceof ArrayList) {
            interestedPeople = (ArrayList<String>) favor.get(Favor.ObjectFields.interested);
            userNames = new HashMap<>();
            //TODO: favor.getRef(Favor.ObjectFields.interested) and set a listener;
            if(isItsOwn.get()) {
                for (String uid : interestedPeople) {
                    User u = new User();
                    u.addOnPropertyChangedCallback(userInfosCb);
                    UserRequest.getWithId(u, uid);
                }
            } else if (interestedPeople.contains(User.getMain().getId()))
                isInterested.set(true);
        }

    }

    private void sendMessage(String uid, String message) {
        ObservableArrayList<ChatInformations> conversations = new ObservableArrayList<>();
        ChatRequest.allChatsOf(conversations, uid);
        conversations.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (propertyId != ObservableArrayList.ContentChangeType.Update.ordinal()) return;
                for (ChatInformations chat : conversations) {
                    ArrayList<String> participantsId = (ArrayList<String>) chat.get(ChatInformations.ObjectFields.participants);
                    if (participantsId.contains(Authentication.getInstance().getUid()) && participantsId.size() == 2) {
                        chat.addMessageToConversation(message);
                        return;
                    }
                }
                ChatsList.createChat(localFavor.get(Favor.StringFields.title), new String[]{uid, Authentication.getInstance().getUid()}, message);
            }
        });


    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_favor_detail_view, container, false);
        binding.setElements(this);

        binding.favReportAbusiveAdd.setOnClickListener((l) ->
                EmailUtils.sendEmail(
                        new Email(Authentication.getInstance().getEmail(), "report@myfavors.xyz", "Abusive favors : " + title.get(), "The abusive favor is : title" + title.get() + "\ndescription : " + description.get()), getContext(),
                "issue has been reported! Sorry for the inconvenience",
                "Sorry an error occured, try again later..."));

        binding.interestedButton.setOnClickListener((l) -> {
            if (isItsOwn.get()) {
                FavorCreateFragment mFrag = new FavorCreateFragment();
                Bundle bundle = new Bundle();
                bundle.putString(FavorCreateFragment.KEY_FRAGMENT_ID, localFavor.getId());
                mFrag.setArguments(bundle);
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        mFrag).addToBackStack(null).commit();
            } else {
                //return if the timer is not over yet
                if (!buttonEnabled) return;
                //disable the button for preventing non-determinism
                buttonEnabled = false;
                //if user is in the list -> remove him from the list
                Database.getInstance().updateFromDb(localFavor).addOnCompleteListener(updateInterestedUsersArray);



                new Handler().postDelayed(() -> {
                    // This method will be executed once the timer is over
                    buttonEnabled = true;
                }, 5000);
            }
        });


        binding.deleteButton.setOnClickListener((l) -> {
            long newUserTokens = User.getMain().get(User.LongFields.tokens) + 1;
            User.getMain().set(User.LongFields.tokens, newUserTokens);
            Database.getInstance().updateOnDb(User.getMain());
            Database.getInstance().deleteFromDatabase(localFavor);
            FirebaseStorageDispatcher.getInstance().deleteImageFromStorage(pictureRef, StorageCategories.FAVOR);

            Toast.makeText(this.getContext(), "Favor deleted successfully", Toast.LENGTH_LONG).show();
            getActivity().onBackPressed();
        });

        binding.favorPosterDetailViewAccess.setOnClickListener(v -> {
            FavorPosterDetailView mFrag = new FavorPosterDetailView();
            Bundle bundle = new Bundle();
            bundle.putString(FavorPosterDetailView.OWNER_ID, localFavor.get(Favor.StringFields.ownerID));
            mFrag.setArguments(bundle);
            getActivity().getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
                    mFrag).addToBackStack("interested").commit();
        });

        binding.selectButton.setOnClickListener(v -> {

            if (interestedPeople.isEmpty()){
                Toast.makeText(getContext(), "Currently no interested people available.", Toast.LENGTH_LONG).show();
                return;
            }
            else if (interestedPeople.size() != userNames.size()){
                Toast.makeText(getContext(), "Impossible to reach the server.", Toast.LENGTH_LONG).show();
                return;
            }
            UsersSelectionFragment mFrag = new UsersSelectionFragment();

            mFrag.setSelectedUsers(selectedUsers);
            mFrag.setUserNames(new ArrayList<>(userNames.values()));
            mFrag.setMaxToSelect(localFavor.get(Favor.LongFields.nbPerson));

            getActivity().getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mFrag).addToBackStack(null).commit();
        });

        binding.payButton.setOnClickListener(v -> {
            if(selectedUsers.size() == 0){
                Toast.makeText(getContext(), "No user selected.", Toast.LENGTH_SHORT).show();
                return;
            }
            paySelectedPeople(selectedUsers);

        });

        if(imageToDisplay != null){
            binding.imageView.setImageURI(imageToDisplay);
        }


        return binding.getRoot();
    }


    OnCompleteListener updateInterestedUsersArray = new OnCompleteListener() {
        @Override
        public void onComplete(@NonNull Task task) {
            if (interestedPeople.contains(User.getMain().getId())) {
                sendMessage(localFavor.get(Favor.StringFields.ownerID), "Sorry, I'm not anymore interested in your favor : " + localFavor.get(Favor.StringFields.title));
                interestedPeople.remove(User.getMain().getId());
                isInterested.set(false);
            } else {
                // Update interested people list
                interestedPeople.add(User.getMain().getId());
                isInterested.set(true);

                // Send chat message
                sendMessage(localFavor.get(Favor.StringFields.ownerID), "I'm interested in your favor : " + localFavor.get(Favor.StringFields.title));

                // Create email and new notif
                User owner = new User();
                String ownerId = localFavor.get(Favor.StringFields.ownerID);
                UserRequest.getWithId(owner, ownerId);
                owner.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                    @Override
                    public void onPropertyChanged(Observable sender, int propertyId) {
                        if (propertyId == User.UpdateType.FROM_REQUEST.ordinal()) {
                            if (owner.get(User.BooleanFields.emailNotifications)) {
                                EmailUtils.sendEmail(
                                        new Email(Authentication.getInstance().getEmail(),
                                                ownerEmail.get(), "Someone is interested in: " + title.get(),
                                                "Hi ! I am interested to help you with your favor. Please answer directly to this email."),
                                        getContext(),
                                        "We will inform the poster of the add that you are interested to help!",
                                        "Sorry an error occurred, try again later...");
                            }

                            String notification = new Notification(NotificationType.INTEREST, localFavor).toString();
                            NotificationEntity notificationEntity = new NotificationEntity(ownerId);
                            notificationEntity.set(NotificationEntity.StringFields.message,notification);
                            Database.getInstance().updateOnDb(notificationEntity);
                        }
                    }
                });


            }

            if (localFavor != null) {
                localFavor.set(Favor.ObjectFields.interested, interestedPeople);
                Log.d(TAG, "updating local favor on DP");
                Database.getInstance().updateOnDb(localFavor);
            }
        }
    };

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @BindingAdapter("android:src")
    public static void setImageUri(ImageView view, String imageName) {
        if (imageName == null) {
            view.setImageURI(null);
        } else {
            view.setImageURI(Uri.parse(getIconPathFromCategory(imageName)));
        }
    }

    public void paySelectedPeople(ArrayList<String> selectedUsers) {

        long tokenPerPerson = localFavor.get(Favor.LongFields.tokenPerPerson);

        if (selectedUsers.size() == 0) {
            Toast.makeText(getContext(), "No user selected.", Toast.LENGTH_SHORT).show();
        }

        for (String selectedUserId : selectedUsers) {
            User toUpdate = userNames.get(selectedUserId);
            toUpdate.set(User.LongFields.tokens, toUpdate.get(User.LongFields.tokens) + tokenPerPerson);
            Database.getInstance().updateOnDb(toUpdate);
            if(toUpdate.get(User.BooleanFields.emailNotifications) !=null && toUpdate.get(User.BooleanFields.emailNotifications)){
                EmailUtils.sendEmail(
                        new Email(localFavor.get(Favor.StringFields.ownerEmail), toUpdate.get(User.StringFields.email), "You have been paid for the favor " + title.get() + "!", "Thank you for helping me with my favor named :" + title.get() + ". You have been paid for it."), getActivity(),"Users have been successfully paid.","");
            }
            sendMessage(toUpdate.getId(), "You have been paid for : " + localFavor.get(Favor.StringFields.title));
            localFavor.set(Favor.LongFields.nbPerson, localFavor.get(Favor.LongFields.nbPerson)-1);
            localFavor.set(Favor.ObjectFields.interested, interestedPeople.remove(toUpdate.getId()));
        }

        if(localFavor.get(Favor.LongFields.nbPerson) > 0) {
            Database.getInstance().updateOnDb(localFavor);
            Toast.makeText(getContext(), "Users have been successfully paid. Reaming : " + localFavor.get(Favor.LongFields.nbPerson).toString(), Toast.LENGTH_SHORT).show();
            updateInterestedUsersNames(localFavor);
        }
        else{
            Toast.makeText(getContext(), "All users was paid, deleting favor" , Toast.LENGTH_SHORT).show();
            Database.getInstance().deleteFromDatabase(localFavor);
            FirebaseStorageDispatcher.getInstance().deleteImageFromStorage(pictureRef, StorageCategories.FAVOR);
            getActivity().onBackPressed();
        }

    }
}


