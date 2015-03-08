package net.ugona.plus;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class Font {

    private static final Map<String, Typeface> FONTS = new HashMap<>();

    private String name;

    public Font(String name) {
        this.name = name;
    }

    public void install(Activity activity) {
        LayoutInflater layoutInflater = activity.getLayoutInflater();
        boolean factoryIsEmpty = layoutInflater.getFactory() == null;
        if (!factoryIsEmpty) {
            throw new IllegalStateException("Impossible to use this method for this activity: layout factory is set!");
        }
        layoutInflater.setFactory(new FontLayoutInflaterFactory());
    }

    private Typeface getFont(int type, boolean sans, Context context) {
        switch (type) {
            case Typeface.NORMAL:
                return getFont(context, sans ? "Light" : "Regular");
            case Typeface.BOLD:
                return getFont(context, sans ? "Medium" : "Bold");
            default: {
                throw new IllegalArgumentException("Undefined font type " + type);
            }
        }
    }

    private Typeface getFontForAttrs(Context context, AttributeSet attrs) {
        int[] fontStyleAttributes = {android.R.attr.textStyle};
        TypedArray typedArray = context.obtainStyledAttributes(attrs, fontStyleAttributes);
        boolean isStyleSpecified = typedArray.getIndexCount() != 0;
        int type = isStyleSpecified ? typedArray.getInt(0, Typeface.NORMAL) : Typeface.NORMAL;
        typedArray.recycle();

        boolean sans = false;
        int[] fontFaceAttributes = {android.R.attr.typeface};
        typedArray = context.obtainStyledAttributes(attrs, fontFaceAttributes);
        isStyleSpecified = typedArray.getIndexCount() != 0;
        if (isStyleSpecified) {
            if (typedArray.getInt(0, 0) == 1)
                sans = true;
        }
        typedArray.recycle();
        return getFont(type, sans, context);
    }

    private Typeface getFont(Context context, String path) {
        path = this.name + "-" + path;
        if (FONTS.containsKey(path)) {
            return FONTS.get(path);
        } else {
            Typeface typeface = makeTypeface(context, path);
            FONTS.put(path, typeface);
            return typeface;
        }
    }

    private Typeface makeTypeface(Context context, String path) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/" + path + ".ttf");
        } catch (Exception e) {
            // add user-friendly error message
            throw new IllegalArgumentException(String.format("Error creating font from assets path '%s'", path), e);
        }
    }

    final class FontLayoutInflaterFactory implements LayoutInflater.Factory {

        final static String LOG_TAG = "fonts";

        private final String[] ANDROID_UI_COMPONENT_PACKAGES = {
                "android.widget.",
                "android.webkit.",
                "android.view."
        };

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            try {
                // we install custom LayoutInflater.Factory, so FragmentActivity have no chance set own factory and
                // inflate tag <fragment> in method onCreateView. So  call it explicitly.
                if ("fragment".equals(name) && context instanceof FragmentActivity) {
                    FragmentActivity fragmentActivity = (FragmentActivity) context;
                    return fragmentActivity.onCreateView(name, context, attrs);
                }

                LayoutInflater layoutInflater = LayoutInflater.from(context);


                View view = createView(name, attrs, layoutInflater);

                if (view == null) {
                    // It's strange! The view is not ours neither android's. May be the package of this view
                    // is not listed in ANDROID_UI_COMPONENT_PACKAGES. Return null for the default behavior.
                    //Log.d(LOG_TAG, "Cannot create view with name: " + name);
                    return null;
                }

                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTypeface(getFontForAttrs(context, attrs));
                }
                if (view instanceof Button) {
                    Button button = (Button) view;
                    button.setTypeface(getFontForAttrs(context, attrs));
                }

                return view;
            } catch (InflateException e) {
//                Log.e(LOG_TAG, "Error inflating view", e);
                return null;
            } catch (ClassNotFoundException e) {
                Log.e(LOG_TAG, "Error inflating view", e);
                return null;
            }
        }

        private View createView(String name, AttributeSet attrs, LayoutInflater layoutInflater) throws ClassNotFoundException {
            View view = null;
            boolean isAndroidComponent = name.indexOf('.') == -1;
            if (isAndroidComponent) {
                // We don't know package name of the view with the given simple name. Try android ui packages listed in
                // ANDROID_UI_COMPONENT_PACKAGES

                // The same implementation is in the class PhoneLayoutInflater from internal API
                for (String androidPackage : ANDROID_UI_COMPONENT_PACKAGES) {
                    try {
                        view = layoutInflater.createView(name, androidPackage, attrs);
                        if (view != null) {
                            break;
                        }
                    } catch (ClassNotFoundException e) {
                        // Do nothing, we will try another package
                    }
                }
            } else {
                view = layoutInflater.createView(name, null, attrs);
            }
            return view;
        }
    }

}
