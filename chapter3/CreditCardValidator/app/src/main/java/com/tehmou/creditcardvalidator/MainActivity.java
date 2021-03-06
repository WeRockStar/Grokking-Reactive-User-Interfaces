package com.tehmou.creditcardvalidator;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tehmou.creditcardvalidator.utils.CardType;
import com.tehmou.creditcardvalidator.utils.FocusWatcherObservable;
import com.tehmou.creditcardvalidator.utils.TextWatcherObservable;
import com.tehmou.creditcardvalidator.utils.ValidationUtils;

import java.util.Arrays;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button submitButton = (Button) findViewById(R.id.submit_button);
        submitButton.setOnClickListener((view) -> {
            Log.d(TAG, "Submit");
            findViewById(R.id.container).requestFocus();
        });

        final EditText creditCardNumberView = (EditText) findViewById(R.id.credit_card_number);
        final EditText creditCardCvcView = (EditText) findViewById(R.id.credit_card_cvc);
        final TextView creditCardType = (TextView) findViewById(R.id.credit_card_type);
        final TextView errorText = (TextView) findViewById(R.id.error_text);

        final Observable<Boolean> creditCardNumberHasFocus =
                FocusWatcherObservable.create(creditCardNumberView);

        final Observable<String> creditCardNumber =
                TextWatcherObservable
                        .create(creditCardNumberView)
                        .startWith("");

        final Observable<Boolean> creditCardCvcHasFocus =
                FocusWatcherObservable.create(creditCardCvcView);

        final Observable<String> creditCardCvc =
                TextWatcherObservable
                        .create(creditCardCvcView)
                        .startWith("");

        final Observable<CardType> cardType =
                creditCardNumber
                        .map(CardType::fromNumber);

        final Observable<Boolean> isKnownCardType =
                cardType
                        .map(cardTypeValue -> cardTypeValue != CardType.UNKNOWN);

        final Observable<Boolean> isValidCheckSum =
                creditCardNumber
                        .map(ValidationUtils::checkCardChecksum);

        final Observable<Boolean> isValidNumber =
                Observable.combineLatest(
                        isKnownCardType,
                        isValidCheckSum,
                        (isValidType, isChecksumCorrect) -> isValidType && isChecksumCorrect);

        final Observable<Boolean> creditCardNumberHasHadFocus =
                Observable.concat(
                        Observable.just(false),
                        creditCardNumberHasFocus
                                .filter(value -> value)
                                .first());

        final Observable<Boolean> creditCardCvcHasHadFocus =
                Observable.concat(
                        Observable.just(false),
                        creditCardCvcHasFocus
                                .filter(value -> value)
                                .first());

        final Observable<Boolean> isValidCvc =
                Observable.combineLatest(
                        cardType,
                        creditCardCvc,
                        ValidationUtils::isValidCvc);

        final Observable<Boolean> showErrorForCreditCardNumber =
                Observable.combineLatest(
                        creditCardNumberHasHadFocus,
                        creditCardNumberHasFocus,
                        isValidNumber,
                        (creditCardNumberHasHadFocusValue,
                         creditCardNumberHasFocusValue,
                         isValidNumberValue) ->
                                creditCardNumberHasHadFocusValue &&
                                        (!creditCardNumberHasFocusValue && !isValidNumberValue));

        final Observable<Boolean> showErrorForCvc =
                Observable.combineLatest(
                        creditCardCvcHasHadFocus,
                        creditCardCvcHasFocus,
                        isValidCvc,
                        (creditCardCvcHasHadFocusValue,
                         creditCardCvcHasFocusValue,
                         isValidCvcValue) ->
                                creditCardCvcHasHadFocusValue &&
                                        (!creditCardCvcHasFocusValue && !isValidCvcValue));

        // Do all side-effects
        showErrorForCreditCardNumber
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        value -> creditCardNumberView.setTextColor(
                                value ? Color.RED : Color.BLACK));

        showErrorForCvc
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        value -> creditCardCvcView.setTextColor(
                                value ? Color.RED : Color.BLACK));

        cardType
                .map(Enum::toString)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(creditCardType::setText);

        Observable.combineLatest(
                isValidNumber,
                isValidCvc,
                (isValidNumberValue, isValidCvcValue) -> isValidNumberValue && isValidCvcValue)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(submitButton::setEnabled);

        Observable.combineLatest(
                Arrays.asList(
                        isKnownCardType.map(value -> value ? "" : "Unknown card type"),
                        isValidCheckSum.map(value -> value ? "" : "Invalid checksum"),
                        isValidCvc.map(value -> value ? "" : "Invalid CVC code")),
                (errorStrings) -> {
                    StringBuilder builder = new StringBuilder();
                    for (Object errorString : errorStrings) {
                        if (!"".equals(errorString)) {
                            builder.append(errorString);
                            builder.append("\n");
                        }
                    }
                    return builder.toString();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(errorText::setText);

    }
}
