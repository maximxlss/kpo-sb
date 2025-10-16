package ru.msk.xls.tui.commands

interface Command {
    val signature: String?
    val description: String

    fun dispatch(args: String)
}