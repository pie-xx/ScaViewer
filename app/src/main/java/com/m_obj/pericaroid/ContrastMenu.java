package com.m_obj.pericaroid;

/**
 * Created by kajikawa on 2015/07/13.
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
/**
class VerticalSeekBar extends SeekBar
{

    public VerticalSeekBar(Context context)
    {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(
            int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c)
    {
        //*�V�[�N�o�[��90�x��]���ĕ`�悷��B
        c.rotate(90);
        c.translate(0, -getWidth());

        super.onDraw(c);
    }

    private OnSeekBarChangeListener onChangeListener;
    @Override
    public void setOnSeekBarChangeListener(
            OnSeekBarChangeListener onChangeListener)
    {
        this.onChangeListener = onChangeListener;
    }

    private int lastProgress = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onChangeListener.onStartTrackingTouch(this);
                setPressed(true);
                setSelected(true);
                break;
            case MotionEvent.ACTION_MOVE:
                super.onTouchEvent(event);
                int progress = (int) (getMax() * event.getY() / getHeight());
                //�V�[�N�o�[�̒l��ݒ肷��B

                if(progress < 0) {progress = 0;}
                if(progress > getMax()) {progress = getMax();}

                setProgress(progress);

                if(progress != lastProgress) {
                    lastProgress = progress;
                    onChangeListener.onProgressChanged(this, progress, true);
                }

                onSizeChanged(getWidth(), getHeight() , 0, 0);
                onChangeListener.onProgressChanged(
                        this, (int) (getMax() * event.getY() / getHeight()), true);
                //�V�[�N�o�[�𓮂���
                setPressed(true);
                setSelected(true);
                break;
            case MotionEvent.ACTION_UP:
                onChangeListener.onStopTrackingTouch(this);
                setPressed(false);
                setSelected(false);
                break;
            case MotionEvent.ACTION_CANCEL:
                super.onTouchEvent(event);
                setPressed(false);
                setSelected(false);
                break;
        }
        return true;
    }

    public synchronized void setProgressAndThumb(int progress)
    {
        setProgress(getMax() - (getMax()- progress));
        onSizeChanged(getWidth(), getHeight() , 0, 0);
    }

    public synchronized void setMaximum(int maximum)
    {
        setMax(maximum);
    }

    public synchronized int getMaximum()
    {
        return getMax();
    }
}
**/
public class ContrastMenu {
    Context		    mContext;
    LinearLayout    mBody;

    LinearLayout    mTitleLayout;
    Button			mSaveButton;
    Button			mCanButton;

    SeekBar         mBlightnessBar;
    TextView        mBlightnessText;
    LinearLayout    mBlightnessLayout;

    TextView        mContrastText;
    SeekBar         mContrastBar;
    LinearLayout    mContrastLayout;

    LinearLayout    mBarlLayout;
    LinearLayout    mPanellLayout;

    Button          mResetBtn;

    float mContrast=1.0f;	// 0..10 1is default.
    float mBrightness=0;	// -255..255 0 is default

    public ContrastMenu(Context context, int dwidth){
        mContext = context;
        int lsize = dwidth / 5;

        mBody = new LinearLayout(context);
        mBody.setOrientation(LinearLayout.VERTICAL);
        mBody.setBackgroundColor(Color.argb(128, 0, 0, 0));

// Titlebar
        mTitleLayout =  new LinearLayout(context);
        mTitleLayout.setGravity(Gravity.RIGHT);

//        mSaveButton = new Button(context);
//        mSaveButton.setText("✔");
//        mTitleLayout.addView(mSaveButton);

        mCanButton = new Button(context);
        mCanButton.setBackgroundDrawable(context.getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel));
        mTitleLayout.addView(mCanButton);
        mCanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                erase();
            }
        });

        mBarlLayout = new LinearLayout(context);
        mBarlLayout.setOrientation(LinearLayout.VERTICAL);

// Blightness
        mBlightnessLayout = new LinearLayout(context);
        mBlightnessText = new TextView(context);
        mBlightnessText.setText("Blightness");
        mBlightnessText.setWidth(lsize);
        mBlightnessBar = new SeekBar(context);
        mBlightnessBar.setMax(100);

        mBlightnessLayout.addView(mBlightnessText);
        mBlightnessLayout.addView(mBlightnessBar,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        mBarlLayout.addView(mBlightnessLayout,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

// Contrast
        mContrastLayout = new LinearLayout(context);

        mContrastBar = new SeekBar(context);
        mContrastBar.setMax(100);

        mContrastText = new TextView(context);
        mContrastText.setText("Contrast");
        mContrastText.setWidth(lsize);

        mContrastLayout.addView(mContrastText);
        mContrastLayout.addView(mContrastBar,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        mBarlLayout.addView(mContrastLayout,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        mResetBtn = new Button(context);
        mResetBtn.setBackgroundDrawable(context.getResources().getDrawable(android.R.drawable.ic_menu_revert));
        mResetBtn.setText("  ");

        mPanellLayout = new LinearLayout(context);
        mPanellLayout.addView(mResetBtn);
        mPanellLayout.addView(mBarlLayout,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        mBody.addView(mTitleLayout,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        mBody.addView(mPanellLayout,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));


        mBody.setVisibility(View.INVISIBLE);

    }
    public LinearLayout getLayout(){
        return mBody;
    }

    public void setOnClickListener(View.OnClickListener onclicked){
        mResetBtn.setOnClickListener(onclicked);
    }
    public void setOnContrastBarChangeListener(SeekBar.OnSeekBarChangeListener onclicked){
        mContrastBar.setOnSeekBarChangeListener(onclicked);
    }
    public void setOnBlightnessBarChangeListener(SeekBar.OnSeekBarChangeListener onclicked){
        mBlightnessBar.setOnSeekBarChangeListener(onclicked);
    }

    public void erase(){
        mBody.setVisibility(View.INVISIBLE);
    }

    public void disp(float blightness, float contrast) {
        mBlightnessBar.setProgress(blightness2progress(blightness));
        mContrastBar.setProgress(contrast2progress(contrast));

        mBody.setVisibility(View.VISIBLE);

    }
    // @param brightness -255..255 0 is default
    int blightness2progress(float blightness){
        return (int)(100 * ( blightness + 255 )/512);
    }
    // @param contrast 0..10 1 is default
    int contrast2progress(float contrast){
        return (int)((Math.log10(contrast)+1)*50);
    //    return (int)(Math.pow(10.0, (contrast-1))*50);
    }

    public boolean isAlive(){
        return mBody.isShown();
    }

}
