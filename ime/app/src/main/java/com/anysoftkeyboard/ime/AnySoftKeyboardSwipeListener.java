/*
 * Copyright (c) 2016 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.ime;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.anysoftkeyboard.LayoutSwitchAnimationListener;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.prefs.AnimationsLevel;
import com.anysoftkeyboard.rx.GenericOnError;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.functions.Consumer;

public abstract class AnySoftKeyboardSwipeListener extends AnySoftKeyboardPopText {

  private int mFirstDownKeyCode;

  private LayoutSwitchAnimationListener mSwitchAnimator;

  private int mSwipeUpKeyCode;
  private int mSwipeUpFromSpaceBarKeyCode;
  private int mSwipeDownKeyCode;
  private int mSwipeLeftKeyCode;
  private int mSwipeRightKeyCode;
  private int mSwipeLeftFromSpaceBarKeyCode;
  private int mSwipeRightFromSpaceBarKeyCode;
  private int mSwipeLeftWithTwoFingersKeyCode;
  private int mSwipeRightWithTwoFingersKeyCode;
  private int mPinchKeyCode;
  private int mSeparateKeyCode;

  private static int getIntFromSwipeConfiguration(final String keyValue) {
    return switch (keyValue) {
      case "next_alphabet" -> KeyCodes.MODE_ALPHABET;
      case "next_symbols" -> KeyCodes.MODE_SYMBOLS;
      case "cycle_keyboards" -> KeyCodes.KEYBOARD_CYCLE;
      case "reverse_cycle_keyboards" -> KeyCodes.KEYBOARD_REVERSE_CYCLE;
      case "shift" -> KeyCodes.SHIFT;
      case "space" -> KeyCodes.SPACE;
      case "hide" -> KeyCodes.CANCEL;
      case "backspace" -> KeyCodes.DELETE;
      case "backword" -> KeyCodes.DELETE_WORD;
      case "clear_input" -> KeyCodes.CLEAR_INPUT;
      case "cursor_up" -> KeyCodes.ARROW_UP;
      case "cursor_down" -> KeyCodes.ARROW_DOWN;
      case "cursor_left" -> KeyCodes.ARROW_LEFT;
      case "cursor_right" -> KeyCodes.ARROW_RIGHT;
      case "next_inside_mode" -> KeyCodes.KEYBOARD_CYCLE_INSIDE_MODE;
      case "switch_keyboard_mode" -> KeyCodes.KEYBOARD_MODE_CHANGE;
      case "split_layout" -> KeyCodes.SPLIT_LAYOUT;
      case "merge_layout" -> KeyCodes.MERGE_LAYOUT;
      case "compact_to_left" -> KeyCodes.COMPACT_LAYOUT_TO_LEFT;
      case "compact_to_right" -> KeyCodes.COMPACT_LAYOUT_TO_RIGHT;
      case "utility_keyboard" -> KeyCodes.UTILITY_KEYBOARD;
      case "quick_text_popup" -> KeyCodes.QUICK_TEXT_POPUP;
      default -> 0; // 0 means no action
    };
  }

  private void subPrefs(
      @StringRes int keyRes, @StringRes int defaultValue, @NonNull Consumer<Integer> consumer) {
    addDisposable(
        prefs()
            .getString(keyRes, defaultValue)
            .asObservable()
            .map(AnySoftKeyboardSwipeListener::getIntFromSwipeConfiguration)
            .subscribe(consumer, GenericOnError.onError("getIntFromSwipeConfiguration")));
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mSwitchAnimator =
        new LayoutSwitchAnimationListener(
            getApplicationContext(), () -> (View) getInputView(), this::doOnKeyForGesture);

    addDisposable(
        AnimationsLevel.createPrefsObservable(this)
            .subscribe(
                animationsLevel ->
                    mSwitchAnimator.setAnimations(animationsLevel == AnimationsLevel.Full),
                GenericOnError.onError("mSwitchAnimator.setAnimations")));

    subPrefs(
        R.string.settings_key_swipe_up_action,
        R.string.swipe_action_value_shift,
        code -> mSwipeUpKeyCode = code);
    subPrefs(
        R.string.settings_key_swipe_up_from_spacebar_action,
        R.string.swipe_action_value_utility_keyboard,
        code -> mSwipeUpFromSpaceBarKeyCode = code);
    subPrefs(
        R.string.settings_key_swipe_down_action,
        R.string.swipe_action_value_hide,
        code -> mSwipeDownKeyCode = code);
    subPrefs(
        R.string.settings_key_swipe_left_action,
        R.string.swipe_action_value_next_symbols,
        code -> mSwipeLeftKeyCode = code);
    subPrefs(
        R.string.settings_key_swipe_right_action,
        R.string.swipe_action_value_next_alphabet,
        code -> mSwipeRightKeyCode = code);
    subPrefs(
        R.string.settings_key_pinch_gesture_action,
        R.string.swipe_action_value_merge_layout,
        code -> mPinchKeyCode = code);
    subPrefs(
        R.string.settings_key_separate_gesture_action,
        R.string.swipe_action_value_split_layout,
        code -> mSeparateKeyCode = code);
    subPrefs(
        R.string.settings_key_swipe_left_space_bar_action,
        R.string.swipe_action_value_next_symbols,
        code -> mSwipeLeftFromSpaceBarKeyCode = code);
    subPrefs(
        R.string.settings_key_swipe_right_space_bar_action,
        R.string.swipe_action_value_next_alphabet,
        code -> mSwipeRightFromSpaceBarKeyCode = code);
    subPrefs(
        R.string.settings_key_swipe_left_two_fingers_action,
        R.string.swipe_action_value_compact_layout_to_left,
        code -> mSwipeLeftWithTwoFingersKeyCode = code);
    subPrefs(
        R.string.settings_key_swipe_right_two_fingers_action,
        R.string.swipe_action_value_compact_layout_to_right,
        code -> mSwipeRightWithTwoFingersKeyCode = code);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mSwitchAnimator.onDestroy();
  }

  private void doOnKeyForGesture(int keyCode) {
    if (keyCode != 0) {
      onKey(keyCode, null, -1, new int[] {keyCode}, false /*not directly pressed the UI key*/);
      setSpaceTimeStamp(keyCode == KeyCodes.SPACE);
    }
  }

  @Override
  public void onSwipeRight(boolean twoFingersGesture) {
    final int keyCode;
    if (mFirstDownKeyCode == KeyCodes.DELETE) {
      keyCode = KeyCodes.DELETE_WORD;
    } else if (mFirstDownKeyCode == KeyCodes.SPACE) {
      keyCode = mSwipeRightFromSpaceBarKeyCode;
    } else if (twoFingersGesture) {
      keyCode = mSwipeRightWithTwoFingersKeyCode;
    } else {
      keyCode = mSwipeRightKeyCode;
    }

    mSwitchAnimator.doSwitchAnimation(
        LayoutSwitchAnimationListener.AnimationType.SwipeRight, keyCode);
  }

  @Override
  public void onSwipeLeft(boolean twoFingersGesture) {
    final int keyCode;
    if (mFirstDownKeyCode == KeyCodes.DELETE) {
      keyCode = KeyCodes.DELETE_WORD;
    } else if (mFirstDownKeyCode == KeyCodes.SPACE) {
      keyCode = mSwipeLeftFromSpaceBarKeyCode;
    } else if (twoFingersGesture) {
      keyCode = mSwipeLeftWithTwoFingersKeyCode;
    } else {
      keyCode = mSwipeLeftKeyCode;
    }

    if (keyCode != 0)
      mSwitchAnimator.doSwitchAnimation(
          LayoutSwitchAnimationListener.AnimationType.SwipeLeft, keyCode);
  }

  @Override
  public void onSwipeDown() {
    doOnKeyForGesture(mSwipeDownKeyCode);
  }

  @Override
  public void onSwipeUp() {
    final int keyCode =
        mFirstDownKeyCode == KeyCodes.SPACE ? mSwipeUpFromSpaceBarKeyCode : mSwipeUpKeyCode;
    doOnKeyForGesture(keyCode);
  }

  @Override
  public void onPinch() {
    doOnKeyForGesture(mPinchKeyCode);
  }

  @Override
  public void onSeparate() {
    doOnKeyForGesture(mSeparateKeyCode);
  }

  @Override
  public void onFirstDownKey(int primaryCode) {
    mFirstDownKeyCode = primaryCode;
  }
}
