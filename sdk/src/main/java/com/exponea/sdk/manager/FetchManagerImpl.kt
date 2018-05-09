package com.exponea.sdk.manager

import com.exponea.sdk.models.CustomerAttributeModel
import com.exponea.sdk.models.CustomerAttributes
import com.exponea.sdk.models.Result
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Response

class FetchManagerImpl(val api: ExponeaService, val gson: Gson) : FetchManager {

    override fun fetchCustomerAttributes(projectToken: String,
                                         attributes: CustomerAttributes,
                                         onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
                                         onFailure: (String) -> Unit) {

        api.postFetchAttributes(projectToken, attributes).enqueue(
                onResponse = {_, response: Response ->
                    val jsonBody = response.body()?.string()
                    val type =  object: TypeToken<Result<List<CustomerAttributeModel>>>(){}.type

                    if (response.code() in 200..203) {
                        val result = gson.fromJson<Result<List<CustomerAttributeModel>>>(jsonBody, type)
                        onSuccess(result)
                    } else {
                        Logger.e(this, "Fetch Failed: ${response.message()}\n" +
                                "Body: $jsonBody")
                        onFailure("Fetch failed: ${response.message()}\n" +
                                "Body: $jsonBody")
                    }
                },
                onFailure = {_, exception ->
                    Logger.e(this, "Fetch failed: exception caught($exception)")
                    onFailure(exception.toString())
                }
        )
    }
}