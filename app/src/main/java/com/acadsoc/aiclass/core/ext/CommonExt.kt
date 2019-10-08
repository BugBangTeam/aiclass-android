package com.agora.smallclass.core.ext

import android.graphics.Outline
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider

/*
    扩展点击事件，参数为方法
 */
fun View.onClick(method: () -> Unit): View {
    setOnClickListener { method() }
    return this
}

/*
    扩展点击事件
 */
fun View.onClick(listener: View.OnClickListener): View {
    setOnClickListener(listener)
    return this
}

/*
    设置圆角
 */
fun View.setRoundRect(): View {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 6f)
            }
        }
        this.clipToOutline = true
    }
    return this
}