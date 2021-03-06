package com.exponea.sdk.manager

import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.Route
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue

class FlushManagerImpl(
        private val configuration: ExponeaConfiguration,
        private val eventRepository: EventRepository,
        private val exponeaService: ExponeaService,
        private val connectionManager: ConnectionManager
) : FlushManager {
    override var onFlushFinishListener: (() -> Unit)? = null
    override var isRunning: Boolean = false

    override fun flushData() {

        if (!connectionManager.isConnectedToInternet()) {
            Logger.d(this, "Internet connection is not available, skipping flushing")
            onFlushFinishListener?.invoke()
            return
        }

        val allEvents = eventRepository.all()
        Logger.d(this, "flushEvents: Count ${allEvents.size}")

        val firstEvent = allEvents.firstOrNull()

        if (firstEvent != null) {
            isRunning = true
            Logger.i(this, "Flushing Event: ${firstEvent.id}")
            trySendingEvent(firstEvent)
        } else {
            isRunning = false
            Logger.i(this, "No events left to flush: ${allEvents.size}")
            onFlushFinishListener?.invoke()
        }
    }

    private fun trySendingEvent(databaseObject: DatabaseStorageObject<ExportedEventType>) {

        when (databaseObject.route) {
            Route.TRACK_EVENTS       -> trackEvent(databaseObject.projectId, databaseObject)
            Route.TRACK_CUSTOMERS    -> trackCustomer(databaseObject.projectId, databaseObject)
            Route.CUSTOMERS_PROPERTY -> trackCustomer(databaseObject.projectId, databaseObject)
            else                     -> {
                Logger.e(this, "Couldn't find properly route")
                return
            }
        }
    }

    private fun trackEvent(
            projectToken: String,
            databaseObject: DatabaseStorageObject<ExportedEventType>
    ) {
        exponeaService
                .postEvent(projectToken, databaseObject.item)
                .enqueue(
                        { _, response ->
                            val responseCode = response.code()
                            Logger.d(this, "Response Code: $responseCode")
                            if (responseCode in 200..299) {
                                onEventSentSuccess(databaseObject)
                            } else {
                                onEventSentFailed(databaseObject)
                            }
                        },
                        { _, ioException ->
                            Logger.e(
                                    this@FlushManagerImpl,
                                    "Sending Event Failed ${databaseObject.id}",
                                    ioException
                            )
                            onEventSentFailed(databaseObject)
                        }
                )
    }

    private fun trackCustomer(
            projectToken: String,
            databaseObject: DatabaseStorageObject<ExportedEventType>
    ) {
        exponeaService
                .postCustomer(projectToken, databaseObject.item)
                .enqueue(
                        { _, response ->
                            Logger.d(this, "Response Code: ${response.code()}")
                            when(response.code()) {
                                in 200..299 -> onEventSentSuccess(databaseObject)
                                in 500..599 -> flushData()
                                else -> onEventSentFailed(databaseObject)
                            }
                        },
                        { _, ioException ->
                            Logger.e(
                                    this@FlushManagerImpl,
                                    "Sending Event Failed ${databaseObject.id}",
                                    ioException
                            )
                            ioException.printStackTrace()
                            onEventSentFailed(databaseObject)
                        }
                )
    }

    private fun onEventSentSuccess(databaseObject: DatabaseStorageObject<ExportedEventType>) {
        Logger.d(this, "onEventSentSuccess: ${databaseObject.id}")

        eventRepository.remove(databaseObject.id)
        // Once done continue and try to flush the rest of events
        flushData()
    }

    private fun onEventSentFailed(databaseObject: DatabaseStorageObject<ExportedEventType>) {
        databaseObject.tries++

        if (databaseObject.tries >= configuration.maxTries) {
            eventRepository.remove(databaseObject.id)
        } else {
            eventRepository.update(databaseObject)
        }

        flushData()
    }
}