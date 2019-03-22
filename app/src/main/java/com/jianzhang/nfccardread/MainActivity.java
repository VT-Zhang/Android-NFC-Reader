package com.jianzhang.nfccardread;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jianzhang.nfccardread.utils.CardNfcUtils;


public class MainActivity extends AppCompatActivity implements CardNfcAsyncTask.CardNfcInterface {

    private CardNfcAsyncTask mCardNfcAsyncTask;
    private Toolbar mToolbar;
    private LinearLayout mCardReadyContent;
    private TextView mPutCardContent;
    private TextView mCardNumberText;
    private TextView mExpireDateText;
    private ImageView mCardLogoIcon;
    private NfcAdapter mNfcAdapter;
    private AlertDialog mTurnNfcDialog;
    private ProgressDialog mProgressDialog;
    private String mDoNotMoveCardMessage;
    private String mUnknownEmvCardMessage;
    private String mCardWithLockedNfcMessage;
    private String mDonationMessage;
    private boolean mIsScanNow;
    private boolean mIntentFromCreate;
    private CardNfcUtils mCardNfcUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null){
            TextView noNfc = findViewById(android.R.id.candidatesArea);
            noNfc.setVisibility(View.VISIBLE);
        } else {
            mCardNfcUtils = new CardNfcUtils(this);
            mPutCardContent = findViewById(R.id.content_putCard);
            mCardReadyContent = findViewById(R.id.content_cardReady);
            mCardNumberText = findViewById(android.R.id.text1);
            mExpireDateText = findViewById(android.R.id.text2);
            mCardLogoIcon = findViewById(android.R.id.icon);
            createProgressDialog();
            initNfcMessages();
            mIntentFromCreate = true;
            onNewIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIntentFromCreate = false;
        if (mNfcAdapter != null && !mNfcAdapter.isEnabled()){
            showTurnOnNfcDialog();
            mPutCardContent.setVisibility(View.GONE);
        } else if (mNfcAdapter != null){
            if (!mIsScanNow){
                mPutCardContent.setVisibility(View.VISIBLE);
                mCardReadyContent.setVisibility(View.GONE);
            }
            mCardNfcUtils.enableDispatch();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mCardNfcUtils.disableDispatch();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            mCardNfcAsyncTask = new CardNfcAsyncTask.Builder(this, intent, mIntentFromCreate)
                    .build();
        }
    }

    @Override
    public void startNfcReadCard() {
        mIsScanNow = true;
        mProgressDialog.show();
    }

    @Override
    public void cardIsReadyToRead() {
        mPutCardContent.setVisibility(View.GONE);
        mCardReadyContent.setVisibility(View.VISIBLE);
        String cardNumber = mCardNfcAsyncTask.getCardNumber();
        cardNumber = getPrettyCardNumber(cardNumber);
        String expiredDate = mCardNfcAsyncTask.getCardExpireDate();
        String cardType = mCardNfcAsyncTask.getCardType();
        mCardNumberText.setText(cardNumber);
        mExpireDateText.setText(expiredDate);
        parseCardType(cardType);
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        if (!lastFour.equals("4700")) {
            showSnackBarLong(mDonationMessage);
        }
    }

    @Override
    public void doNotMoveCardSoFast() {
        showSnackBar(mDoNotMoveCardMessage);
    }

    @Override
    public void unknownEmvCard() {
        showSnackBar(mUnknownEmvCardMessage);
    }

    @Override
    public void cardWithLockedNfc() {
        showSnackBar(mCardWithLockedNfcMessage);
    }

    @Override
    public void finishNfcReadCard() {
        mProgressDialog.dismiss();
        mCardNfcAsyncTask = null;
        mIsScanNow = false;
    }

    private void createProgressDialog(){
        String title = getString(R.string.ad_progressBar_title);
        String mess = getString(R.string.ad_progressBar_mess);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(mess);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
    }

    private void showSnackBar(String message){
        Snackbar.make(mToolbar, message, Snackbar.LENGTH_LONG).show();
    }

    private void showSnackBarLong(String message){
        Snackbar.make(mToolbar, message, Snackbar.LENGTH_LONG).setDuration(5000).show();
    }

    private void showTurnOnNfcDialog(){
        if (mTurnNfcDialog == null) {
            String title = getString(R.string.ad_nfcTurnOn_title);
            String mess = getString(R.string.ad_nfcTurnOn_message);
            String pos = getString(R.string.ad_nfcTurnOn_pos);
            String neg = getString(R.string.ad_nfcTurnOn_neg);
            mTurnNfcDialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(mess)
                    .setPositiveButton(pos, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Send the user to the settings page and hope they turn it on
                            if (android.os.Build.VERSION.SDK_INT >= 16) {
                                startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                            } else {
                                startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                            }
                        }
                    })
                    .setNegativeButton(neg, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            onBackPressed();
                        }
                    }).create();
        }
        mTurnNfcDialog.show();
    }

    private void initNfcMessages(){
        mDoNotMoveCardMessage = getString(R.string.snack_doNotMoveCard);
        mCardWithLockedNfcMessage = getString(R.string.snack_lockedNfcCard);
        mUnknownEmvCardMessage = getString(R.string.snack_unknownEmv);
        mDonationMessage = getString(R.string.snack_donation);
    }


    private void parseCardType(String cardType){
        if (cardType.equals(CardNfcAsyncTask.CARD_UNKNOWN)){
            Snackbar.make(mToolbar, getString(R.string.snack_unknown_bank_card), Snackbar.LENGTH_LONG);
        } else if (cardType.equals(CardNfcAsyncTask.CARD_VISA)) {
            mCardLogoIcon.setImageResource(R.mipmap.ic_visa_logo);
        } else if (cardType.equals(CardNfcAsyncTask.CARD_NAB_VISA)) {
            mCardLogoIcon.setImageResource(R.mipmap.ic_visa_logo);
        } else if (cardType.equals(CardNfcAsyncTask.CARD_MASTER_CARD)) {
            mCardLogoIcon.setImageResource(R.mipmap.master_logo);
        } else if (cardType.equals(CardNfcAsyncTask.CARD_AMERICAN_EXPRESS)) {
            mCardLogoIcon.setImageResource(R.mipmap.amex_logo);
        }
    }

    private String getPrettyCardNumber(String card){
        String div = " - ";
        String mask = "****";
        return  card.substring(0,4) + div + card.substring(4,8) + div + mask
                + div + card.substring(12,16);
    }

}
