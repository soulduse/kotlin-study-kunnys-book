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
import com.androidhuman.example.simplegithub.api.NetworkCallback
import com.androidhuman.example.simplegithub.api.NetworkProvider
import com.androidhuman.example.simplegithub.api.model.GithubAccessToken
import com.androidhuman.example.simplegithub.api.provideAuthApi
import com.androidhuman.example.simplegithub.data.AuthTokenProvider
import com.androidhuman.example.simplegithub.ui.main.MainActivity
import kotlinx.coroutines.experimental.Deferred
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.newTask

class SignInActivity : AppCompatActivity() {

    internal lateinit var btnStart: Button

    internal lateinit var progress: ProgressBar

    internal val api: AuthApi by lazy { provideAuthApi() }

    internal val authTokenProvider by lazy { AuthTokenProvider(this) }

    internal var accessTokenCall: Deferred<GithubAccessToken> ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

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
        accessTokenCall = api.getAccessToken(
                BuildConfig.GITHUB_CLIENT_ID,
                BuildConfig.GITHUB_CLIENT_SECRET, code
        )

        NetworkProvider.request(accessTokenCall!!,
                NetworkCallback<GithubAccessToken>().apply{
                    preExecute = {
                        showProgress()
                    }

                    success = {
                        authTokenProvider.updateToken(it.accessToken)
                        launchMainActivity()
                    }

                    error = {
                        showError(it)
                    }

                    postExecute = {
                        hideProgress()
                    }
                })
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

    override fun onStop() {
        accessTokenCall?.run { cancel() }
        super.onStop()
    }
}
