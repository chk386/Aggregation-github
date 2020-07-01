package com.nhn.github

import java.io.File
import java.nio.charset.Charset

fun printCsv(member: Member) {
    // 월, 커밋로그수, 코드라인, pr수, 리뷰댓글수
    val (id, name, team, company, activities) = member
    var (t1, t2, t3, t4) = listOf(0, 0, 0, 0)

    activities.forEach { activity ->
        val (month, commitLogs, pullRequests, reviews) = activity

        val commitCount = commitLogs.size
        val totalLineCount = commitLogs.map { it.modifiedLineCount }
            .fold(0) { a, b -> a + b }
        val totalPrCount = pullRequests.size
        val reviewCount = reviews.map { it.comments.size }
            .fold(0) { a, b -> a + b }

        t1 += commitCount
        t2 += totalLineCount
        t3 += totalPrCount
        t4 += reviewCount

        println("${id}\t${name}\t${company}\t${team}\t${month}\t${commitLogs.size}\t${totalLineCount}\t${totalPrCount}\t${reviewCount}")
    }

    if(activities.isNotEmpty()) {
        println("\t\t\t\t총\t${t1}\t${t2}\t${t3}\t${t4}")
    }

    val sb = StringBuilder()

    val commitDetails = member.activity.map { activity -> activity.commitLogs }
        .flatten()
        .joinToString("\n") {
            "commit로그\t${it.commitLogTitle.lines().first()}\t${it.commitUrl}\t${it.modifiedLineCount}"
        }


    val prDetails = "\n" + member.activity.map { activity -> activity.pullRequests }
        .flatten()
        .joinToString("\n") {
            "pullRequest로그\t${it.prTitle.lines().first()}\t${it.prUrl}"
        }

    sb.append(commitDetails).append(prDetails)

    member.activity.map { activity -> activity.reviews }
        .flatten()
        .forEach { review ->
            review.comments.forEach {
                sb.append(
                    "\nreview댓글\t${review.reviewPrTitle.lines().first()}\t${review.reviewPrUrl}\t${it.lines().first()}"
                )
            }
        }

    val memberDetail = sb.toString()
    if(memberDetail.trim().isNotBlank()) {
        File("/Users/haekyu.cho/Desktop/${member.id}.csv").writeText(memberDetail, Charset.defaultCharset())
    }

    sb.setLength(0)
}
