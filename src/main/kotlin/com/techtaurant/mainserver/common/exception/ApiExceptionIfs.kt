package com.techtaurant.mainserver.common.exception

import com.techtaurant.mainserver.common.status.StatusIfs

interface ApiExceptionIfs {
    val status: StatusIfs
    val detail: String
}
