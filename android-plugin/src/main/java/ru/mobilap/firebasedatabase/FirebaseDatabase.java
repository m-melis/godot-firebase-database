package ru.mobilap.firebasedatabase;

import android.app.Activity;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ChildEventListener;

import org.godotengine.godot.Godot;
import org.godotengine.godot.Dictionary;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;

public class FirebaseDatabase extends GodotPlugin {

    final private SignalInfo getValueSignal = new SignalInfo(
            "get_value", String.class, String.class);
    final private SignalInfo childAddedSignal = new SignalInfo(
            "child_added", String.class, String.class);
    final private SignalInfo childChangedSignal = new SignalInfo(
            "child_changed", String.class, String.class);
    final private SignalInfo childMovedSignal = new SignalInfo(
            "child_moved", String.class, String.class);
    final private SignalInfo childRemovedSignal = new SignalInfo(
            "child_removed", String.class, String.class);


    private Godot activity = null;
    private com.google.firebase.database.FirebaseDatabase database = null;
    private DatabaseReference dbref = null;
    //private ValueEventListener rootValueListener = null;
    private ChildEventListener rootChildListener = null;

    public FirebaseDatabase(Godot godot) 
    {
        super(godot);
        activity = godot;
        database = com.google.firebase.database.FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);
        dbref = database.getReference();
    }

    @Override
    public String getPluginName() {
        return "FirebaseDatabase";
    }

    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList(
                             "set_db_root",
                             "set_value",
                             "push_child",
                             "update_children",
                             "remove_value",
                             "get_value");
    }

    @Override
    public Set<SignalInfo> getPluginSignals() {
        return new HashSet<SignalInfo>(
                Arrays.asList(
                        getValueSignal,
                        childAddedSignal,
                        childChangedSignal,
                        childMovedSignal,
                        childRemovedSignal));
    }

    @Override
    public View onMainCreate(Activity activity) {
        return null;
    }

    // Public methods

    public void set_db_root(final String[] path) {
        if(dbref != null && rootChildListener != null) {
            dbref.removeEventListener(rootChildListener);
        }
        dbref = database.getReference();
        dbref = getReferenceForPath(path);
        if(rootChildListener == null) {
            rootChildListener = new ChildEventListener() {
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.w("godot", "FBDB set_db_root:onCancelled", error.toException());
                    }
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                        Log.d("godot", "FBDB set_db_root:onChildAdded - " + snapshot.toString());
                        emitSignal(childAddedSignal.getName(),
                                (Object[]) dataSnapShotToString(snapshot));
                    }
                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                        Log.d("godot", "FBDB set_db_root:onChildChanged - " + snapshot.toString());
                        emitSignal(childChangedSignal.getName(),
                                (Object[]) dataSnapShotToString(snapshot));
                    }
                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                        Log.d("godot", "FBDB set_db_root:onChildMoved - " + snapshot.toString());
                        emitSignal(childMovedSignal.getName(),
                                (Object[]) dataSnapShotToString(snapshot));
                    }
                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {
                        Log.d("godot", "FBDB set_db_root:onChildRemoved - " + snapshot.toString());
                        emitSignal(childRemovedSignal.getName(),
                                (Object[]) dataSnapShotToString(snapshot));
                    }
                };
        }
        dbref.addChildEventListener(rootChildListener);
    }
    
    private DatabaseReference getReferenceForPath(final String[] path) {
        DatabaseReference ref = dbref;
        for (String p : path) {
            ref = ref.child(p);
        }
        Log.d("godot", "FBDB getReferenceForPath - " + ref.toString());
        return ref;
    }

    private String[] dataSnapShotToString(DataSnapshot dataSnapshot){
        return new String[] {
                String.valueOf(dataSnapshot.getKey()),
                String.valueOf(dataSnapshot.getValue()).replaceAll("=",":")
        };
    }

    public void set_value(final String[] path, final Dictionary value) {
        DatabaseReference ref = getReferenceForPath(path);
        ref.setValue(value);
    }

    public String push_child(final String[] path) {
        DatabaseReference ref = getReferenceForPath(path);
        ref = ref.push();
        return ref.getKey();
    }

    public void update_children(final String[] paths, final Dictionary params) {
        Dictionary updates = new Dictionary();
        for(String path: paths) {
            updates.put(path, params);
        }
        dbref.updateChildren(updates);
    }

    public void remove_value(final String[] path) {
        DatabaseReference ref = getReferenceForPath(path);
        ref.removeValue();
    }

    public void get_value(final String[] path) {
        Log.d("godot", "FBDB get_value - " + Arrays.toString(path));
        DatabaseReference ref = getReferenceForPath(path);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d("godot", "FBDB get_value:onDataChange - " + dataSnapshot.toString());
                    emitSignal(getValueSignal.getName(),
                            (Object[]) dataSnapShotToString(dataSnapshot));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting value failed, log a error message
                    Log.w("godot", "FBDB get_value:onCancelled", databaseError.toException());
                }});
    }
}
