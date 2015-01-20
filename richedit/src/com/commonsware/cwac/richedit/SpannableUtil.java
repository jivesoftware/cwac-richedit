package com.commonsware.cwac.richedit;

import android.text.Spannable;
import android.text.Spanned;

class SpannableUtil {
    public static void setSpan(Spannable str, Object what, int start, int end) {
        str.setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
