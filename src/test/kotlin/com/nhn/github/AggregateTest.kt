package com.nhn.github

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.WebDriverRunner
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

        fetchAllMembers()
    }

    @Test
    fun aggregateGithubActivities() {
        // todo: members를 순회하여 적용
        members.filter { it.company == "JP" }
            .forEach { member ->
                for (x in 0 until members.size) {
                    if (member.id == members[x].id) {
                        members[x].activity = getAll(member)
                    }
                }
            }

        println("드디어 끝났다.!!")
    }

    private fun getAll(member: Member): List<Activity> {
        return (from..to).map { month ->
            val monthText = month.toString().padStart(2, '0')
            // fixme : memberId -> member[x]
            val memberGithubUrl =
                "${domain}${member.id}?tab=overview&from=${year}-${monthText}-01&to=${year}-${monthText}-31"

            open(memberGithubUrl)

            if (`$`(".col-lg-9.col-md-8.col-12.float-md-left.pl-md-2").innerText().trim()
                    .startsWith("This user is suspended.")
            ) {
                return emptyList()
            }

            // 맴버의 overview화면에서 본인이 작성한 review url, title정보를 추출한다.
            Activity(month).also {
                val extractCommitLogs = extractCommitLogs(memberGithubUrl)
                    .toMutableList()

                it.pullRequests = extractPullRequests(memberGithubUrl)
                    .also { pullRequest ->
                        pullRequest.forEach { x ->
                            println("${x.prTitle} : ${x.prTitle}")
                        }
                    }

                val (reviews, commitLogs) = extractReviews(member, memberGithubUrl)
                    .also { review ->
                        review.first.forEach { x ->
                            println("${x.reviewPrUrl}, ${x.comments.size}")
                        }
                    }

                it.reviews = reviews
                it.commitLogs = extractCommitLogs + commitLogs
            }
        }
    }

    private fun extractReviews(member: Member, memberGithubUrl: String): Pair<List<Review>, List<CommitLog>> {
        open(memberGithubUrl)

        if (WebDriverRunner.getWebDriver().findElements(By.cssSelector(".octicon.octicon-eye")).size == 0) {
            return Pair(emptyList(), emptyList())
        }

        val reviews = `$`(".octicon.octicon-eye").parent().parent().findAll("a").map {
            Review(
                it.innerText().trim(),
                it.getAttribute("href") ?: throw Exception("맴버 overview에서 월별 pullRequest comment url 주소가 어떻게 없을 수가 있죠?")
            )
        }

        val commitLogs = mutableListOf<CommitLog>()

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
                        .map { it ->
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

            // https://github.nhnent.com/commerce-jp/tempocloud/pull/1674  로 이동후
            // commits탭 클릭
            val tabNavElem = `$$`(".tabnav-tab.js-pjax-history-navigate")[1]
            // keigo-hokonohara에서 오류
            tabNavElem.click()
            tabNavElem.waitUntil(Condition.attribute("class", "tabnav-tab selected js-pjax-history-navigate"), 1000)

            // commits를 순회하여
            val map = `$$`("div.table-list-cell p a.message").map {
                val commitLog = it.attr("aria-label") ?: throw Exception("커밋로그 타이틀은 반드시 재")
                val url = it.attr("href") ?: throw Exception("url은 반드시 commitLogs")

                Pair(commitLog, url)
            }.map {
                val (commitLog, url) = it
                val hash = url.substringAfterLast("/")
                val pre = url.substringBefore("pull")
                val commitUrl = "${pre}commit/$hash"

                open(commitUrl)
                val modifiedLineCount = getDiffLineCount()
                back()

                CommitLog(commitLog, commitUrl, modifiedLineCount)
            }

            commitLogs.addAll(map)

            // https://github.nhnent.com/ncp/admin/pull/91/commits/77c3a94c9ed9ff96231498c687267c4d106abd72 의 커밋로그를 따서
            // https://github.nhnent.com/ncp/admin/commit/77c3a94c9ed9ff96231498c687267c4d106abd72 커밋로그로 변환
            // 여기서 부터는 extractCommitLogs와 동일..
        }

        return Pair(reviews, commitLogs)
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
        commitLogs.forEach {
            open(it.commitUrl)

            it.modifiedLineCount = getDiffLineCount()
        }

        return commitLogs
    }

    private fun getDiffLineCount(): Int {
        return `$`(".toc-diff-stats").let {
            val texts = it.findAll("strong")

            val (additionsIdx, deletiionsIdx) =
                if (texts.size == 3) {
                    1 to 2
                } else {
                    0 to 1
                }

            val modifiedLineCount = texts[additionsIdx].text.substringBefore(" ").removeComma()
                .toInt() + texts[deletiionsIdx].text.substringBefore(" ").removeComma().toInt()

            modifiedLineCount
        }
    }

    // 개발자의 월별 커밋 github 화면 url추출
    private fun extractCommitHrefs(): List<String> {
        return `$$`("li[class^=ml-0]").map {
            it.find(By.className("f6")).getAttribute("href") ?: throw Exception("commit 링크 오류")
        }
    }

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