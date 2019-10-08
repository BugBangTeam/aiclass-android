package com.acadsoc.aiclass.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;
import com.acadsoc.aiclass.R;

import java.lang.reflect.Field;

public class RelayoutUtil {

    private float mScaleWidth, mScaleHeight;

    public RelayoutUtil(Context context, int baseWidth, int baseHeight) {
        DisplayMetrics display = context.getResources().getDisplayMetrics();
        mScaleWidth = display.heightPixels * 1.0f / baseWidth;
        mScaleHeight = display.widthPixels * 1.0f / baseHeight;
    }

    public View relayoutViewHierarchy(Context context, int id) {
        View view = View.inflate(context, id, null);
        relayoutViewHierarchy(view);
        return view;
    }

    public View relayoutViewHierarchy(Context context, int id, boolean isPortrait) {
        View view = View.inflate(context, id, null);
        if (isPortrait) {
            relayoutViewHierarchy(view, mScaleWidth, mScaleHeight);
        } else {
            relayoutViewHierarchy(view, mScaleHeight, mScaleWidth);
        }
        return view;
    }

    public View relayoutViewHierarchy(View view) {
        relayoutViewHierarchy(view, mScaleWidth, mScaleHeight);
        return view;
    }

    public float getScale() {
        return mScaleWidth > mScaleHeight
                ? mScaleHeight : mScaleWidth;
    }

    public float getScale(int px) {
        return getScale() * px;
    }

    public float getScaleX(float px) {
        return mScaleWidth * px;
    }

    public float getScaleY(float px) {
        return mScaleHeight * px;
    }

    public void relayoutViewHierarchy(View view, float scale_w, float scale_h) {

        if (view == null) {
            return;
        }

        if (view.getTag() != null &&
                view.getTag().equals(view.getContext().getString(R.string.tag_all))) {
            return;
        }

        scaleView(view, scale_w, scale_h);

        if (view instanceof ViewGroup) {
            View[] children = null;
            try {
                Field field = ViewGroup.class.getDeclaredField("mChildren");
                field.setAccessible(true);
                children = (View[]) field.get(view);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (children != null) {
                for (View child : children) {
                    relayoutViewHierarchy(child, scale_w, scale_h);
                }
            }
        }
    }

    private void scaleView(View view, float scale_w, float scale_h) {

        if (view.getTag() != null &&
                view.getTag().equals(view.getContext().getString(R.string.tag_current))) {
            return;
        }

        if (view instanceof TextView) {
            resetTextSize((TextView) view, scale_w > scale_h ? scale_h : scale_w);
        }

        int pLeft = Tools.f2i(view.getPaddingLeft() * scale_w);
        int pTop = Tools.f2i(view.getPaddingTop() * scale_h);
        int pRight = Tools.f2i(view.getPaddingRight() * scale_w);
        int pBottom = Tools.f2i(view.getPaddingBottom() * scale_h);
        view.setPadding(pLeft, pTop, pRight, pBottom);

        LayoutParams params = view.getLayoutParams();
        scaleLayoutParams(params, scale_w, scale_h);

    }

    public void scaleLayoutParams(LayoutParams params, float scale_w, float scale_h) {
        if (params == null) {
            return;
        }

        boolean isEq = params.width == params.height;

        if (params.width != -1 && params.width != -2) {
            params.width = Tools.f2i(params.width * scale_w);
        }
        if (params.height != -1 && params.height != -2) {
            params.height = Tools.f2i(params.height * (isEq ? scale_w : scale_h));
        }

        if (params instanceof MarginLayoutParams) {
            MarginLayoutParams mParams = (MarginLayoutParams) params;
            mParams.leftMargin = Tools.f2i(mParams.leftMargin * scale_w);
            mParams.rightMargin = Tools.f2i(mParams.rightMargin * scale_w);
            mParams.topMargin = Tools.f2i(mParams.topMargin * scale_h);
            mParams.bottomMargin = Tools.f2i(mParams.bottomMargin * scale_h);
        }
    }

    private void resetTextSize(TextView textView, float scale) {
        float size = textView.getTextSize();
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * scale);
    }

}
