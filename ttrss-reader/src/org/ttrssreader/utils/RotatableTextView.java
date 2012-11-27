/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.utils;

import org.ttrssreader.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/*
 * RotatableTextView, (C) 2010, Radu Motisan radu.motisan@gmail.com, modified by Nils Braden
 * 
 * Purpose: create a custom textview control, that supports rotation and other customizations
 */
public class RotatableTextView extends TextView {
    
    private int color = Color.WHITE;
    private int backgroundColor = Color.BLACK;
    private Typeface typeface = null;
    private int size = 14;
    private int angle = 0;
    private int pivotWidth = 0;
    private int pivotHeight = 0;
    private String text = "";
    
    private static final Paint paint = new Paint();
    
    public RotatableTextView(Context context) {
        super(context);
        typeface = Typeface.create("arial", Typeface.NORMAL);
        
        paint.setTypeface(typeface);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(Align.CENTER);
    }
    
    public RotatableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        typeface = Typeface.create("arial", Typeface.NORMAL);
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RotatableTextView);
        text = a.getString(R.styleable.RotatableTextView_text);
        color = a.getColor(R.styleable.RotatableTextView_textColor, Color.WHITE);
        size = a.getInt(R.styleable.RotatableTextView_textSize, 14);
        angle = a.getInt(R.styleable.RotatableTextView_textRotation, 0);
        backgroundColor = a.getColor(R.styleable.RotatableTextView_backgroundColor, Color.BLACK);
        a.recycle();
    }
    
    public RotatableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        typeface = Typeface.create("arial", Typeface.NORMAL);
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RotatableTextView, defStyle, 0);
        text = a.getString(R.styleable.RotatableTextView_text);
        color = a.getColor(R.styleable.RotatableTextView_textColor, Color.WHITE);
        size = a.getInt(R.styleable.RotatableTextView_textSize, 14);
        angle = a.getInt(R.styleable.RotatableTextView_textRotation, 0);
        backgroundColor = a.getColor(R.styleable.RotatableTextView_backgroundColor, Color.BLACK);
        a.recycle();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        pivotWidth = this.getWidth() / 2;
        pivotHeight = this.getHeight() / 2;
        
        canvas.rotate(angle, pivotWidth, pivotHeight);
        canvas.drawColor(backgroundColor);
        canvas.drawText(text, pivotWidth, pivotHeight, paint);
        super.onDraw(canvas);
    }
}
