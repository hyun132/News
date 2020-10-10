package com.androiddevs.mvvmnewsapp.utils

sealed class Resource<T> (val data:T? = null, val message:String? =null){
    //success의 경우 받아온 데이터가 있으므로 ? 없어야함
    class Success<T>(data: T):Resource<T>(data)
    class Error<T>(message: String,data: T?=null): Resource<T>(data,message)
    class Loading<T>:Resource<T>()
}