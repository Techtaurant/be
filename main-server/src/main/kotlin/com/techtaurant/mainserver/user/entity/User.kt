package com.techtaurant.mainserver.user.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.enums.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.Date

@Entity
@Table(name = "\"user\"")
class User(

    var name: String,

    var email: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(255)")
    var provider: OAuthProvider,

    var identifier: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(255)")
    var role: UserRole,

    @Column(name = "profile_image_url")
    var profileImageUrl: String,
) : EntityBase()
