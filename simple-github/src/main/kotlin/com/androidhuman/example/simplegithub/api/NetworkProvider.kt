package com.androidhuman.example.simplegithub.api

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.HttpException

/**
 * Created by soul on 2018. 1. 7..
 */
object NetworkProvider{

    private fun defaultError(t: Throwable) {
        t.printStackTrace()
    }

    fun <T> request(call: Deferred<T>, callback: NetworkCallback<T>) {
        request(
                call,
                callback.preExecute,
                callback.success,
                callback.error,
                callback.postExecute
        )
    }

    private fun <T> request(call: Deferred<T>,
                            onPreExecute: (()->Unit)?,
                            onSuccess: ((T) -> Unit)?,
                            onError: ((Throwable) -> Unit)?,
                            onPostExecute: (()-> Unit)?) {
        launch(UI) {
            onPreExecute?.let {
                onPreExecute()
            }
            try {
                onSuccess?.let {
                    onSuccess(call.await())
                }
            } catch (httpException: HttpException) {
                // a non-2XX response was received
                defaultError(httpException)
            } catch (t: Throwable) {
                // a networking or data conversion error
                onError?.let {
                    onError(t)
                }
            }

            onPostExecute?.let {
                onPostExecute()
            }
        }
    }
}
