package ch.epfl.sweng.favors;

import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.google.firebase.auth.FirebaseAuth;

import ch.epfl.sweng.favors.R;
import ch.epfl.sweng.favors.database.User;
import ch.epfl.sweng.favors.database.UserGender;
import ch.epfl.sweng.favors.databinding.FragmentEditProfileBinding;


public class EditProfileFragment extends Fragment {

    private static final String TAG = "EDIT_PROFILE_FRAGMENT";

    public ObservableField<String> firstName = User.getMain().getObservableStringObject(User.StringFields.firstName);
    public ObservableField<String> lastName = User.getMain().getObservableStringObject(User.StringFields.lastName);
    public ObservableField<String> baseCity = User.getMain().getObservableStringObject(User.StringFields.basedLocation);
    public ObservableField<String> sex = User.getMain().getObservableStringObject(User.StringFields.sex);


    FragmentEditProfileBinding binding;

    private TextWatcherCustom profFirstNameEditWatcher = new TextWatcherCustom() {
        @Override
        public void afterTextChanged(Editable editable) {
            User.getMain().set(User.StringFields.firstName, editable.toString());
        }
    };

    private TextWatcherCustom profLastNameEditWatcher = new TextWatcherCustom() {
        @Override
        public void afterTextChanged(Editable editable) {
            User.getMain().set(User.StringFields.lastName, editable.toString());
        }
    };


    private TextWatcherCustom profCityEditWatcher = new TextWatcherCustom() {
        @Override
        public void afterTextChanged(Editable editable) {
            User.getMain().set(User.StringFields.basedLocation, editable.toString());
        }
    };

    private TextWatcherCustom profSexEditWatcher = new TextWatcherCustom() {
        @Override
        public void afterTextChanged(Editable editable) {
            User.getMain().set(User.StringFields.sex, editable.toString());
        }
    };

    private void updateSex(){
        Log.d(TAG, User.getMain().get(User.StringFields.sex));
        UserGender gender = UserGender.getGenderFromString(User.getMain().get(User.StringFields.sex));
        Log.d(TAG,gender.toString());

        switch (gender){
            case F: binding.profSexEdit.check(R.id.profSexFEdit); break;
            case M: binding.profSexEdit.check(R.id.profSexMEdit); break;
            case DEFAULT: Log.e(TAG,"Gender parsing issue.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


         binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_profile,container,false);
         binding.setElements(this);

         updateSex();

         User.getMain().set(User.StringFields.email, FirebaseAuth.getInstance().getCurrentUser().getEmail());

         binding.profFirstNameEdit.addTextChangedListener(profFirstNameEditWatcher);

         binding.profLastNameEdit.addTextChangedListener(profLastNameEditWatcher);

         binding.profCityEdit.addTextChangedListener(profCityEditWatcher);

         binding.profSexEdit.setOnCheckedChangeListener((RadioGroup group, int checkedId) ->{
                    if(checkedId == R.id.profSexMEdit){
                        User.getMain().set(User.StringFields.sex,UserGender.M.toString());
                    }
                    if(checkedId == R.id.profSexFEdit){
                        User.getMain().set(User.StringFields.sex,UserGender.F.toString());
                    }
         });

         binding.commitChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                User.getMain().updateOnDb();
                EditProfileFragment.this.getActivity().getSupportFragmentManager().popBackStack();
            }
        });

         return binding.getRoot();
    }

}
