package com.androiddevs.mvvmnewsapp.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.*
import android.net.NetworkCapabilities.*
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androiddevs.mvvmnewsapp.NewsApplication
import com.androiddevs.mvvmnewsapp.models.Article
import com.androiddevs.mvvmnewsapp.models.NewsResponse
import com.androiddevs.mvvmnewsapp.repository.NewsRepository
import com.androiddevs.mvvmnewsapp.utils.Resource
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

//                                                          ↓ ViewModel대신 사용한 이유는 Android ViewModel 안에서 applicationContext를 사용할 수 있음
//                                                          ↓        이를 이용하여 인터넷 연결상태를 가져오기 위해서
class NewsViewModel(app:Application, val newsRepository: NewsRepository):AndroidViewModel(app){
    val breakingNews:MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var breakingNewsPage=1
    var breakingNewsResponse: NewsResponse?=null

    val searchNews:MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var searchNewsPage=1
    var searchNewsResponse: NewsResponse?=null

    init {
        getBreakingNews("us")
    }

    //viewModel이 살아있는 동안 유효한 scope
   fun getBreakingNews(countryCode:String) = viewModelScope.launch {
        safeBreakingNewsCall(countryCode)

    }

    fun searchNews(searchQuery:String) = viewModelScope.launch {
        safeSearchNewsCall(searchQuery)
    }

    private fun handleBreakingNewsResponse(response: Response<NewsResponse>) : Resource<NewsResponse>{
        if (response.isSuccessful){
            response.body()?.let { resultResponse->

                breakingNewsPage++
                //최초의 호출이면(첫번째 페이지 로딩하는 시점이면)
                if(breakingNewsResponse==null){
                    breakingNewsResponse=resultResponse
                }else{
                    val oldArticles = breakingNewsResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }

                return Resource.Success(breakingNewsResponse?:resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun handleSearchNewsResponse(response: Response<NewsResponse>) : Resource<NewsResponse>{
        if (response.isSuccessful){
            response.body()?.let { resultResponse->

                searchNewsPage++
                //최초의 호출이면(첫번째 페이지 로딩하는 시점이면)
                if(searchNewsResponse==null){
                    searchNewsResponse=resultResponse
                }else{
                    val oldArticles = searchNewsResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }

                return Resource.Success(searchNewsResponse?:resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun saveArticle(article: Article) = viewModelScope.launch {
        newsRepository.upsert(article)
    }

    fun getSavedNews() = newsRepository.getSavedNews()

    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }

    private suspend fun safeSearchNewsCall(searchQuery: String){
        breakingNews.postValue(Resource.Loading())
        try {
            //인터넷이 연결되어있으면
            if(hasInternetConnection()){
                val response = newsRepository.searchNews(searchQuery,searchNewsPage)
                searchNews.postValue(handleSearchNewsResponse(response))
            }else{
                //인터넷이 연결되지 않은 경우 에러메시지를 Resource객체에 담아 보내줌(받는쪽에서 Resource형태로 받기 때문)
                searchNews.postValue(Resource.Error("No internet connection"))
            }
            //그 외의 에러 발생할 경우
        }catch (t:Throwable){
            when(t){
                is IOException ->searchNews.postValue(Resource.Error("Network Failure"))
                //IO 문제 아니면 json conversion문제라고 함
                else -> searchNews.postValue(Resource.Error("Conversion Error"))
            }
        }
    }

    private suspend fun safeBreakingNewsCall(countryCode:String){
        breakingNews.postValue(Resource.Loading())
        try {
            //인터넷이 연결되어있으면
            if(hasInternetConnection()){
                val response = newsRepository.getBreakingNews(countryCode,breakingNewsPage)
                breakingNews.postValue(handleBreakingNewsResponse(response))
            }else{
                //인터넷이 연결되지 않은 경우 에러메시지를 Resource객체에 담아 보내줌(받는쪽에서 Resource형태로 받기 때문)
                breakingNews.postValue(Resource.Error("No internet connection"))
            }
            //그 외의 에러 발생할 경우
        }catch (t:Throwable){
            when(t){
                is IOException ->breakingNews.postValue(Resource.Error("Network Failure"))
                //IO 문제 아니면 json conversion문제라고 함
                else -> breakingNews.postValue(Resource.Error("Conversion Error"))
            }
        }
    }

    private fun hasInternetConnection():Boolean{
        //user가 인터넷에 연결되었는지
        val connectivityManager = getApplication<NewsApplication>().getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
            val activityNetwork = connectivityManager.activeNetwork ?:return false
            val capabilities = connectivityManager.getNetworkCapabilities(activityNetwork)?: return false
            return when{
                capabilities.hasTransport(TRANSPORT_WIFI)->true
                capabilities.hasTransport(TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(TRANSPORT_ETHERNET)->true
                else->false
            }
        }else{
            connectivityManager.activeNetworkInfo?.run {
                return when(type){
                    TYPE_WIFI->true
                    TYPE_MOBILE -> true
                    TYPE_ETHERNET-> true
                    else->false
                }
            }
        }
        return false
    }
}