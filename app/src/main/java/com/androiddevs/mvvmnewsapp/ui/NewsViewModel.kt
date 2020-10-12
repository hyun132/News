package com.androiddevs.mvvmnewsapp.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androiddevs.mvvmnewsapp.models.Article
import com.androiddevs.mvvmnewsapp.models.NewsResponse
import com.androiddevs.mvvmnewsapp.repository.NewsRepository
import com.androiddevs.mvvmnewsapp.utils.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

class NewsViewModel(val newsRepository: NewsRepository):ViewModel(){
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
        //일단 데이터 가져오기 전이니까 초기값을 loading으로 해둠
        breakingNews.postValue(Resource.Loading())
        //suspend로 아래 줄의 작업이 완료되어야 그 아래 줄로 넘어감
        val response = newsRepository.getBreakingNews(countryCode,breakingNewsPage)
        breakingNews.postValue(handleBreakingNewsResponse(response))

    }

    fun searchNews(searchQuery:String) = viewModelScope.launch {
        searchNews.postValue(Resource.Loading())
        val response = newsRepository.searchNews(searchQuery,searchNewsPage)
        searchNews.postValue(handleSearchNewsResponse(response))
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
}