package com.splitwise.service

import com.splitwise.domain.User
import com.splitwise.persistence.UserRepository
import org.mindrot.jbcrypt.BCrypt

sealed class RegistrationResult {
    data class Success(val user: User) : RegistrationResult()
    data class Failure(val errors: List<String>) : RegistrationResult()
}

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    object InvalidCredentials : AuthResult()
}

class UserService(private val userRepository: UserRepository) {

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): RegistrationResult {
        val errors = mutableListOf<String>()

        if (username.isBlank()) errors += "Username is required"
        if (email.isBlank()) errors += "Email is required"
        else if (!isValidEmail(email)) errors += "Email is not a valid email address"
        if (password.isBlank()) errors += "Password is required"
        else if (password.length < 8) errors += "Password must be at least 8 characters"
        else if (password.length > 72) errors += "Password must be 72 characters or fewer"
        if (password.isNotBlank() && password != confirmPassword) errors += "Passwords do not match"

        if (errors.isNotEmpty()) return RegistrationResult.Failure(errors)

        if (userRepository.findByUsername(username) != null) errors += "Username already exists"
        if (userRepository.findByEmail(email.lowercase()) != null) errors += "Email already exists"

        if (errors.isNotEmpty()) return RegistrationResult.Failure(errors)

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        val user = userRepository.save(
            username = username,
            email = email.lowercase(),
            passwordHash = passwordHash,
        )
        return RegistrationResult.Success(user)
    }

    fun authenticate(username: String, password: String): AuthResult {
        val credentials = userRepository.findForAuth(username) ?: return AuthResult.InvalidCredentials
        if (!BCrypt.checkpw(password, credentials.passwordHash)) return AuthResult.InvalidCredentials
        return AuthResult.Success(credentials.user)
    }

    private fun isValidEmail(email: String): Boolean =
        Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())
}
