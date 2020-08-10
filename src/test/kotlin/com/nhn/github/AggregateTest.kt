package com.nhn.github

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.WebDriverRunner
import com.vladsch.kotlin.jdbc.HikariCP
import com.vladsch.kotlin.jdbc.SessionImpl
import com.vladsch.kotlin.jdbc.sqlQuery
import com.vladsch.kotlin.jdbc.usingDefault
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.io.File
import java.nio.charset.Charset
import java.time.LocalDate

/**
 * @author haekyu cho
 */

internal class AggregateTest {
    private val members = mutableListOf<Member>()
    private val domain = "https://github.nhnent.com/"
    private val org = "commerce-jp"

    // 시작월
    private val from = 1

    // 종료월
    private val to = 7

    // 현재 년도
    private val year = LocalDate.now().year

    @BeforeEach
    fun open() {
        HikariCP.default("jdbc:mysql://localhost:3306/test", "chk386", "Cdr0m38^")
        SessionImpl.defaultDataSource = { HikariCP.dataSource() }

//        Configuration.headless = true
        open("$domain/orgs/ncp/people")
        `$`("#login_field").value = "haekyu.cho"
        `$`("#password").value = "Cdr0m38^"
        `$`("[name=commit]").submit()

//        fetchAllMembers()
    }

    @Test
    fun a() {
        val listOf =
            listOf(1 to "https://github.nhnent.com/commerce-jp/tempocloud/commit/c7cad7e5b037458f53ca0e3fb2ee281447153ccb")

        fetchCodeLines(listOf)
    }

    @Test
    fun `커밋로그 집계`() {
        val commitNosWithUrl = extractCommitLogs(listOf(org))
        fetchCodeLines(commitNosWithUrl)


        // comit 상세 페이지로 이동하여 x additions를 카운트와 changedFiles를 추출하자
    }

    @Test
    fun `activity 집계`() {
        println("아이디\t이름\t회사\t팀\t월\t커밋수\t개발라인수\tpr요청수\t리뷰댓글수")

        members.filter { it.company == "JP" }
            .forEach { member ->
                for (x in 0 until members.size) {
                    if (member.id == members[x].id) {
                        members[x].activity = fetchAllActivities(member)
                        printCsv(members[x])
                    }
                }
            }
    }

    private fun fetchAllActivities(member: Member): List<Activity> {
        return (from..to).map { month ->
            val monthText = month.toString().padStart(2, '0')
            val memberGithubUrl =
                "${domain}${member.id}?tab=overview&from=${year}-${monthText}-01&to=${year}-${monthText}-31"

            open(memberGithubUrl)

            if (`$`(".col-lg-9.col-md-8.col-12.float-md-left.pl-md-2")
                    .innerText()
                    .trim()
                    .startsWith("This user is suspended.")
            ) {
                return emptyList()
            }

            // 맴버의 overview화면에서 본인이 작성한 review url, title정보를 추출한다.
            Activity(month).apply {

                if (`$`("#js-contribution-activity").text().contains("no activity")) {
                    pullRequests = emptyList()
                    reviews = emptyList()
                    commitLogs = emptyList()
                } else {
                    val prs = extractPullRequests(memberGithubUrl)

                    pullRequests = prs
                    reviews = extractReviews(member, memberGithubUrl)
                }
            }
        }
    }

