package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.ExpenseDataSource
import com.example.model.CategoryConfig
import com.example.model.Expense
import com.example.model.PaymentMethod
import com.example.model.UserSettings
import com.example.util.DateFormatUtils
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

@Entity(tableName = "guest_expenses")
data class LocalExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val dateKey: String,
    val monthKey: String,
    val note: String?,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val cardName: String? = null,
    val installments: Int = 1,
    val creditSettled: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(tableName = "guest_settings")
data class LocalSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val dailyBudget: Double = 0.0,
    val currencySymbol: String = "$",
)

@Entity(tableName = "category_configs")
data class LocalCategoryConfigEntity(
    @PrimaryKey val id: String,
    val label: String,
    val iconKey: String,
    val colorHex: Long,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val monthlyLimit: Double = 0.0,
) {
    fun toModel(): CategoryConfig = CategoryConfig(
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

@Dao
interface GuestExpenseDao {
    @Query("SELECT * FROM guest_expenses ORDER BY dateKey DESC, createdAtMillis DESC")
    fun observeExpenses(): Flow<List<LocalExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: LocalExpenseEntity)

    @Query("SELECT * FROM guest_expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: String): LocalExpenseEntity?

    @Query("UPDATE guest_expenses SET creditSettled = :settled, updatedAtMillis = :updatedAtMillis WHERE id = :expenseId")
    suspend fun updateCreditSettled(expenseId: String, settled: Boolean, updatedAtMillis: Long)

    @Query("DELETE FROM guest_expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: String)

    @Query("SELECT * FROM guest_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<LocalSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: LocalSettingsEntity)
}

@Dao
interface CategoryConfigDao {
    @Query("SELECT * FROM category_configs ORDER BY sortOrder ASC, label ASC")
    fun observeAllCategories(): Flow<List<LocalCategoryConfigEntity>>

    @Query("SELECT COUNT(*) FROM category_configs")
    suspend fun getCategoryCount(): Int

    @Query("SELECT * FROM category_configs WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: String): LocalCategoryConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: LocalCategoryConfigEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(categories: List<LocalCategoryConfigEntity>)

    @Query("UPDATE category_configs SET isEnabled = :enabled WHERE id = :categoryId")
    suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean)

    @Query("DELETE FROM category_configs WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: String)

    @Query("DELETE FROM category_configs")
    suspend fun deleteAllCategories()
}

@Database(
    entities = [
        LocalExpenseEntity::class,
        LocalSettingsEntity::class,
        LocalCategoryConfigEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class GuestExpenseDatabase : RoomDatabase() {
    abstract fun guestExpenseDao(): GuestExpenseDao
    abstract fun categoryConfigDao(): CategoryConfigDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE guest_expenses ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'")
                db.execSQL("ALTER TABLE guest_expenses ADD COLUMN cardName TEXT")
                db.execSQL("ALTER TABLE guest_expenses ADD COLUMN installments INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE guest_expenses ADD COLUMN creditSettled INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `category_configs` (
                        `id` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `monthlyLimit` REAL NOT NULL DEFAULT 0.0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: GuestExpenseDatabase? = null

        fun getInstance(context: Context): GuestExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GuestExpenseDatabase::class.java,
                    "gasto_diario_guest.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class LocalGuestRepository(
    private val dao: GuestExpenseDao,
) : ExpenseDataSource {

    override fun observeExpenses(): Flow<List<Expense>> =
        dao.observeExpenses().map { list ->
            list.map { entity ->
                Expense(
                    id = entity.id,
                    userId = GUEST_USER_ID,
                    title = entity.title,
                    amount = entity.amount,
                    category = entity.category,
                    dateKey = entity.dateKey,
                    monthKey = entity.monthKey,
                    note = entity.note?.takeIf { it.isNotBlank() },
                    paymentMethod = entity.paymentMethod,
                    cardName = entity.cardName?.takeIf { it.isNotBlank() },
                    installments = entity.installments.toLong(),
                    creditSettled = entity.creditSettled,
                    createdAt = Timestamp(entity.createdAtMillis / 1000, 0),
                    updatedAt = Timestamp(entity.updatedAtMillis / 1000, 0),
                )
            }
        }

    override fun observeUserSettings(): Flow<UserSettings> =
        dao.observeSettings().map { entity ->
            UserSettings(
                id = GUEST_USER_ID,
                userId = GUEST_USER_ID,
                dailyBudget = entity?.dailyBudget ?: 0.0,
                currencySymbol = entity?.currencySymbol ?: "$",
            )
        }

    override suspend fun addExpense(
        title: String,
        amount: Double,
        categoryId: String,
        categoryLabel: String,
        dateKey: String,
        note: String?,
        paymentMethod: PaymentMethod,
        cardName: String?,
        installments: Int,
        creditSettled: Boolean,
    ): Result<Unit> = try {
        require(amount > 0.0 && amount <= 10_000_000.0) {
            "El monto debe ser mayor a 0 y menor a 10,000,000."
        }
        require(DateFormatUtils.isValidDateKey(dateKey)) {
            "Fecha inválida."
        }
        val now = System.currentTimeMillis()
        val cleanCategoryId = categoryId.trim().take(40).ifBlank { "OTHER" }
        val cleanTitle = title.trim().ifBlank { categoryLabel.ifBlank { "Gasto" } }.take(80)
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(160)
        val cleanCardName = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
            cardName?.trim()?.takeIf { it.isNotEmpty() }?.take(40)
        } else null
        val validInstallments = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
            installments.coerceIn(1, 60)
        } else 1
        val validSettled = paymentMethod == PaymentMethod.CREDIT_CARD && creditSettled

        dao.upsertExpense(
            LocalExpenseEntity(
                id = UUID.randomUUID().toString(),
                title = cleanTitle,
                amount = amount,
                category = cleanCategoryId,
                dateKey = dateKey,
                monthKey = dateKey.take(7),
                note = cleanNote,
                paymentMethod = paymentMethod.name,
                cardName = cleanCardName,
                installments = validInstallments,
                creditSettled = validSettled,
                createdAtMillis = now,
                updatedAtMillis = now,
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateExpense(
        expenseId: String,
        title: String,
        amount: Double,
        categoryId: String,
        categoryLabel: String,
        dateKey: String,
        note: String?,
        paymentMethod: PaymentMethod,
        cardName: String?,
        installments: Int,
        creditSettled: Boolean,
    ): Result<Unit> = try {
        require(expenseId.isNotBlank()) { "El ID del gasto no puede estar vacío." }
        require(amount > 0.0 && amount <= 10_000_000.0) {
            "El monto debe ser mayor a 0 y menor a 10,000,000."
        }
        require(DateFormatUtils.isValidDateKey(dateKey)) {
            "Fecha inválida."
        }
        val existing = dao.getExpenseById(expenseId)
        val now = System.currentTimeMillis()
        val cleanCategoryId = categoryId.trim().take(40).ifBlank { "OTHER" }
        val cleanTitle = title.trim().ifBlank { categoryLabel.ifBlank { "Gasto" } }.take(80)
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(160)
        val cleanCardName = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
            cardName?.trim()?.takeIf { it.isNotEmpty() }?.take(40)
        } else null
        val validInstallments = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
            installments.coerceIn(1, 60)
        } else 1
        val validSettled = paymentMethod == PaymentMethod.CREDIT_CARD && creditSettled

        dao.upsertExpense(
            LocalExpenseEntity(
                id = expenseId,
                title = cleanTitle,
                amount = amount,
                category = cleanCategoryId,
                dateKey = dateKey,
                monthKey = dateKey.take(7),
                note = cleanNote,
                paymentMethod = paymentMethod.name,
                cardName = cleanCardName,
                installments = validInstallments,
                creditSettled = validSettled,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun setCreditSettled(expenseId: String, settled: Boolean): Result<Unit> = try {
        require(expenseId.isNotBlank()) { "El ID del gasto no puede estar vacío." }
        dao.updateCreditSettled(expenseId, settled, System.currentTimeMillis())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteExpense(expenseId: String): Result<Unit> = try {
        require(expenseId.isNotBlank()) { "El ID del gasto no puede estar vacío." }
        dao.deleteExpenseById(expenseId)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun saveUserSettings(
        dailyBudget: Double,
        currencySymbol: String,
    ): Result<Unit> = try {
        require(dailyBudget in 0.0..10_000_000.0) {
            "El presupuesto diario debe estar entre 0 y 10,000,000."
        }
        val cleanCurrency = currencySymbol.trim().ifBlank { "$" }.take(5)
        dao.upsertSettings(
            LocalSettingsEntity(
                id = 1,
                dailyBudget = dailyBudget,
                currencySymbol = cleanCurrency,
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val GUEST_USER_ID = "local_guest"
    }
}
