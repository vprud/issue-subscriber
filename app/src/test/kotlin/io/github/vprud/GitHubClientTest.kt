package io.github.vprud

import io.mockk.every
import io.mockk.mockk
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitHubClientTest {
    private val mockClient = mockk<OkHttpClient>()
    private val mockCall = mockk<Call>()
    private val mockResponse = mockk<Response>()
    private val mockResponseBody = mockk<ResponseBody>()
    private val githubClient = GitHubClient(mockClient, "test-token")

    private val sampleIssueJson =
        """
        [{
            "url": "https://api.github.com/repos/test/test/issues/1",
            "repository_url": "https://api.github.com/repos/test/test",
            "html_url": "https://github.com/test/test/issues/1",
            "number": 1,
            "title": "Test Issue",
            "labels": [],
            "state": "open",
            "comments": 0,
            "created_at": "2024-03-20T12:00:00Z",
            "updated_at": "2024-03-20T12:00:00Z",
            "body": "Test body"
        }]
        """.trimIndent()

    @Test
    fun `fetchNewIssues returns parsed issues when successful`() {
        setupMockResponse(200, sampleIssueJson)

        val issues = githubClient.fetchNewIssues("test/test", null)

        assertEquals(1, issues.size)
        assertEquals("Test Issue", issues[0].title)
        assertEquals(1, issues[0].number)
    }

    @Test
    fun `fetchLatestIssue returns null when no issues exist`() {
        setupMockResponse(200, "[]")

        val issue = githubClient.fetchLatestIssue("test/test")

        assertNull(issue)
    }

    private fun setupMockResponse(
        code: Int,
        body: String,
    ) {
        every { mockResponseBody.string() } returns body
        every { mockResponse.body } returns mockResponseBody
        every { mockResponse.isSuccessful } returns (code == 200)
        every { mockResponse.code } returns code
        every { mockResponse.message } returns "OK"
        every { mockCall.execute() } returns mockResponse
        every {
            mockClient.newCall(any())
        } returns mockCall
    }
}
