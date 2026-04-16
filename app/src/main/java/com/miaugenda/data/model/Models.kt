package com.miaugenda.data.model

data class User(
    val id: Int,
    val employeeNumber: String,
    val name: String,
    val email: String,
    val role: String,
    val category: String,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class ShiftType(
    val id: Int,
    val name: String,
    val startTime: String,
    val endTime: String
)

data class Shift(
    val id: Int,
    val userId: Int,
    val shiftTypeId: Int,
    val date: String,
    val user: UserSummary,
    val shiftType: ShiftType
)

data class UserSummary(
    val id: Int,
    val name: String,
    val employeeNumber: String,
    val role: String?
)

data class AttendanceRecord(
    val id: String,
    val userId: String,
    val date: String,
    val checkIn: String,
    val checkOut: String?,
    val createdAt: String
)


data class LoginRequest(
    val employeeNumber: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)
