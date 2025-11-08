package ru.msk.xls.kpo.bank.domain

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

enum class CategoryType {
    INCOME, SPENDING
}

object Categories : IntIdTable("categories") {
    val type = enumeration<CategoryType>("type")
    val name = varchar("name", 128)
}

class Category(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Category>(Categories)

    var type by Categories.type
    var name by Categories.name
}
