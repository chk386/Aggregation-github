package com.nhn.github

import com.codeborne.selenide.Selenide.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.time.LocalDate

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

        val activities: MutableList<Activity> = mutableListOf()

        for (x in from..to) {
            println("================================== $x 월 ======================")
            val month = x.toString().padStart(2, '0')
            // fixme : memberId -> member[x]
            val memberGithubUrl = "${domain}${memberId}?tab=overview&from=${year}-${month}-01&to=${year}-${month}-31"
//            open("$domain${members[0].id}?tab=overview&from=2020-01-01&to=2020-06-30")

//            val memberCommitLogs = extractCommitLogs()
//            val pullRequests = extractPullRequests(memberGithubUrl)

            val reviews = mutableListOf<Review>()

            open(memberGithubUrl)
            `$`(".octicon.octicon-eye").parent().parent().findAll("a").forEach {
                reviews.add(Review(it.innerText().trim(), it.getAttribute("href") ?: throw Exception("pr url이 없으면 안됨")))
            }

            reviews.forEach { review ->
                open(review.reviewPrUrl)

                `$$`("div.review-comment-contents.js-suggested-changes-contents").filter {
                    it.find("strong").innerText().trim() == memberId
                }.forEach {
                    println(it.find(".comment-body markdown-body.js-comment-body").innerText())
                }


                val comments = `$$`(".author.text-inherit.css-truncate-target").filter {
                    it.text == memberId
                }.filter {
                    it.parent().parent().tagName == "h4"
                }.map {
                    val comment =
                        it.parent().parent().parent().find("div.comment-body.markdown-body.js-comment-body")
                            .innerText().trim()
                    println(comment)
                    comment
                }

                review.comments = comments
                println("aa)")
            }

            val activity = Activity(x)


            activities.add(activity)
            // commit 카운트와 commit url, total 코드 작성(삭제 수)

        }

        println("종료")
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
//        commitLogs.forEachIndexed { idx, it ->
//            if(idx > 1) {
//                return@forEachIndexed
//            }
//
//            open(it.commitUrl)
//            val a = `$$`(".blob-code.blob-code-marker-cell").forEach { elem ->
//                val modified =
//                    elem.getAttribute("data-code-marker") ?: throw Exception("diff화면에서 코드라인 옆에는 + 또는 - 또는 공백이여야한다.")
//                if (modified == "+" || modified == "-") {
//                    it.modifiedLineCount++
//                }
//            }
//
//            println(it.commitLogTitle + "  라인수 " + it.modifiedLineCount)
//        }


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
    var pr: String = "",
    var review: String = "",
    var commitLogs: List<CommitLog> = mutableListOf(),
    var pullRequests: List<PullRequest> = mutableListOf(),
    var reviewUrls: List<String> = mutableListOf()
)

data class CommitLog(val commitLogTitle: String, val commitUrl: String, var modifiedLineCount: Int = 0)
data class PullRequest(val prTitle: String, val prUrl: String)
data class Review(
    val reviewPrTitle: String,
    val reviewPrUrl: String,
    var comments: List<String> = mutableListOf()
)