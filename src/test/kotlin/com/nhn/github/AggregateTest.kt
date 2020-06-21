package com.nhn.github

import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.open
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
        `$`("#password").value = "asdß"
        `$`("[name=commit]").submit()

//        fetchMembers()
    }

    private fun fetchMembers() {Ø¡
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

    @Test
    fun login() {
        for(x in from..to) {
            val month = x.toString().padStart(2, '0')

//            open("$domain${members[0].id}?tab=overview&from=2020-01-01&to=2020-06-30")
            open("${domain}sungchoon-park?tab=overview&from=${year}-${month}-01&to=${year}-${month}-30")

            // commit 카운트와 commit url, total 코드 작성(삭제 수)


        }


        // activity 조회
//        open("$domain${members[0].id}?tab=overview&from=2020-01-01&to=2020-06-30")




//        println(members)
    }
}

data class Member(val id: String, val name: String, val team: String, val company: String, var activity: List<Activity> = emptyList())

data class Activity(val month: Int, val commit: String, val pr: String, val review: String, val commitUrls: List<String>, val prUrls: List<String>, val reviewUrls: List<String>, val totalCodeLine: Int)