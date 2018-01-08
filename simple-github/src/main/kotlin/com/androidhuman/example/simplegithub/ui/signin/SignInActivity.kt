package com.androidhuman.example.simplegithub.ui.signin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import com.androidhuman.example.simplegithub.BuildConfig
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.AuthApi
import com.androidhuman.example.simplegithub.api.provideAuthApi
import com.androidhuman.example.simplegithub.data.AuthTokenProvider
import com.androidhuman.example.simplegithub.extensions.AutoClearedDisposable
import com.androidhuman.example.simplegithub.extensions.plusAssign
import com.androidhuman.example.simplegithub.ui.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.newTask

class SignInActivity : AppCompatActivity() {

    internal lateinit var btnStart: Button

    internal lateinit var progress: ProgressBar

    internal val api: AuthApi by lazy { provideAuthApi() }

    internal val authTokenProvider by lazy { AuthTokenProvider(this) }

    internal val disposables = AutoClearedDisposable(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        lifecycle += disposables

        btnStart = findViewById(R.id.btnActivitySignInStart)
        progress = findViewById(R.id.pbActivitySignIn)

        btnStart.setOnClickListener {
            // https://github.com/login/oauth/authorize?client_id=ee68ae37486527021166
            val authUri = Uri.Builder().scheme("https").authority("github.com")
                    .appendPath("login")
                    .appendPath("oauth")
                    .appendPath("authorize")
                    .appendQueryParameter("client_id", BuildConfig.GITHUB_CLIENT_ID)
                    .build()

            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this@SignInActivity, authUri)
        }

        if (null != authTokenProvider.token) {
            launchMainActivity()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        showProgress()

        val uri = intent.data ?: throw IllegalArgumentException("No data exists")

        val code = uri.getQueryParameter("code") ?: throw IllegalStateException("No code exists")

        getAccessToken(code)
    }

    private fun getAccessToken(code: String) {

        // REST API를 통해 엑세스 토큰을 요청한다.
        disposables += api.getAccessToken(
                BuildConfig.GITHUB_CLIENT_ID, BuildConfig.GITHUB_CLIENT_SECRET, code)
                .map { it.accessToken }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { showProgress() }
                .doOnTerminate { hideProgress() }
                .subscribe({ token ->
                    // API를 통해 엑세스 토큰을 정상적으로 받았을 때 처리할 작업을 구현합니다
                    // 작업 중 오류가 발생하면 이 블록은 호출되지 않습니다.
                    authTokenProvider.updateToken(token)
                    launchMainActivity()
                }) {
                    // error 블록
                    // 네트워크 오류나 데이터 처리 오류 등
                    // 작업이 정상적으로 완료되지 않았을 때 호출됨
                    showError(it)
                }
    }

    private fun showProgress() {
        btnStart.visibility = View.GONE
        progress.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        btnStart.visibility = View.VISIBLE
        progress.visibility = View.GONE
    }

    private fun showError(throwable: Throwable) {
        longToast(throwable.message ?: "No message available")
    }

    private fun launchMainActivity() {
        startActivity(intentFor<MainActivity>().clearTask().newTask())
    }
}
