package palsofpaulos.soundscape.common;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class LayoutAnimations {
    public static class HeightAnimation extends Animation {
        private View mView;
        private float mToHeight;
        private float mFromHeight;

        public HeightAnimation(View v, float fromHeight, float toHeight, int duration) {
            mToHeight = toHeight;
            mFromHeight = fromHeight;
            mView = v;
            setDuration(duration);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height =
                    (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            ViewGroup.LayoutParams p = mView.getLayoutParams();
            p.height = (int) height;
            mView.requestLayout();
        }
    }

    public static class WidthAnimation extends Animation {
        private View mView;
        private float mToWidth;
        private float mFromWidth;

        public WidthAnimation(View v, float fromWidth, float toWidth, int duration) {
            mToWidth = toWidth;
            mFromWidth = fromWidth;
            mView = v;
            setDuration(duration);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float width =
                    (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            ViewGroup.LayoutParams p = mView.getLayoutParams();
            p.width = (int) width;
            mView.requestLayout();
        }
    }
}
