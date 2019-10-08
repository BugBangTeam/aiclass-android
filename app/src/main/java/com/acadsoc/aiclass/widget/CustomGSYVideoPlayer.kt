package com.acadsoc.aiclass.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

/**
 * 隐藏播放器部分控件
 */
class CustomGSYVideoPlayer constructor(context: Context, attrs: AttributeSet) :
    StandardGSYVideoPlayer(context, attrs) {

    override fun init(context: Context) {
        super.init(context)
        // 隐藏部分按钮
        setAlphaTo0f(
            mStartButton, mTitleTextView, mBackButton, mFullscreenButton,
            mProgressBar, mCurrentTimeTextView, mTotalTimeTextView, mBottomContainer,
            mTopContainer, mBottomProgressBar, mThumbImageViewLayout, mLockScreen //   mLoadingProgressBar 显示加载中
        )
    }

    private fun setAlphaTo0f(vararg vs: View) {
        for (view in vs) {
            view.alpha = 0.0f
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

}

