package com.androidhuman.example.simplegithub.ui.repo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.GithubApi
import com.androidhuman.example.simplegithub.api.NetworkCallback
import com.androidhuman.example.simplegithub.api.NetworkProvider
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.provideGithubApi
import com.androidhuman.example.simplegithub.ui.GlideApp
import kotlinx.android.synthetic.main.activity_repository.*
import kotlinx.coroutines.experimental.Deferred
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class RepositoryActivity : AppCompatActivity() {

    internal val api: GithubApi by lazy { provideGithubApi(this) }

    internal var repoCall: Deferred<GithubRepo> ?= null

    internal val dateFormatInResponse = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())

    internal val dateFormatToShow = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository)

        val login = intent.getStringExtra(KEY_USER_LOGIN) ?: throw IllegalArgumentException("No login info exists in extras")
        val repo = intent.getStringExtra(KEY_REPO_NAME) ?: throw IllegalArgumentException("No repo info exists in extras")

        showRepositoryInfo(login, repo)
    }

    private fun showRepositoryInfo(login: String, repoName: String) {
        repoCall = api.getRepository(login, repoName)
        NetworkProvider.request(repoCall!!, NetworkCallback<GithubRepo>().apply {
            preExecute = {
                showProgress()
            }

            success = { repo->
                hideProgress(true)
                GlideApp.with(this@RepositoryActivity)
                        .load(repo.owner.avatarUrl)
                        .into(ivActivityRepositoryProfile)

                tvActivityRepositoryName.text = repo.fullName
                tvActivityRepositoryStars.text = resources
                        .getQuantityString(R.plurals.star, repo.stars, repo.stars)
                if (repo.description.isNullOrEmpty()) {
                    tvActivityRepositoryDescription.setText(R.string.no_description_provided)
                } else {
                    tvActivityRepositoryDescription.text = repo.description
                }
                if (repo.language.isNullOrEmpty()) {
                    tvActivityRepositoryLanguage.setText(R.string.no_language_specified)
                } else {
                    tvActivityRepositoryLanguage.text = repo.language
                }

                try {
                    val lastUpdate = dateFormatInResponse.parse(repo.updatedAt)
                    tvActivityRepositoryLastUpdate.text = dateFormatToShow.format(lastUpdate)
                } catch (e: ParseException) {

                }
            }

            error = {
                hideProgress(false)
                showError(it.message)
            }
        })
    }

    private fun showProgress() {
        llActivityRepositoryContent.visibility = View.GONE
        pbActivityRepository.visibility = View.VISIBLE
    }

    private fun hideProgress(isSucceed: Boolean) {
        llActivityRepositoryContent.visibility = if (isSucceed) View.VISIBLE else View.GONE
        pbActivityRepository.visibility = View.GONE
    }

    private fun showError(message: String?) {
        with(tvActivityRepositoryMessage){
            text = message?:"Error"
            visibility = View.VISIBLE
        }
    }

    override fun onStop() {
        repoCall?.run { cancel() }
        super.onStop()
    }

    companion object {

        const val KEY_USER_LOGIN = "user_login"

        const val KEY_REPO_NAME = "repo_name"
    }
}
