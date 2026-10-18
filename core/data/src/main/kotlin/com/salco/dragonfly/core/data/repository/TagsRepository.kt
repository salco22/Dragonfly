package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.TagDao
import com.salco.dragonfly.core.database.model.asEntity
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.Tag
import com.salco.dragonfly.core.model.TagSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface TagsRepository {
    fun observeTags(): Flow<List<Tag>>
    fun observeTagSummaries(): Flow<List<TagSummary>>

    /** Saves [tag] and returns its id; fails with [DuplicateNameException] if the name is taken. */
    suspend fun saveTag(tag: Tag): Long

    /** Deletes the tag and removes it from every transaction. */
    suspend fun deleteTag(id: Long)
}

internal class OfflineTagsRepository @Inject constructor(
    private val tagDao: TagDao,
) : TagsRepository {

    override fun observeTags(): Flow<List<Tag>> = tagDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override fun observeTagSummaries(): Flow<List<TagSummary>> =
        tagDao.observeUsage().map { rows -> rows.map { TagSummary(Tag(it.id, it.name), it.transactionCount) } }

    override suspend fun saveTag(tag: Tag): Long {
        val name = tag.name.trim()
        val existing = tagDao.findByName(name)
        if (existing != null && existing.id != tag.id) throw DuplicateNameException(name)
        val rowId = tagDao.upsert(tag.copy(name = name).asEntity())
        return if (tag.id != 0L) tag.id else rowId
    }

    override suspend fun deleteTag(id: Long) = tagDao.deleteById(id)
}