    private fun extractReviews(member: Member, memberGithubUrl: String): List<Review> {
        open(memberGithubUrl)

        if (WebDriverRunner.getWebDriver().findElements(By.cssSelector(".octicon.octicon-eye")).size == 0) {
            return emptyList()
        }

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
                        .filter { it.text.trim() == member.id }
                        .map {
                            it.parent().parent().parent()
                                .find("div.comment-body.markdown-body")
                                .innerText()
                                .trim()
                        }
                }
                .flatten()
                .run {
                    val lists = `$$`("h3.timeline-comment-header-text.f5.text-normal")
                        .filter { it.isDisplayed }
                        .filter { it.find("a.author").text().trim() == member.id }
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

//    @Deprecated(message = "커밋로그는 repository에 전체 commit탭에서 추출하자")
//    private fun extractCommitLogsInPullRequest(
//        pullRequests: MutableList<PullRequest>,
//        memberId: String
//    ): List<CommitLog> {
//        // pullRequest에 있는 커밋 로그 수집
//        return pullRequests.map { pr ->
//            open(pr.prUrl)
//
//            // https://github.nhnent.com/commerce-jp/tempocloud/pull/1674  로 이동후
//            // commits탭 클릭
//            val tabNavElem = `$$`(".tabnav-tab.js-pjax-history-navigate")[1]
//            // keigo-hokonohara에서 오류
//            tabNavElem.click()
//            tabNavElem.waitUntil(Condition.attribute("class", "tabnav-tab selected js-pjax-history-navigate"), 30000)
//
//            // commits를 순회하여
////            `$$`("div.table-list-cell p a.message")
//            `$$`("div.table-list-cell:not(.commit-links-cell)").filter {
//                it.find("div.commit-meta div .commit-author").text().startsWith(memberId)
//            }.map {
//                it.find("p a.message")
//            }.map {
//                val commitLog = it.attr("aria-label") ?: throw Exception("커밋로그 타이틀은 반드시 재")
//                val url = it.attr("href") ?: throw Exception("url은 반드시 commitLogs")
//
//                Pair(commitLog, url)
//            }.map {
//                val (commitLog, url) = it
//                val hash = url.substringAfterLast("/")
//                val pre = url.substringBefore("pull")
//                val commitUrl = "${pre}commit/$hash"
//
//                open(commitUrl)
//                val modifiedLineCount = getDiffLineCount()
//                back()
//
//                CommitLog(commitLog, commitUrl, modifiedLineCount)
//            }
//            // https://github.nhnent.com/ncp/admin/pull/91/commits/77c3a94c9ed9ff96231498c687267c4d106abd72 의 커밋로그를 따서
//            // https://github.nhnent.com/ncp/admin/commit/77c3a94c9ed9ff96231498c687267c4d106abd72 커밋로그로 변환
//            // 여기서 부터는 extractCommitLogs와 동일..
//        }.flatten()
//    }

    // 월별 개발자 commits 목록 화면 진입, commmit 집계
//    private fun extractCommitLogs(memberGithubUrl: String): List<CommitLog> {
//        open(memberGithubUrl)
//
//        val commitLogs = mutableListOf<CommitLog>()
//
//        extractCommitHrefs().forEach { href ->
//            open(href)
//
//            `$$`(".message.js-navigation-open").forEach {
//                val commitLogTitle = (it.getAttribute("aria-label") ?: "").substringBefore('\n')
//                val commitUrl = it.attr("href") ?: throw Exception("commit로그 페이지 링크 오류")
//
//                // 중복제거
//                if (commitLogs.none { t -> t.commitUrl == commitUrl }) {
//                    commitLogs.add(CommitLog(commitLogTitle, commitUrl))
//                }
//            }
//        }
//
//        // 라인 수정 카운트
//        commitLogs.forEach {
//            open(it.commitUrl)
//
//            it.modifiedLineCount = getDiffLineCount()
//        }
//
//        return commitLogs
//    }

//    private fun getDiffLineCount(): Int {
//        return `$`(".toc-diff-stats").let {
//            val texts = it.findAll("strong")
//
//            val (additionsIdx, deletionIdx) =
//                if (texts.size == 3) {
//                    1 to 2
//                } else {
//                    0 to 1
//                }
//
//            val modifiedLineCount = texts[additionsIdx].text.substringBefore(" ").removeComma()
//                .toInt() + texts[deletionIdx].text.substringBefore(" ").removeComma().toInt()
//
//            modifiedLineCount
//        }
//    }

    // 개발자의 월별 커밋 github 화면 url추출
//    @Deprecated(message = "이건 여기서 안쓸듯?")
//    private fun extractCommitHrefs(): List<String> {
//        return `$$`("li[class^=ml-0]").map {
//            it.find(".f6").getAttribute("href") ?: throw Exception("commit 링크 오류")
//        }
//    }

    // 개발자 목록 가져오기
    private fun fetchAllMembers() {
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

fun String.removeComma(): String {
    return replace(",", "")
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

