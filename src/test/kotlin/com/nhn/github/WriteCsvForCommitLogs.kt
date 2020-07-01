package com.nhn.github

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide.*

/**
 * @author haekyu cho
 */

fun extractCommitLogs(orgs: List<String>): List<CommitDetailLog> {
    val commitDetailLogs = mutableListOf<CommitDetailLog>()

    return orgs.map { org ->
        val url = "https://github.nhnent.com/$org"
        open(url)

        `$$`(".org-repos.repo-list ul li").filter {
            !it.find("relative-time").text.contains("2019")
        }.map {
            it.find("div div h3 a").attr("href") ?: throw Exception("없으면 안됨")
        }
    }.flatten()
        .map {
            open(it)
            `$`(".commits a").click()
            `$`("#branch-select-menu").waitUntil(Condition.appear, 5000)

            while (true) {
                `$$`(".commit-group-title").filter { elem ->
                    elem.text.contains("2020")
                }.forEach { elem ->
                    val month = toMonth(elem.text)

                    elem.sibling(0).findAll("li.commit").map { li ->
                        val commitDiv = li.find("div.table-list-cell")
                        val commitDetail = commitDiv.find("p.commit-title a.message")
                        val commitTitle =
                            (commitDetail.attr("aria-label") ?: throw Exception("없으면 안된다.!")).substringBefore('\n')
                        val commitUrl = commitDetail.attr("href") ?: throw Exception("url이 어떻게 없을수가 있나?")
                        val memberId = commitDiv.find("a.commit-author, span.commit-author").text

                        commitDetailLogs.add(CommitDetailLog(memberId, month, commitTitle, commitUrl))
                    }
                }

                // 2019가 있으면 종료
                val is2019 = `$$`(".commit-group-title").any { x -> x.text.contains("2019") }
                val aBtn = `$$`("a.btn.btn-outline.BtnGroup-item").last()
                val clickable = aBtn.text == "Older"

                if (is2019 || !clickable) {
                    break
                } else {
                    open(aBtn.attr("href") ?: "")
                }
            }

            return commitDetailLogs
        }
}

fun fetchCodeLines(commitLogs: List<CommitDetailLog>): List<CommitDetailLog> {
    return commitLogs.map {
        open(it.commitUrl)
        `$`(".toc-diff-stats").waitUntil(Condition.appear, 5000)

        val elem = `$`("button.btn-link.js-details-target")
        val changedFileCount = elem.text.substringBefore(" ").toInt()
        val additions = elem.sibling(0).text.substringBefore(" ").toInt()

        it.copy(changedFilesCount = changedFileCount, modifiedLineCount = additions)
    }
}

data class CommitDetailLog(
    val memberId: String,
    val month: Int,
    val commitLogTitle: String,
    val commitUrl: String,
    var modifiedLineCount: Int = 0,
    var changedFilesCount: Int = 0
)

private fun toMonth(txt: String): Int {
    val lowercase = txt.toLowerCase()
    return when {
        lowercase.contains("jun") -> 6
        lowercase.contains("may") -> 5
        lowercase.contains("apr") -> 4
        lowercase.contains("mar") -> 3
        lowercase.contains("feb") -> 2
        lowercase.contains("jan") -> 1
        else -> throw Exception("일단 1-6월까지")
    }
}
