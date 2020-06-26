package com.nhn.github

import org.junit.jupiter.api.Test
import kotlin.math.acos

fun printCsv(members: List<Member>) {
    // 월, 커밋로그수, 코드라인, pr수, 리뷰댓글수

    println("아이디\t이름\t회사\t팀\t월\t커밋수\t개발라인수\tpr요청수\t리뷰댓글수")
    members
        .filter { it.activity.isNotEmpty() }
        .forEach { member ->
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

            println("\t\t\t\t총\t${t1}\t${t2}\t${t3}\t${t4}")
        }

    members
        .filter { it.activity.isNotEmpty() }
        .forEach { m ->
            m.activity.map { activity -> activity.commitLogs }
                .flatten()
                .forEach {
                    println("commit로그\t${it.commitLogTitle.lines().first()}\t${it.commitUrl}\t${it.modifiedLineCount}")
                }

            m.activity.map { activity -> activity.pullRequests }
                .flatten()
                .forEach {
                    println("pullRequest로그\t${it.prTitle.lines().first()}\t${it.prUrl}")
                }


            m.activity.map { activity -> activity.reviews }
                .flatten()
                .forEach { review ->
                    review.comments.forEach {
                        println("review댓글\t${review.reviewPrTitle.lines().first()}\t${review.reviewPrUrl}\t${it.lines().first()}")
                    }
                }
        }

}

