package com.techtaurant.mainserver.common.status

interface StatusIfs {
    fun getHttpStatusCode(): Int

    fun getCustomStatusCode(): Int

    fun getDescription(): String
}
