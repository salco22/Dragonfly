package com.salco.dragonfly.feature.classification

import app.cash.turbine.test
import com.salco.dragonfly.core.model.Category
import com.salco.dragonfly.core.model.Tag
import com.salco.dragonfly.core.testing.FakeCategoriesRepository
import com.salco.dragonfly.core.testing.FakeTagsRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.classification.categories.CategoriesUiState
import com.salco.dragonfly.feature.classification.categories.CategoriesViewModel
import com.salco.dragonfly.feature.classification.tags.TagsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ClassificationViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categories = FakeCategoriesRepository()
    private val tags = FakeTagsRepository()

    @Test
    fun addCategory_appearsInList() = runTest {
        val vm = CategoriesViewModel(categories)
        vm.uiState.test {
            assertTrue((awaitItem() as CategoriesUiState.Success).categories.isEmpty())
            vm.startAdding()
            vm.onNameChange(" Casa ")
            vm.save()
            assertEquals(listOf("Casa"), (awaitItem() as CategoriesUiState.Success).categories.map { it.category.name })
        }
        assertNull(vm.editor.value)
    }

    @Test
    fun blankOrDuplicateName_showsError() = runTest {
        categories.saveCategory(Category(name = "Casa"))
        val vm = CategoriesViewModel(categories)
        vm.startAdding()
        vm.save()
        assertEquals(NameError.BLANK, vm.editor.value?.error)
        vm.onNameChange("casa")
        vm.save()
        assertEquals(NameError.DUPLICATE, vm.editor.value?.error)
    }

    @Test
    fun renameAndDeleteTag() = runTest {
        val id = tags.saveTag(Tag(name = "Vacanze"))
        val vm = TagsViewModel(tags)
        vm.startEditing(Tag(id, "Vacanze"))
        vm.onNameChange("Viaggi")
        vm.save()
        assertEquals(listOf("Viaggi"), tags.tags.value.map { it.name })

        vm.startEditing(Tag(id, "Viaggi"))
        vm.delete()
        assertTrue(tags.tags.value.isEmpty())
        assertNull(vm.editor.value)
    }
}
