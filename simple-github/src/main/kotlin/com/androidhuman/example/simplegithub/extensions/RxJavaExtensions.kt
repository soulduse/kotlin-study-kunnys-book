package com.androidhuman.example.simplegithub.extensions

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import io.reactivex.disposables.Disposable

/**
 * Created by soul on 2018. 1. 8..
 */
operator fun AutoClearedDisposable.plusAssign(disposable: Disposable){

    // CompositeDisposable.add() 함수 호출
    this.add(disposable)
}

operator fun Lifecycle.plusAssign(disposable: LifecycleObserver){
    addObserver(disposable)
}