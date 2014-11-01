package com.rengwuxian.materialedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;

/**
 * EditText in Material Design
 * <p/>
 * author:rengwuxian
 * <p/>
 *
 *
 */
public class MaterialEditText extends EditText {
	/**
	 * the spacing between the main text and the inner top padding.
	 */
	private int extraPaddingTop;

	/**
	 * the spacing between the main text and the inner bottom padding.
	 */
	private int extraPaddingBottom;

	/**
	 * the floating label's text size.
	 */
	private final int floatingLabelTextSize;

	/**
	 * the spacing between the main text and the inner components (floating label, bottom ellipsis, characters counter).
	 */
	private final int innerComponentsSpacing;

	/**
	 * whether the floating label should be shown. default is false.
	 */
	private final boolean floatingLabelEnabled;

	/**
	 * whether to highlight the floating label's text color when focused (with the main color). default is true.
	 */
	private final boolean highlightFloatingLabel;

	/**
	 * the base color of the line and the texts. default is black.
	 */
	private int baseColor;

	/**
	 * inner top padding
	 */
	private int innerPaddingTop;

	/**
	 * inner bottom padding
	 */
	private int innerPaddingBottom;

	/**
	 * the underline's highlight color, and the highlight color of the floating label if app:highlightFloatingLabel is set true in the xml. default is black(when app:darkTheme is false) or white(when app:darkTheme is true)
	 */
	private final int primaryColor;

	/**
	 * the color for when something is wrong.(e.g. exceeding max characters)
	 */
	private final int errorColor;

	/**
	 * characters count limit. 0 means no limit. default is 0. NOTE: the character counter will increase the View's height.
	 */
	private final int maxCharacters;

	/**
	 * whether to show the bottom ellipsis in singleLine mode. default is false. NOTE: the bottom ellipsis will increase the View's height.
	 */
	private final boolean bottomEllipsis;

	/**
	 * bottom ellipsis's height
	 */
	private final int bottomEllipsisSize;

	/**
	 * animation fraction of the floating label (0 as totally hidden).
	 */
	private float floatingLabelFraction;

	/**
	 * whether the floating label is being shown.
	 */
	private boolean floatingLabelShown;

	/**
	 * the floating label's focusFraction
	 */
	private float focusFraction;

	private ArgbEvaluator focusEvaluator = new ArgbEvaluator();
	Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	ObjectAnimator labelAnimator;
	ObjectAnimator labelFocusAnimator;
	OnFocusChangeListener interFocusChangeListener;
	OnFocusChangeListener outerFocusChangeListener;

	public MaterialEditText(Context context) {
		this(context, null);
	}

