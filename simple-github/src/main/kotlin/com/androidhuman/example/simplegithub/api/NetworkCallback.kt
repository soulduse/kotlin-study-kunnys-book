package com.androidhuman.example.simplegithub.api

/**
 * Created by soul on 2018. 1. 7..
 */
class NetworkCallback<T> {
    var preExecute: (()->Unit) ?= null
    var success: ((T) -> Unit) ?= null
    var error: ((Throwable)-> Unit) ?= null
    var postExecute: (()->Unit) ?= null
}