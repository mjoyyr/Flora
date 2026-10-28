package com.floralorder.app.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.floralorder.app.R

/**
 * 5-second animated loading screen shown every time the app is opened.
 * Then it hands over to LoginActivity (which skips straight to Home /
 * Admin Dashboard when the user is already logged in).
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION_MS = 5000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val animators = mutableListOf<Animator>()
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<View>(R.id.ivLogo)
        val ring1 = findViewById<View>(R.id.ring1)
        val ring2 = findViewById<View>(R.id.ring2)
        val llTitle = findViewById<LinearLayout>(R.id.llTitle)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)
        val tvLoading = findViewById<TextView>(R.id.tvLoading)
        val progress = findViewById<ProgressBar>(R.id.progress)

        animateLogo(ivLogo)
        animateRing(ring1, 0L)
        animateRing(ring2, 1000L)
        animateTitle(llTitle)
        animateTagline(tvTagline)
        animateProgress(progress)
        animateLoadingText(tvLoading)

        handler.postDelayed({ goToLogin() }, SPLASH_DURATION_MS)
    }

    // Logo pops in with a bounce, then gently "breathes" and sways.
    private fun animateLogo(logo: View) {
        logo.scaleX = 0f
        logo.scaleY = 0f
        logo.alpha = 0f

        val popIn = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, View.SCALE_X, 0f, 1f),
                ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0f, 1f),
                ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(logo, View.ROTATION, -200f, 0f)
            )
            duration = 1100
            interpolator = OvershootInterpolator(1.6f)
        }

        val breathe = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.07f).apply {
                    repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
                },
                ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 1.07f).apply {
                    repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
                },
                ObjectAnimator.ofFloat(logo, View.ROTATION, -4f, 4f).apply {
                    repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
                }
            )
            duration = 1400
            interpolator = AccelerateDecelerateInterpolator()
        }

        popIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!finished) track(breathe).start()
            }
        })
        track(popIn).start()
    }

    // Soft ripple that keeps expanding out of the logo.
    private fun animateRing(ring: View, delay: Long) {
        ring.alpha = 0f
        val loop = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            startDelay = delay + 500
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val f = it.animatedFraction
                val scale = 1f + 0.7f * f
                ring.scaleX = scale
                ring.scaleY = scale
                ring.alpha = 0.9f * (1f - f)
            }
        }
        track(loop).start()
    }

    // "FLORA" appears letter by letter, each one rising and fading in.
    private fun animateTitle(container: LinearLayout) {
        val word = "FLORA"
        container.gravity = Gravity.CENTER
        word.forEachIndexed { index, ch ->
            val tv = TextView(this).apply {
                text = ch.toString()
                textSize = 36f
                setTextColor(getColor(R.color.white))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                letterSpacing = 0.12f
                alpha = 0f
                translationY = 60f
            }
            container.addView(tv)

            val anim = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(tv, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(tv, View.TRANSLATION_Y, 60f, 0f)
                )
                duration = 600
                startDelay = 900L + index * 150L
                interpolator = OvershootInterpolator(2f)
            }
            track(anim).start()
        }
    }

    private fun animateTagline(tagline: View) {
        val anim = ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f).apply {
            duration = 900
            startDelay = 1900
        }
        track(anim).start()
    }

    // Progress bar fills over exactly the splash duration.
    private fun animateProgress(bar: ProgressBar) {
        val anim = ValueAnimator.ofInt(0, bar.max).apply {
            duration = SPLASH_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { bar.progress = it.animatedValue as Int }
        }
        track(anim).start()
    }

    // Cycles through friendly loading messages.
    private fun animateLoadingText(tv: TextView) {
        val messages = listOf(
            "Gathering fresh blooms…",
            "Arranging your bouquets…",
            "Tying the ribbons…",
            "Almost ready…"
        )
        val step = SPLASH_DURATION_MS / messages.size
        messages.forEachIndexed { index, message ->
            handler.postDelayed({
                if (finished) return@postDelayed
                tv.animate().alpha(0f).setDuration(200).withEndAction {
                    tv.text = message
                    tv.animate().alpha(0.95f).setDuration(250).start()
                }.start()
            }, index * step)
        }
    }

    private fun goToLogin() {
        if (finished) return
        finished = true
        startActivity(Intent(this, LoginActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun <T : Animator> track(animator: T): T {
        animators.add(animator)
        return animator
    }

    override fun onDestroy() {
        finished = true
        handler.removeCallbacksAndMessages(null)
        animators.forEach { it.cancel() }
        animators.clear()
        super.onDestroy()
    }
}
