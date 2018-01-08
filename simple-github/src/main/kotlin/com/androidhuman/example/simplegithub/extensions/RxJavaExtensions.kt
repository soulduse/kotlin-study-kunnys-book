package com.androidhuman.example.simplegithub.extensions

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Created by soul on 2018. 1. 8..
 */
operator fun CompositeDisposable.plusAssign(disposable: Disposable){

    // CompositeDisposable.add() 함수 호출
    this.add(disposable)
}