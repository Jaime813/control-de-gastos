package com.example.data.local

import com.example.model.CategoryConfig
import com.example.model.CategoryResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class CategoryConfigRepository(
    private val dao: CategoryConfigDao,
) {
    private val seedMutex = Mutex()

    fun observeCategories(): Flow<List<CategoryConfig>> =
        dao.observeAllCategories().map { list ->
            if (list.isEmpty()) CategoryResolver.defaultConfigs()
            else list.map { it.toModel() }
        }

    suspend fun ensureSeeded() {
        seedMutex.withLock {
            if (dao.getCategoryCount() == 0) {
                val defaults = CategoryResolver.defaultConfigs().map { it.toEntity() }
                dao.insertAllIgnore(defaults)
            }
        }
    }

    suspend fun saveCategory(
        id: String?,
        label: String,
        iconKey: String,
        colorHex: Long,
        isEnabled: Boolean,
        monthlyLimit: Double,
    ): Result<String> = try {
        ensureSeeded()
        val cleanLabel = label.trim().take(32)
        require(cleanLabel.isNotBlank()) { "El nombre de la categoría no puede estar vacío." }
        require(monthlyLimit in 0.0..10_000_000.0) { "El límite mensual es inválido." }

        val existing = id?.takeIf { it.isNotBlank() }?.let { dao.getCategoryById(it) }
        val resolvedId = existing?.id ?: buildSafeCategoryId(cleanLabel)
        val sortOrder = existing?.sortOrder ?: (dao.getCategoryCount())

        dao.upsertCategory(
            LocalCategoryConfigEntity(
                id = resolvedId,
                label = cleanLabel,
                iconKey = iconKey,
                colorHex = colorHex,
                isEnabled = isEnabled,
                isDefault = existing?.isDefault ?: false,
                sortOrder = sortOrder,
                monthlyLimit = monthlyLimit,
            )
        )
        Result.success(resolvedId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean): Result<Unit> = try {
        ensureSeeded()
        if (!enabled) {
            val currentEnabledCount = dao.observeAllCategories().first().count { it.isEnabled }
            val target = dao.getCategoryById(categoryId)
            if (target?.isEnabled == true && currentEnabledCount <= 1) {
                return Result.failure(IllegalStateException("Debe quedar al menos una categoría activa."))
            }
        }
        dao.setCategoryEnabled(categoryId, enabled)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteCustomCategory(categoryId: String): Result<Unit> = try {
        ensureSeeded()
        val existing = dao.getCategoryById(categoryId)
            ?: return Result.failure(IllegalArgumentException("Categoría no encontrada."))
        if (existing.isDefault) {
            return Result.failure(IllegalStateException("Las categorías predeterminadas no se eliminan; puedes ocultarlas o renombrarlas."))
        }
        dao.deleteCategoryById(categoryId)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun resetDefaults(): Result<Unit> = try {
        seedMutex.withLock {
            dao.deleteAllCategories()
            dao.insertAllIgnore(CategoryResolver.defaultConfigs().map { it.toEntity() })
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun buildSafeCategoryId(label: String): String {
        val slug = label
            .uppercase()
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')
            .take(18)
            .ifBlank { "CAT" }
        val suffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        return "CAT_${slug}_$suffix".take(40)
    }

    private fun CategoryConfig.toEntity(): LocalCategoryConfigEntity = LocalCategoryConfigEntity(
        id = id,
        label = label,
        iconKey = iconKey,
        colorHex = colorHex,
        isEnabled = isEnabled,
        isDefault = isDefault,
        sortOrder = sortOrder,
        monthlyLimit = monthlyLimit,
    )
}