	public MaterialEditText(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MaterialEditText(Context context, AttributeSet attrs, int style) {
		super(context, attrs, style);

		setFocusable(true);
		setFocusableInTouchMode(true);
		setClickable(true);

		floatingLabelTextSize = getResources().getDimensionPixelSize(R.dimen.floating_label_text_size);
		innerComponentsSpacing = getResources().getDimensionPixelSize(R.dimen.inner_components_spacing);
		bottomEllipsisSize = getResources().getDimensionPixelSize(R.dimen.bottom_ellipsis_height);

		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MaterialEditText);
		baseColor = typedArray.getColor(R.styleable.MaterialEditText_baseColor, Color.BLACK);
		setTextColor(baseColor & 0x00ffffff | 0xdf000000);
		setHintTextColor(baseColor & 0x00ffffff | 0x44000000);
		primaryColor = typedArray.getColor(R.styleable.MaterialEditText_primaryColor, baseColor);
		floatingLabelEnabled = typedArray.getBoolean(R.styleable.MaterialEditText_floatLabel, false);
		highlightFloatingLabel = typedArray.getBoolean(R.styleable.MaterialEditText_highlightFloatingLabel, true);
		errorColor = typedArray.getColor(R.styleable.MaterialEditText_errorColor, getResources().getColor(R.color.red_error));
		maxCharacters = typedArray.getInt(R.styleable.MaterialEditText_maxCharacters, 0);
		bottomEllipsis = typedArray.getBoolean(R.styleable.MaterialEditText_singleLineEllipsis, false);
		typedArray.recycle();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			setBackground(null);
		} else {
			setBackgroundDrawable(null);
		}
		initPadding();
		initFloatingLabel();
	}

	private float getFloatingLabelFraction() {
		return floatingLabelFraction;
	}

	private void setFloatingLabelFraction(float floatingLabelFraction) {
		this.floatingLabelFraction = floatingLabelFraction;
		invalidate();
	}

	private float getFocusFraction() {
		return focusFraction;
	}

	private void setFocusFraction(float focusFraction) {
		this.focusFraction = focusFraction;
		invalidate();
	}

	private int getPixel(int dp) {
		return Density.dp2px(getContext(), dp);
	}

	private void initPadding() {
		extraPaddingTop = floatingLabelTextSize + innerComponentsSpacing;
		extraPaddingBottom = maxCharacters > 0 ? innerComponentsSpacing + floatingLabelTextSize : bottomEllipsis ? innerComponentsSpacing * 2 + bottomEllipsisSize : 0;
		extraPaddingBottom += getPixel(8);
		setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
	}

	@Override
	public void setPadding(int left, int top, int right, int bottom) {
		innerPaddingTop = top;
		innerPaddingBottom = bottom;
		super.setPadding(left, top + extraPaddingTop, right, top + extraPaddingBottom);
	}

	/**
	 * get inner top padding, not the real paddingTop
	 */
	public int getInnerPaddingTop() {
		return innerPaddingTop;
	}

	/**
	 * get inner bottom padding, not the real paddingBottom
	 */
	public int getInnerPaddingBottom() {
		return innerPaddingBottom;
	}

	private void initFloatingLabel() {
		if (floatingLabelEnabled) {
			// observe the text changing
			addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					if (s.length() == 0) {
						if (floatingLabelShown) {
							floatingLabelShown = false;
							getLabelAnimator().reverse();
						}
					} else if (!floatingLabelShown) {
						floatingLabelShown = true;
						if (getLabelAnimator().isStarted()) {
							getLabelAnimator().reverse();
						} else {
							getLabelAnimator().start();
						}
					}
				}
			});
			if (highlightFloatingLabel) {
				// observe the focus state to animate the floating label's text color appropriately
				interFocusChangeListener = new OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							if (getLabelFocusAnimator().isStarted())
								getLabelFocusAnimator().reverse();
							else {
								getLabelFocusAnimator().start();
							}
						} else {
							getLabelFocusAnimator().reverse();
						}
						if (outerFocusChangeListener != null) {
							outerFocusChangeListener.onFocusChange(v, hasFocus);
						}
					}
				};
				super.setOnFocusChangeListener(interFocusChangeListener);
			}
		}
	}

	@Override
	public void setOnFocusChangeListener(OnFocusChangeListener listener) {
		if (interFocusChangeListener == null) {
			super.setOnFocusChangeListener(listener);
		} else {
			outerFocusChangeListener = listener;
		}
	}

	private ObjectAnimator getLabelAnimator() {
		if (labelAnimator == null) {
			labelAnimator = ObjectAnimator.ofFloat(this, "floatingLabelFraction", 0f, 1f);
		}
		return labelAnimator;
	}

	private ObjectAnimator getLabelFocusAnimator() {
		if (labelFocusAnimator == null) {
			labelFocusAnimator = ObjectAnimator.ofFloat(this, "focusFraction", 0f, 1f);
		}
		return labelFocusAnimator;
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		// set the textSize
		paint.setTextSize(floatingLabelTextSize);

		// draw the background
		float lineStartY = getHeight() - getPaddingBottom() + getPixel(8);
		if (hasFocus()) {
			if (isExceedingMaxCharacters()) {
				paint.setColor(errorColor);
			} else {
				paint.setColor(primaryColor);
			}
			canvas.drawRect(getScrollX(), lineStartY, getWidth() + getScrollX(), lineStartY + getPixel(2), paint);

			// draw the characters counter
			if (maxCharacters > 0) {
				if (!isExceedingMaxCharacters()) {
					paint.setColor(getCurrentHintTextColor());
				}
				Paint.FontMetrics fontMetrics = paint.getFontMetrics();
				float relativeHeight = - fontMetrics.ascent - fontMetrics.descent;
				String text = getText().length() + " / " + maxCharacters;
				canvas.drawText(text, getWidth() + getScrollX() - paint.measureText(text), lineStartY + innerComponentsSpacing + relativeHeight, paint);
			}
		} else {
			paint.setColor(baseColor);
			canvas.drawRect(getScrollX(), lineStartY, getWidth() + getScrollX(), lineStartY + getPixel(1), paint);
		}

		// draw the floating label
		if (floatingLabelEnabled && !TextUtils.isEmpty(getHint())) {
			// calculate the text color
			paint.setColor((Integer) focusEvaluator.evaluate(focusFraction, getCurrentHintTextColor(), primaryColor));

			// calculate the vertical position
			int start = innerPaddingTop + floatingLabelTextSize + innerComponentsSpacing;
			int distance = innerComponentsSpacing;
			int position = (int) (start - distance * floatingLabelFraction);

			// calculate the alpha
			int alpha = (int) (floatingLabelFraction * 0xff * (0.74f * focusFraction + 0.26f));
			paint.setAlpha(alpha);

			// draw the floating label
			canvas.drawText(getHint().toString(), getPaddingLeft() + getScrollX(), position, paint);
		}

		// draw the bottom ellipsis
		if (hasFocus() && bottomEllipsis && getScrollX() != 0) {
			paint.setColor(primaryColor);
			float startY = lineStartY + innerComponentsSpacing;
			canvas.drawCircle(bottomEllipsisSize / 2 + getScrollX(), startY + bottomEllipsisSize / 2, bottomEllipsisSize / 2, paint);
			canvas.drawCircle(bottomEllipsisSize * 5 / 2 + getScrollX(), startY + bottomEllipsisSize / 2, bottomEllipsisSize / 2, paint);
			canvas.drawCircle(bottomEllipsisSize * 9 / 2 + getScrollX(), startY + bottomEllipsisSize / 2, bottomEllipsisSize / 2, paint);
		}

		// draw the original things
		super.onDraw(canvas);
	}

	public boolean isExceedingMaxCharacters() {
		return maxCharacters > 0 && getText() != null && maxCharacters < getText().length();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (bottomEllipsis && getScrollX() > 0 && event.getAction() == MotionEvent.ACTION_DOWN && event.getX() < getPixel(4 * 5) && event.getY() > getHeight() - extraPaddingBottom - innerPaddingBottom  && event.getY() < getHeight() - innerPaddingBottom) {
			setSelection(0);
			return false;
		}
		return super.onTouchEvent(event);
	}
}