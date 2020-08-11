package com.nhn.github

/**
 * @author Haekyu Cho
 */
data class CommitLog(val no: Int, val author: String, val month: Int, val url: String, val title: String, val modifiedLineCount: Int, val deletedLintCount: Int, val fileChangedCount: Int, val fileChangedNameCompressed: String)