package app.coreme.messenger.features.auth.domain.usecase

import app.coreme.messenger.features.auth.domain.repository.AuthRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LogoutUseCaseTest {

    private lateinit var repository: AuthRepository
    private lateinit var useCase: LogoutUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = LogoutUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository logout`() = runTest {
        coJustRun { repository.logout() }

        useCase()

        coVerify(exactly = 1) { repository.logout() }
    }
}
