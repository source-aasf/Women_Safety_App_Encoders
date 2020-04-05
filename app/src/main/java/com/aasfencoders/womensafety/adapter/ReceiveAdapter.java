package com.aasfencoders.womensafety.adapter;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.aasfencoders.womensafety.Class.InviteSentClass;
import com.aasfencoders.womensafety.Class.ReceiveClass;
import com.aasfencoders.womensafety.R;
import com.aasfencoders.womensafety.data.DataContract;
import com.aasfencoders.womensafety.receivedConnection;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ReceiveAdapter extends ArrayAdapter<ReceiveClass> {

    private Context mContext;
    private FirebaseFunctions mFunctions;
    private ReceiveClass currentCall;
    private SharedPreferences sharedPreferences;
    private Button accept;
    private Button reject;
    private TextView status;
    private ArrayList<ReceiveClass> invitelist;
    private int pos;

    public ReceiveAdapter(@NonNull Context context, ArrayList<ReceiveClass> inviteList) {
        super(context, 0, inviteList);
        mContext = context;
        mFunctions = FirebaseFunctions.getInstance();
        invitelist = inviteList;
        sharedPreferences = mContext.getSharedPreferences(mContext.getString(R.string.package_name), Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(mContext).inflate(R.layout.single_receive_contact_item, parent, false);

        }
        accept = listItemView.findViewById(R.id.acceptButton);
        reject = listItemView.findViewById(R.id.rejectButton);
        status = listItemView.findViewById(R.id.person_receive_status);
        showButton();

        currentCall = getItem(position);
        pos = position;

        Log.i("********" , Integer.toString(pos));

        TextView name = listItemView.findViewById(R.id.person_receive_name);
        TextView number = listItemView.findViewById(R.id.person_receive_number);
        name.setText(currentCall.getName());
        number.setText(currentCall.getNumber());

        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                currentCall = getItem(position);
                String name = currentCall.getName();
                String phone = currentCall.getNumber();

                Log.i("#######" , name);
                Log.i("#######" , phone);

                ContentValues values2 = new ContentValues();
                values2.put(DataContract.DataEntry.COLUMN_STATUS_INVITATION, mContext.getString(R.string.matched));
                values2.put(DataContract.DataEntry.COLUMN_STATUS, mContext.getString(R.string.zero));

                String selection = DataContract.DataEntry.COLUMN_PHONE + " =? ";
                String[] selectionArgs = new String[]{phone};

                int rowsAffected = mContext.getContentResolver().update(DataContract.DataEntry.CONTENT_URI, values2, selection, selectionArgs);

                if (rowsAffected == 0) {
                    String nameOfContact = null;
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        nameOfContact = getContactName(getContext(), phone);

                        if (nameOfContact == null) {
                            String code = sharedPreferences.getString(mContext.getString(R.string.ISONUMBER), mContext.getString(R.string.defaultISOCodeNumber));
                            String phonewithCode = phone.replace(code, "");
                            String nameOfContactWithoutCOde = getContactName(getContext(), phonewithCode);

                            if (nameOfContactWithoutCOde != null) {
                                nameOfContact = nameOfContactWithoutCOde;
                            }
                        }
                    }

                    if (nameOfContact == null) {
                        nameOfContact = name;
                    }
                    ContentValues values = new ContentValues();
                    values.put(DataContract.DataEntry.COLUMN_NAME, nameOfContact);
                    values.put(DataContract.DataEntry.COLUMN_PHONE, phone);
                    values.put(DataContract.DataEntry.COLUMN_STATUS, mContext.getString(R.string.zero));
                    values.put(DataContract.DataEntry.COLUMN_STATUS_INVITATION, mContext.getString(R.string.matched));
                    mContext.getContentResolver().insert(DataContract.DataEntry.CONTENT_URI, values);
                }
                callFunction(name, phone, mContext.getString(R.string.accept) , position);

            }
        });

        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentCall = getItem(position);
                String name = currentCall.getName();
                String phone = currentCall.getNumber();
                callFunction(name, phone, mContext.getString(R.string.reject) , position);

            }
        });

        return listItemView;
    }

    private String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }


    private void showButton() {
        accept.setVisibility(View.VISIBLE);
        reject.setVisibility(View.VISIBLE);
        status.setVisibility(View.INVISIBLE);
    }

    private void hideButton(int i , int position) {
        currentCall = getItem(position);

        accept.setVisibility(View.INVISIBLE);
        reject.setVisibility(View.INVISIBLE);
        status.setVisibility(View.VISIBLE);
        switch (i) {
            case 1:
                status.setText("ACCEPTED!!");
                status.setBackgroundColor(Color.argb(255, 0, 230, 0));
                break;
            case 0:
                status.setText("REJECTED!!");
                status.setBackgroundColor(Color.argb(255, 230, 0, 0));
                break;
        }
        callTimer(position);
    }

    private void callTimer(final int position) {
        new CountDownTimer(500, 500) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                deleteFromList(position);
            }
        }.start();
    }

    private void deleteFromList(int position) {
        invitelist.remove(invitelist.get(position));
        notifyDataSetChanged();
    }

    private void callFunction(String source_name, String source_no, final String selection , final int position) {

        String target_no = sharedPreferences.getString(mContext.getString(R.string.userNumber), mContext.getString(R.string.error));
        String target_name = sharedPreferences.getString(mContext.getString(R.string.username), mContext.getString(R.string.error));

        if (source_name.equals(mContext.getString(R.string.error)) || source_no.equals(mContext.getString(R.string.error))) {
            Toast.makeText(mContext, mContext.getString(R.string.errormessage), Toast.LENGTH_SHORT).show();
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("source_no", source_no);
            data.put("target_no", target_no);
            data.put("selection", selection);
            data.put("source_name", source_name);
            data.put("target_name", target_name);

            final SweetAlertDialog pDialog = new SweetAlertDialog(getContext(), SweetAlertDialog.PROGRESS_TYPE);
            pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            switch (selection) {
                case "accept":
                    pDialog.setTitleText("Accepting request...");
                    break;
                case "reject":
                    pDialog.setTitleText("Rejecting request...");
                    break;
            }
            pDialog.setCancelable(false);
            pDialog.show();

            mFunctions
                    .getHttpsCallable("sent_status")
                    .call(data)
                    .continueWith(new Continuation<HttpsCallableResult, String>() {
                        @Override
                        public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                            pDialog.dismissWithAnimation();
                            String result = (String) task.getResult().getData();
                            if (result != null) {
                                Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
                            }
                            callTimer(position);
                            return result;
                        }
                    });
        }

    }
}