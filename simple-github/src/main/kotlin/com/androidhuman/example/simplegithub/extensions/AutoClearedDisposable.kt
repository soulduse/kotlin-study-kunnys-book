package com.androidhuman.example.simplegithub.extensions

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.support.v7.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Created by soul on 2018. 1. 8..
 */
class AutoClearedDisposable(
        // 생명주기를 참조할 액티비티
        private val lifecycleOwner: AppCompatActivity,

        // onStop() 콜백 함수가 호출되었을 때,
        // 관리하고 있는 디스포저블 객체를 해제할지 여부를 지정합니다
        // 기본값은 true 입니다.
        private val alwaysClearOnStop: Boolean = true,
        private val compositeDisposable: CompositeDisposable = CompositeDisposable())
    : LifecycleObserver {

    // 디스포저블을 추가합니다.
    fun add(disposable: Disposable){
        check(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED))

        compositeDisposable.add(disposable)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun clearUp(){
        if(!alwaysClearOnStop && !lifecycleOwner.isFinishing){
            return
        }

        compositeDisposable.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun detachSelf(){
        // 관리하는 디스포저블을 해제합니다.
        compositeDisposable.clear()

        lifecycleOwner.lifecycle.removeObserver(this)
    }
}