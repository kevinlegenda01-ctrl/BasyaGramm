package com.basya.gramm.ui;

import android.app.Activity;
import com.basya.gramm.App;
import com.basya.gramm.R;

public class ThemeHelper {
    public static void apply(Activity a) {
        String t = App.theme();
        if ("light".equals(t)) {
            a.setTheme(R.style.Theme_BasyaGramm_Light);
        } else {
            a.setTheme(R.style.Theme_BasyaGramm);
        }
    }
}
