package com.nhn.github

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.WebDriverRunner.getWebDriver
import com.vladsch.kotlin.jdbc.sqlQuery
import com.vladsch.kotlin.jdbc.usingDefault
import org.openqa.selenium.By.cssSelector
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset.defaultCharset
import java.util.zip.Deflater

/**
 * @author haekyu cho
 */

fun extractCommitLogs(orgs: List<String>): List<Pair<Int, String>> {
    return orgs.asSequence().map { org ->
        val url = "https://github.nhnent.com/$org"
        open(url)

        `$$`(".org-repos.repo-list ul li").filter {
            !it.find("relative-time").text.endsWith("2019")
        }.map {
            it.find("div div h3 a").attr("href") ?: throw Exception("없으면 안됨")
        }
    }.flatten()
//        .filter { !it.contains("tempocloud") }
        .filter { !it.contains("japan-nhngodo-shopby") }
        .map {
            val commitNos = mutableListOf<Pair<Int, String>>()

            open(it)
            `$`(".commits a").click()
            `$`("#branch-select-menu").waitUntil(Condition.appear, 5000)

            while (true) {
                `$$`(".commit-group-title").filter { elem ->
                    !elem.text.endsWith("2019")
                }.forEach { elem ->
                    val month = toMonth(elem.text)

                    if (month <= 7) {
                        elem.sibling(0).findAll("li.commit").map { li ->
                            val commitDiv = li.find("div.table-list-cell")
                            val findAll = commitDiv.findAll("p.commit-title a")
                            val commitDetail = if (findAll[0].attr("aria-label") == null) {
                                if (findAll.size == 1) {
                                    findAll[0]
                                } else {
                                    findAll[1]
                                }
                            } else {
                                findAll[0]
                            }

                            try {
                                val commitTitle =
                                    if (commitDetail.attr("aria-label") == null) {
                                        commitDetail.text
                                    } else {
                                        commitDetail.attr("aria-label")!!.substringBefore('\n')
                                    }

                                val commitUrl = commitDetail.attr("href") ?: throw Exception("url이 어떻게 없을수가 있나?")
                                val author = commitDiv.find("a.commit-author, span.commit-author").text

                                usingDefault { session ->
                                    session.transaction { tx ->
                                        with(commitNos) {
                                            val commitNo = tx.updateGetId(
                                                sqlQuery(
                                                    """
                                                insert into commit_log(author, month, url, title, modified_line_count, deleted_line_count, file_changed_count, file_changed_name_compressed)
                                                values(?, ?, ?, ?, ?, ?, ?, substring(?, 1, 600))
                                                """.trimIndent(), author, month, commitUrl, commitTitle, 0, 0, 0, ""
                                                )
                                            ) ?: 0

                                            add(commitNo to commitUrl)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                println(commitDetail.attr("href") ?: throw Exception("url이 어떻게 없을수가 있나?"))
                            }

//                            if (!commitTitle.startsWith("Merge branch")) {
//                                commitDetailLogs.add(CommitDetailLog(memberId, month, commitTitle, commitUrl))
//                            }
                        }
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

            commitNos
        }.flatten().toList()
}

fun fetchCodeLines(commitNosWithUrl: List<Pair<Int, String>>) {
    commitNosWithUrl.filter {
        !it.second.startsWith("https://nhnent.dooray.com")
    }.forEach { (no, url) ->
        open(url)
        `$`(".toc-diff-stats").waitUntil(Condition.appear, 5000)

        val elem = `$`("button.btn-link.js-details-target,div.toc-diff-stats strong")
        val changedFileCount = elem.text.substringBefore(" ").removeComma().toInt()
        val additions = elem.sibling(0).text.substringBefore(" ").removeComma().toInt()
        val deletions = elem.sibling(1).text.substringBefore(" ").removeComma().toInt()

        val fileNames = if (getWebDriver().findElements(cssSelector(".toc-diff-stats button.btn-link")).size == 0) {
            ""
        } else {
            `$`(".toc-diff-stats button.btn-link").click()
            `$$`("#toc ol.content.collapse.js-transitionable li > a").joinToString { it.text }
        }

        // update하자
        usingDefault { session ->
            session.transaction { tx ->
                tx.update(
                    sqlQuery(
                        """
                        update commit_log
                           set modified_line_count = ?
                             , deleted_line_count = ?
                             , file_changed_count = ?
                             , file_changed_name_compressed = substring(?, 1, 600)
                         where no = ?    
                        """.trimIndent(), additions, deletions, changedFileCount, compress(fileNames), no
                    )
                )
            }
        }
    }
}

private fun compress(fileNames: String): String {
    val toByteArray = fileNames.toByteArray(defaultCharset())
    val buf = ByteArray(1024)
    val baos = ByteArrayOutputStream(toByteArray.size)

    Deflater().apply {
        setLevel(Deflater.BEST_COMPRESSION)
        setInput(toByteArray)
        finish()
    }.run {
        while (!finished()) {
            val count = deflate(buf)
            baos.write(buf, 0, count)
        }

        baos.close()

        return baos.toByteArray().toHex()
    }
}

data class CommitDetailLog(
    val memberId: String,
    val month: Int,
    val commitLogTitle: String,
    val commitUrl: String,
    var modifiedLineCount: Int = 0,
    var deletedLineCount: Int = 0,
    var changedFilesCount: Int = 0
) {
    fun `is`(newCommitLogs: List<CommitDetailLog>): Boolean {
        if (newCommitLogs.isEmpty()) {
            return false
        }

        val newCommitLog = newCommitLogs.last()

        return this.memberId == newCommitLog.memberId &&
                this.modifiedLineCount == newCommitLog.modifiedLineCount &&
                this.deletedLineCount == newCommitLog.deletedLineCount &&
                this.changedFilesCount == newCommitLog.changedFilesCount
    }
}

private fun toMonth(txt: String): Int {
    return txt.toLowerCase().let {
        when {
            it.contains("aug") -> 8
            it.contains("jul") -> 7
            it.contains("jun") -> 6
            it.contains("may") -> 5
            it.contains("apr") -> 4
            it.contains("mar") -> 3
            it.contains("feb") -> 2
            it.contains("jan") -> 1
            else -> throw Exception("일단 1-6월까지")
        }
    }
}

fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}