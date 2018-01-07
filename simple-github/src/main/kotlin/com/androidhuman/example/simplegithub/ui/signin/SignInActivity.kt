package com.androidhuman.example.simplegithub.ui.signin

import com.androidhuman.example.simplegithub.BuildConfig
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.AuthApi
import com.androidhuman.example.simplegithub.api.model.GithubAccessToken
import com.androidhuman.example.simplegithub.api.provideAuthApi
import com.androidhuman.example.simplegithub.data.AuthTokenProvider
import com.androidhuman.example.simplegithub.ui.main.MainActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.newTask

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignInActivity : AppCompatActivity() {

    internal lateinit var btnStart: Button

    internal lateinit var progress: ProgressBar

    internal val api: AuthApi by lazy { provideAuthApi() }

    internal val authTokenProvider by lazy { AuthTokenProvider(this) }

    internal var accessTokenCall: Call<GithubAccessToken> ?= null

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
        showProgress()

        accessTokenCall = api.getAccessToken(
                BuildConfig.GITHUB_CLIENT_ID, BuildConfig.GITHUB_CLIENT_SECRET, code)

        accessTokenCall!!.enqueue(object : Callback<GithubAccessToken> {
            override fun onResponse(call: Call<GithubAccessToken>,
                                    response: Response<GithubAccessToken>) {
                hideProgress()

                val token = response.body()
                if (response.isSuccessful && null != token) {
                    authTokenProvider.updateToken(token.accessToken)

                    launchMainActivity()
                } else {
                    showError(IllegalStateException(
                            "Not successful: " + response.message()))
                }
            }

            override fun onFailure(call: Call<GithubAccessToken>, t: Throwable) {
                hideProgress()
                showError(t)
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
