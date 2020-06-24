package com.nhn.github

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.time.LocalDate
import kotlin.math.min

/**
 * @author haekyu cho
 */

internal class AggregateTest {
    private val members = mutableListOf<Member>()
    private val domain = "https://github.nhnent.com/"
    private val memberId = "sungchoon-park"

    // 조회시작일
    private val from = 1

    // 조회종료일
    private val to = 1

    // 현재 년도
    private val year = LocalDate.now().year

    @BeforeEach
    fun open() {
        open("$domain/orgs/ncp/people")
        `$`("#login_field").value = "haekyu.cho"
        `$`("#password").value = "Cdr0m38^"
        `$`("[name=commit]").submit()

//        fetchMembers()
    }

    @Test
    fun aggregateGithubActivities() {
        // todo: members를 순회하여 적용
        val activities = (from..to).map { month ->
            val monthText = month.toString().padStart(2, '0')
            // fixme : memberId -> member[x]
            val memberGithubUrl =
                "${domain}${memberId}?tab=overview&from=${year}-${monthText}-01&to=${year}-${monthText}-31"
//            open("$domain${members[0].id}?tab=overview&from=2020-01-01&to=2020-06-30")

            // 맴버의 overview화면에서 본인이 작성한 review url, title정보를 추출한다.
            Activity(month).also {
                it.commitLogs = extractCommitLogs(memberGithubUrl)
                    .also {
                    it.forEach { x ->
                        println("${x.commitUrl}, line : ${x.modifiedLineCount}")
                    }
                }

                it.pullRequests = extractPullRequests(memberGithubUrl).also {
                    it.forEach { x ->
                        println("${x.prTitle} : ${x.prTitle}")
                    }
                }

                it.reviews = extractReviews(memberGithubUrl).also {
                    it.forEach { x ->
                        println("${x.reviewPrUrl}, ${x.comments.size}")
                    }
                }
            }
        }
    }

    private fun extractReviews(memberGithubUrl: String): List<Review> {
        open(memberGithubUrl)
        val reviews = `$`(".octicon.octicon-eye").parent().parent().findAll("a").map {
            Review(
                it.innerText().trim(),
                it.getAttribute("href") ?: throw Exception("맴버 overview에서 월별 pullRequest comment url 주소가 어떻게 없을 수가 있죠?")
            )
        }

        // 리뷰 페이지 이동 & 코멘트 수집
        reviews.forEachIndexed { idx, review ->
            open(review.reviewPrUrl)

            // show outdated
            `$$`("span[title='Label: Outdated']").forEach { outdated ->
                outdated.click()
                sleep(1000)
            }

            // outdated comments 수집
            val comments = `$$`("h4 strong .author.text-inherit.css-truncate-target").filter { it.isDisplayed }
                .map { authorElem ->
                    authorElem.parent().parent().parent()
                        .findAll("h4 strong a.author")
                        .filter { it.text.trim() == memberId }
                        .map { it ->
                            it.parent().parent().parent()
                                .find("div.comment-body.markdown-body")
                                .innerText()
                                .trim()
//                                .also {
//                                    println("$memberId 코멘트 : $it")
//                                }
                        }
                }
                .flatten()
                .run {
                    val lists = `$$`("h3.timeline-comment-header-text.f5.text-normal")
                        .filter { it.isDisplayed }
                        .filter { it.find("a.author").text().trim() == memberId }
                        .map { it.parent().parent().parent().parent().find("td.comment-body p").text().trim() }

                    this + lists
                }

            reviews[idx].comments = comments
        }

        return reviews
    }

    // pullRequest 집계
    private fun extractPullRequests(memberGithubUrl: String): List<PullRequest> {
        open(memberGithubUrl)

        val pullRequests = mutableListOf<PullRequest>()
        val createdPrs = `$$`("h3 a[data-hovercard-type=pull_request]")
        val openedPrs = `$`(".octicon.octicon-git-pull-request").parent().parent().findAll("a")
        createdPrs.forEach {
            pullRequests.add(
                PullRequest(it.innerText().trim(), it.getAttribute("href") ?: throw Exception("pr url이 없으면 안됨"))
            )
        }
        openedPrs.forEach {
            pullRequests.add(
                PullRequest(it.innerText().trim(), it.getAttribute("href") ?: throw Exception("pr url이 없으면 안됨"))
            )
        }

        return pullRequests
    }

    // 월별 개발자 commits 목록 화면 진입, commmit 집계
    private fun extractCommitLogs(memberGithubUrl: String): List<CommitLog> {
        open(memberGithubUrl)

        val commitLogs = mutableListOf<CommitLog>()

        extractCommitHrefs().forEach { href ->
            open(href)

            `$$`(".message.js-navigation-open").forEach {
                val commitLogTitle = (it.getAttribute("aria-label") ?: "").substringBefore('\n')
                val commitUrl = it.attr("href") ?: throw Exception("commit로그 페이지 링크 오류")

                // 중복제거
                if (commitLogs.none { t -> t.commitUrl == commitUrl }) {
                    commitLogs.add(CommitLog(commitLogTitle, commitUrl))
                }
            }
        }

        // 라인 수정 카운트
        commitLogs.forEachIndexed { idx, it ->
            open(it.commitUrl)
            Thread.sleep(2000)
            executeJavaScript<String>("window.scrollTo(0, document.body.scrollHeight)")

            val plusMarkerCount = `$$`("td.blob-code.blob-code-deletion.blob-code-marker-cell").size
            val minusMarkerCount = `$$`("td.blob-code.blob-code-addition.blob-code-marker-cell").size
            it.modifiedLineCount = plusMarkerCount + minusMarkerCount
        }

        return commitLogs
    }

    // 개발자의 월별 커밋 github 화면 url추출
    private fun extractCommitHrefs(): List<String> {
        return `$$`("li[class^=ml-0]").map {
            it.find(By.className("f6")).getAttribute("href") ?: throw Exception("commit 링크 오류")
        }
    }

    // 개발자 목록 가져오기
    private fun getMembers() {
        while (true) {
            members.addAll(`$`(".table-list").findAll("[itemprop=name]").map {
                val id = it.text()
                val (name, team, company) = it.parent().findAll("a")[0].text().split("/")

                Member(id, name, team, company)
            })

            val nextElem = `$`(".next_page")
            if (nextElem.attr("class")!!.contains("disabled")) {
                break
            } else {
                nextElem.click()
            }
        }
    }
}

data class Member(
    val id: String,
    val name: String,
    val team: String,
    val company: String,
    var activity: List<Activity> = emptyList()
)

data class Activity(
    val month: Int,
    var commitLogs: List<CommitLog> = mutableListOf(),
    var pullRequests: List<PullRequest> = mutableListOf(),
    var reviews: List<Review> = mutableListOf()
)

data class CommitLog(val commitLogTitle: String, val commitUrl: String, var modifiedLineCount: Int = 0)
data class PullRequest(val prTitle: String, val prUrl: String)
data class Review(
    val reviewPrTitle: String,
    val reviewPrUrl: String,
    var comments: List<String> = mutableListOf()
)