package com.androidhuman.example.simplegithub.ui.search

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.GithubApi
import com.androidhuman.example.simplegithub.api.NetworkCallback
import com.androidhuman.example.simplegithub.api.NetworkProvider
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.model.RepoSearchResponse
import com.androidhuman.example.simplegithub.api.provideGithubApi
import com.androidhuman.example.simplegithub.ui.repo.RepositoryActivity
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.experimental.Deferred
import org.jetbrains.anko.startActivity

class SearchActivity : AppCompatActivity(), SearchAdapter.ItemClickListener {

    internal lateinit var menuSearch: MenuItem

    internal lateinit var searchView: SearchView

    internal val adapter: SearchAdapter by lazy {
        SearchAdapter().apply { setItemClickListener(this@SearchActivity) }
    }

    internal val api: GithubApi by lazy { provideGithubApi(this) }

    internal var searchCall: Deferred<RepoSearchResponse> ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        with(rvActivitySearchList){
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        menuSearch = menu.findItem(R.id.menu_activity_search_query)

        searchView = (menuSearch.actionView as SearchView).apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    updateTitle(query)
                    hideSoftKeyboard()
                    collapseSearchView()
                    searchRepository(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
        }

        with(menuSearch){
            setOnActionExpandListener(object: MenuItem.OnActionExpandListener{
                override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean = true

                override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                    if(searchView.query.isNullOrEmpty()){
                        finish()
                    }
                    return true
                }
            })

            expandActionView()
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (R.id.menu_activity_search_query == item.itemId) {
            item.expandActionView()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(repository: GithubRepo) {
        startActivity<RepositoryActivity>(
                RepositoryActivity.KEY_USER_LOGIN to repository.owner.login,
                RepositoryActivity.KEY_REPO_NAME to repository.name
        )
    }

    private fun searchRepository(query: String) {
        searchCall = api.searchRepository(query)
        NetworkProvider.request(searchCall!!,
                NetworkCallback<RepoSearchResponse>().apply {
                    preExecute = {
                        clearResults()
                        hideError()
                        showProgress()
                    }

                    success = {
                        with(adapter){
                            setItems(it.items)
                            notifyDataSetChanged()
                        }

                        if(0 == it.totalCount){
                            showError(getString(R.string.no_search_result))
                        }
                    }

                    error = {
                        showError(it.message)
                    }

                    postExecute = {
                        hideProgress()
                    }
                })
    }

    private fun updateTitle(query: String) {
        supportActionBar?.run { subtitle = query }
    }

    private fun hideSoftKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).run {
            hideSoftInputFromWindow(searchView.windowToken, 0)
        }
    }

    private fun collapseSearchView() {
        menuSearch.collapseActionView()
    }

    private fun clearResults() {
        with(adapter){
            clearItems()
            notifyDataSetChanged()
        }
    }

    private fun showProgress() {
        pbActivitySearch.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        pbActivitySearch.visibility = View.GONE
    }

    private fun showError(message: String?) {
        with(tvActivitySearchMessage){
            text = message?: "No message"
            visibility = View.VISIBLE
        }
    }

    private fun hideError() {
        with(tvActivitySearchMessage){
            text = ""
            visibility = View.GONE
        }
    }

    override fun onStop() {
        searchCall?.run { cancel() }
        super.onStop()
    }
}
