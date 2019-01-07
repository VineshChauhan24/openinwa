

package io.github.subhamtyagi.openinwhatsapp.fragment;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.InputStream;
import java.net.URISyntaxException;

import io.github.subhamtyagi.openinwhatsapp.R;

public class MainFragment extends BaseFragment {

    private String number;

    public MainFragment() {
    }

    @Override
    public void onStart() {
        Intent intent = getActivity().getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            String type = intent.getType();
            if ("text/x-vcard".equals(type)) {
                isShare = true;

                Uri contactUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                ContentResolver cr = getContext().getContentResolver();
                String data = "";
                try {
                    InputStream stream = cr.openInputStream(contactUri);

                    StringBuffer fileContent = new StringBuffer("");
                    int ch;
                    while( (ch = stream.read()) != -1)
                        fileContent.append((char)ch);
                    stream.close();

                    data = new String(fileContent);

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                for (String line : data.split("\n")) {
                    line = line.trim();
                    //todo: support other phone numbers from vcard
                    if (line.startsWith("TEL;CELL:")) {
                        number = line.substring(9);
                        mPhoneEdit.setText(number);
                        mOnPhoneChangedListener.onPhoneChanged(number);
                    }
                }
            }
        }

        super.onStart();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment, container, false);
        initUI(rootView);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initCodes(getActivity());
    }


    @Override
    protected void open() {
        if (setNumber())
            openInWhatsapp();
    }


    @Override
    protected void share() {
        if (setNumber())
            shareLink(getShareMSG());
    }

    private boolean setNumber() {
        hideKeyboard(mPhoneEdit);
        mPhoneEdit.setError(null);
        number = validate();
        if (number == null) {
            mPhoneEdit.requestFocus();
            mPhoneEdit.setError(getString(R.string.label_error_incorrect_phone));
            return false;
        }
        return true;
    }

    private String getNumber(boolean raw) {
        if (this.number.length() == 0) {
            return "";
        }
        return (raw ? "" : "phone=") + this.number.replaceAll("^0+", "");
    }

    private void openInWhatsapp() {
        try {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Conversation"));
            intent.putExtra("jid", getNumber(true) + "@s.whatsapp.net");
            intent.putExtra("displayname", "+" + getNumber(true));
            startActivity(Intent.parseUri("whatsapp://send/?" + getNumber(false), 0));
        } catch (URISyntaxException a) {
            a.printStackTrace();
        }
    }

    private void shareLink(String message) {
        String number = getNumber(false);
        StringBuilder append = new StringBuilder().append("http://api.whatsapp.com/send?").append(number);
        String str = (number.length() == 0 || message.length() == 0) ? "" : "&";
        String url = append.append(str).append(message).toString();
        Intent intent = new Intent("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.TEXT", url);
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, "Send to "));
    }
}
